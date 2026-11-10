package app.olauncher.folders

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

/**
 * Modelo de carpetas sin dependencias de Android.
 *
 * - Cada carpeta tiene un id estable y vive una sola vez en [folders].
 * - Una carpeta puede contener aplicaciones, pinned-shortcuts y otras carpetas como ids.
 * - Jerarquía de un solo padre: el padre se deriva de las listas de hijos y nunca se almacena
 *   duplicado. Una carpeta está en la raíz si tiene una referencia externa [LOCATION_DRAWER].
 * - Las referencias externas (escritorio y deslizamiento) no alteran la jerarquía.
 * - El contador de referencias se deriva de las referencias reales, por lo que no puede
 *   desincronizarse: padre existente (1) + referencias externas.
 * - Cuando una carpeta se queda sin referencias se elimina y libera a sus carpetas hijas en
 *   cascada. Las aplicaciones y los pinned-shortcuts no se eliminan nunca desde aquí.
 * - Se impiden las referencias circulares.
 */
class FolderStore(snapshot: Snapshot = Snapshot()) {

    class Folder internal constructor(val id: String, name: String) {
        var name: String = name
            internal set

        var parentId: String? = null
            internal set

        internal val children = LinkedHashSet<String>()

        /** Ids de los elementos contenidos, en orden de inserción. */
        val childIds: List<String>
            get() = children.toList()
    }

    data class FolderData(val id: String, val name: String, val children: List<String> = emptyList())

    data class ExternalRef(val location: String, val folderId: String)

    data class Snapshot(
        val folders: List<FolderData> = emptyList(),
        val externalRefs: List<ExternalRef> = emptyList(),
    )

    enum class AddResult { ADDED, ALREADY_PRESENT, FOLDER_NOT_FOUND, WOULD_CREATE_CYCLE }

    enum class MoveResult {
        MOVED, ALREADY_IN_PLACE, FOLDER_NOT_FOUND, PARENT_NOT_FOUND, WOULD_CREATE_CYCLE
    }

    private val folders = LinkedHashMap<String, Folder>()
    private val externalRefs = LinkedHashSet<ExternalRef>()

    init {
        load(snapshot)
    }

    // ---------------------------------------------------------------- consultas

    fun folder(id: String): Folder? = folders[id]

    fun isFolderId(itemId: String): Boolean = itemId in folders

    fun folderIds(): List<String> = folders.keys.toList()

    fun folderName(id: String): String? = folders[id]?.name

    fun parentOf(id: String): String? = folders[id]?.parentId

    fun childrenOf(folderId: String): List<String> = folders[folderId]?.childIds.orEmpty()

    fun childFolderIds(folderId: String): List<String> =
        folders[folderId]?.children?.filter { it in folders }.orEmpty()

    /** Carpetas que están en la raíz del App Drawer, en orden de inserción. */
    fun rootFolderIds(): List<String> = folderIdsAt(LOCATION_DRAWER)

    /** Carpetas referenciadas desde [location] (p. ej. [LOCATION_DRAWER]), en orden. */
    fun folderIdsAt(location: String): List<String> =
        externalRefs.filter { it.location == location && it.folderId in folders }.map { it.folderId }

    /** Carpetas que contienen directamente [itemId]. */
    fun containingFolders(itemId: String): List<String> =
        folders.values.filter { itemId in it.children }.map { it.id }

    /** `true` si [itemId] está dentro de alguna carpeta. */
    fun containsItem(itemId: String): Boolean =
        folders.values.any { itemId in it.children }

    /**
     * Número real de ubicaciones que apuntan a la carpeta: padre existente (1) más referencias
     * externas (raíz del App Drawer, escritorio, deslizamiento). No se almacena.
     */
    fun referenceCount(folderId: String): Int {
        val folder = folders[folderId] ?: return 0
        val fromParent = if (folder.parentId != null && folder.parentId in folders) 1 else 0
        val fromLocations = externalRefs.count { it.folderId == folderId }
        return fromParent + fromLocations
    }

    /** Ubicaciones que referencian la carpeta: `parent` o las externas. */
    fun referenceLocations(folderId: String): List<String> {
        val folder = folders[folderId] ?: return emptyList()
        val locations = mutableListOf<String>()
        if (folder.parentId != null && folder.parentId in folders) locations.add(LOCATION_PARENT)
        externalRefs.filter { it.folderId == folderId }.forEach { locations.add(it.location) }
        return locations
    }

    /** Ruta completa, p. ej. `Trabajo/Proyectos/Documentos`. */
    fun path(folderId: String, separator: String = "/"): String? {
        if (folderId !in folders) return null
        val names = ArrayDeque<String>()
        val seen = HashSet<String>()
        var current: String? = folderId
        while (current != null && seen.add(current)) {
            val folder = folders[current] ?: break
            names.addFirst(folder.name)
            current = folder.parentId
        }
        return names.joinToString(separator)
    }

    /** Cadena de ids desde la carpeta raíz hasta [folderId], incluida. */
    fun ancestorIds(folderId: String): List<String> {
        if (folderId !in folders) return emptyList()
        val chain = ArrayDeque<String>()
        val seen = HashSet<String>()
        var current: String? = folderId
        while (current != null && seen.add(current)) {
            val folder = folders[current] ?: break
            chain.addFirst(current)
            current = folder.parentId
        }
        return chain.toList()
    }

    /**
     * Primera carpeta con ese nombre en el nivel indicado. [parentId] `null` significa raíz del
     * App Drawer. La comparación ignora mayúsculas/minúsculas y acentos.
     */
    fun findFolderByName(parentId: String?, name: String): String? {
        val candidates = if (parentId == null) rootFolderIds() else childFolderIds(parentId)
        val normalized = FolderText.normalize(name)
        return candidates.firstOrNull { candidate ->
            folders[candidate]?.let { FolderText.normalize(it.name) == normalized } == true
        }
    }

    /** `true` si [candidateId] está dentro del subárbol de [ancestorId]. */
    fun isDescendant(candidateId: String, ancestorId: String): Boolean {
        val seen = HashSet<String>()
        var current = folders[candidateId]?.parentId
        while (current != null && seen.add(current)) {
            if (current == ancestorId) return true
            current = folders[current]?.parentId
        }
        return false
    }

    /** `true` si [folderId] puede moverse a [newParentId] sin crear un ciclo. */
    fun canMove(folderId: String, newParentId: String): Boolean {
        if (folderId !in folders || newParentId !in folders) return false
        if (folderId == newParentId) return false
        return !isDescendant(newParentId, folderId)
    }

    // ------------------------------------------------------------------ altas

    /** Crea una carpeta en la raíz del App Drawer. */
    fun createFolder(name: String): String {
        val id = FOLDER_ID_PREFIX + UUID.randomUUID()
        folders[id] = Folder(id, name.trim())
        externalRefs.add(ExternalRef(LOCATION_DRAWER, id))
        return id
    }

    /**
     * Añade [itemId] a [folderId]. Las carpetas se mueven (nunca se duplican); las aplicaciones y
     * los pinned-shortcuts solo se referencian. La política de duplicados vive en el repositorio.
     */
    fun addToFolder(folderId: String, itemId: String): AddResult {
        val folder = folders[folderId] ?: return AddResult.FOLDER_NOT_FOUND
        if (itemId == folderId) return AddResult.WOULD_CREATE_CYCLE
        if (itemId in folder.children) return AddResult.ALREADY_PRESENT

        if (itemId in folders) {
            if (!canMove(itemId, folderId)) return AddResult.WOULD_CREATE_CYCLE
            detachFromCurrentLocation(folders[itemId]!!)
            folders[itemId]!!.parentId = folderId
            folder.children.add(itemId)
            return AddResult.ADDED
        }

        folder.children.add(itemId)
        return AddResult.ADDED
    }

    /**
     * Añade un elemento aplicando la política de duplicados. Las carpetas siempre se mueven; las
     * aplicaciones y los pinned-shortcuts se retiran de sus otras carpetas cuando
     * [allowDuplicateItems] es `false`.
     */
    fun addItem(folderId: String, itemId: String, allowDuplicateItems: Boolean): AddResult {
        if (folderId !in folders) return AddResult.FOLDER_NOT_FOUND
        if (!allowDuplicateItems && itemId !in folders) {
            removeItemFromAllFolders(itemId, exceptFolderId = folderId)
        }
        return addToFolder(folderId, itemId)
    }

    /** Referencia externa: raíz del App Drawer, escritorio o deslizamiento. */
    fun addExternalRef(location: String, folderId: String): Boolean {
        val folder = folders[folderId] ?: return false
        if (location == LOCATION_DRAWER && folder.parentId != null) return false
        return externalRefs.add(ExternalRef(location, folderId))
    }

    fun renameFolder(folderId: String, newName: String): Boolean {
        val folder = folders[folderId] ?: return false
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        folder.name = trimmed
        return true
    }

    // ------------------------------------------------------------ movimientos

    /**
     * Mueve una carpeta a otra o a la raíz (`newParentId == null`). La pertenencia jerárquica se
     * transfiere en una sola operación, sin pasar por cero referencias.
     */
    fun moveFolder(folderId: String, newParentId: String?): MoveResult {
        val folder = folders[folderId] ?: return MoveResult.FOLDER_NOT_FOUND
        if (newParentId == null) {
            val alreadyRoot = folder.parentId == null &&
                    externalRefs.contains(ExternalRef(LOCATION_DRAWER, folderId))
            if (alreadyRoot) return MoveResult.ALREADY_IN_PLACE
            detachFromCurrentLocation(folder)
            externalRefs.add(ExternalRef(LOCATION_DRAWER, folderId))
            return MoveResult.MOVED
        }

        val parent = folders[newParentId] ?: return MoveResult.PARENT_NOT_FOUND
        if (folder.parentId == newParentId) return MoveResult.ALREADY_IN_PLACE
        if (!canMove(folderId, newParentId)) return MoveResult.WOULD_CREATE_CYCLE

        detachFromCurrentLocation(folder)
        folder.parentId = newParentId
        parent.children.add(folderId)
        return MoveResult.MOVED
    }

    // ------------------------------------------------------------ eliminación

    /**
     * Elimina la referencia de [itemId] dentro de [folderId]. Si es una carpeta y era su última
     * referencia, se elimina definitivamente en cascada. Devuelve `true` si había referencia.
     */
    fun removeFromFolder(folderId: String, itemId: String): Boolean {
        val folder = folders[folderId] ?: return false
        if (!folder.children.remove(itemId)) return false
        if (itemId in folders) folders[itemId]!!.parentId = null
        releaseIfUnreferenced(itemId)
        return true
    }

    /**
     * Elimina la referencia externa de [folderId] en [location]. Si era la última, la carpeta se
     * elimina definitivamente.
     */
    fun removeExternalRef(location: String, folderId: String): Boolean {
        val removed = externalRefs.removeAll { it.location == location && it.folderId == folderId }
        if (!removed) return false
        releaseIfUnreferenced(folderId)
        return true
    }

    /** Quita todas las referencias externas que cumplan [predicate]; el llamador recopila basura. */
    fun removeExternalRefsWhere(predicate: (ExternalRef) -> Boolean) {
        externalRefs.removeAll { predicate(it) }
    }

    /**
     * Quita [itemId] de todas las carpetas (salvo [exceptFolderId]). Se usa para mover aplicaciones
     * y pinned-shortcuts cuando `allowDuplicateItems` vale `false`.
     */
    fun removeItemFromAllFolders(itemId: String, exceptFolderId: String? = null): Boolean {
        var removed = false
        folders.values.toList().forEach { folder ->
            if (folder.id == exceptFolderId) return@forEach
            if (folder.children.remove(itemId)) {
                removed = true
                if (itemId in folders) folders[itemId]!!.parentId = null
            }
        }
        if (removed) releaseIfUnreferenced(itemId)
        return removed
    }

    /** Elimina todas las carpetas sin referencias, en cascada. */
    fun collectGarbage() {
        folders.keys.toList().forEach { releaseIfUnreferenced(it) }
    }

    private fun detachFromCurrentLocation(folder: Folder) {
        folder.parentId?.let { parentId -> folders[parentId]?.children?.remove(folder.id) }
        folder.parentId = null
        externalRefs.removeAll { it.location == LOCATION_DRAWER && it.folderId == folder.id }
    }

    /**
     * Si [start] ya no tiene referencias se elimina; al desaparecer libera sus hijos carpetas, que
     * se eliminan solo si se quedan sin ninguna otra referencia. Iterativo para no desbordar la pila.
     */
    private fun releaseIfUnreferenced(start: String) {
        val pending = ArrayDeque<String>()
        pending.add(start)
        while (pending.isNotEmpty()) {
            val id = pending.removeLast()
            val folder = folders[id] ?: continue
            if (referenceCount(id) > 0) continue

            folder.parentId?.let { parentId -> folders[parentId]?.children?.remove(id) }
            folders.remove(id)
            externalRefs.removeAll { it.folderId == id }

            folder.children.filter { it in folders }.forEach { childId ->
                folders[childId]?.parentId = null
                pending.add(childId)
            }
        }
    }

    // ------------------------------------------------------------ persistencia

    fun snapshot(): Snapshot = Snapshot(
        folders = folders.values.map { FolderData(it.id, it.name, it.children.toList()) },
        externalRefs = externalRefs.filter { it.folderId in folders }.toList(),
    )

    private fun load(snapshot: Snapshot) {
        snapshot.folders.forEach { data ->
            if (data.id.isNotBlank() && data.id !in folders) {
                folders[data.id] = Folder(data.id, data.name)
            }
        }

        snapshot.folders.forEach { data ->
            val parent = folders[data.id] ?: return@forEach
            data.children.forEach { raw ->
                val childId = raw.trim()
                if (childId.isEmpty() || childId == data.id) return@forEach
                if (childId in folders) {
                    val child = folders[childId] ?: return@forEach
                    // Un solo padre: la primera referencia encontrada gana.
                    if (child.parentId != null) return@forEach
                    child.parentId = data.id
                    parent.children.add(childId)
                } else {
                    // Referencias a carpetas inexistentes: se descartan. Las apps no se validan
                    // aquí (pueden estar desinstaladas temporalmente).
                    if (childId.startsWith(FOLDER_ID_PREFIX)) return@forEach
                    parent.children.add(childId)
                }
            }
        }

        // Se añaden antes de romper ciclos: una carpeta puede estar corruptamente anidada y a la
        // vez tener referencia de raíz; tras sanear la jerarquía se decide cuál es válida.
        snapshot.externalRefs.forEach { ref ->
            if (ref.folderId in folders) externalRefs.add(ref)
        }

        breakCycles()
        dropDrawerRefsForNestedFolders()
        collectGarbage()
    }

    private fun dropDrawerRefsForNestedFolders() {
        externalRefs.removeAll { ref ->
            ref.location == LOCATION_DRAWER && folders[ref.folderId]?.parentId != null
        }
    }

    private fun breakCycles() {
        repeat(folders.size + 1) {
            if (!breakOneCycle()) return
        }
    }

    private fun breakOneCycle(): Boolean {
        folders.values.forEach { start ->
            val seen = HashSet<String>()
            var current: String? = start.id
            while (current != null) {
                if (!seen.add(current)) {
                    val node = folders[current] ?: return@forEach
                    node.parentId?.let { parentId -> folders[parentId]?.children?.remove(node.id) }
                    node.parentId = null
                    return true
                }
                current = folders[current]?.parentId
            }
        }
        return false
    }

    companion object {
        const val LOCATION_DRAWER = "drawer"
        const val LOCATION_HOME_PREFIX = "home:"
        const val LOCATION_SWIPE_LEFT = "swipe:left"
        const val LOCATION_SWIPE_RIGHT = "swipe:right"
        const val LOCATION_PARENT = "parent"

        private const val HEADER = "folders-v1"

        fun serialize(snapshot: Snapshot): String {
            if (snapshot.folders.isEmpty()) return ""
            val builder = StringBuilder(HEADER).append('\n')
            snapshot.folders.forEach { folder ->
                builder.append("F\t")
                    .append(encode(folder.id)).append('\t')
                    .append(encode(folder.name)).append('\t')
                    .append(folder.children.joinToString(",") { encode(it) })
                    .append('\n')
            }
            snapshot.externalRefs.forEach { ref ->
                builder.append("R\t")
                    .append(encode(ref.location)).append('\t')
                    .append(encode(ref.folderId)).append('\n')
            }
            return builder.toString()
        }

        /** Tolerante: las líneas mal formadas se ignoran y un texto desconocido da estado vacío. */
        fun deserialize(text: String?): Snapshot {
            if (text.isNullOrBlank()) return Snapshot()
            val lines = text.lines()
            if (lines.firstOrNull() != HEADER) return Snapshot()

            val folders = mutableListOf<FolderData>()
            val refs = mutableListOf<ExternalRef>()
            for (line in lines.drop(1)) {
                val parts = line.split('\t')
                try {
                    when {
                        parts[0] == "F" && parts.size == 4 -> folders.add(
                            FolderData(
                                id = decode(parts[1]),
                                name = decode(parts[2]),
                                children = if (parts[3].isEmpty()) emptyList()
                                else parts[3].split(',').map { decode(it) },
                            )
                        )

                        parts[0] == "R" && parts.size == 3 ->
                            refs.add(ExternalRef(decode(parts[1]), decode(parts[2])))
                    }
                } catch (_: Exception) {
                    // Línea corrupta: se ignora.
                }
            }
            return Snapshot(folders, refs)
        }

        private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

        private fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")
    }
}
