package app.olauncher.folders

import android.content.Context
import app.olauncher.data.Prefs

/**
 * Persistencia de [FolderStore] y política de carpetas.
 *
 * Usa el mismo archivo de Settings que Olauncher (`app.olauncher`) con claves propias, por lo que
 * no necesita modificar [Prefs] ni migrar datos: si las claves no existen, no hay carpetas.
 *
 * Las referencias de escritorio y deslizamiento no se guardan duplicadas: se reconstruyen a partir
 * de los huecos reales de [Prefs] en cada carga. Así, asignar, sustituir, desactivar o desinstalar
 * un elemento en esos huecos actualiza el contador de referencias sin tocar el código original.
 */
class FolderRepository(context: Context) {

    private val appContext = context.applicationContext
    private val settings = appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    private val prefs = Prefs(appContext)

    val allowDuplicateItems: Boolean
        get() = settings.getBoolean(KEY_ALLOW_DUPLICATE_ITEMS, false)

    val showFoldersFirst: Boolean
        get() = settings.getBoolean(KEY_SHOW_FOLDERS_FIRST, true)

    fun load(): FolderStore {
        val store = FolderStore(FolderStore.deserialize(settings.getString(KEY_FOLDERS_DATA, "")))
        reconcileExternalRefs(store)
        return store
    }

    /** Ejecuta una mutación atómica: carga, modifica, elimina huérfanas, sincroniza y guarda. */
    private fun <T> edit(block: (FolderStore) -> T): T {
        val store = load()
        val result = block(store)
        store.collectGarbage()
        syncSlotNames(store)
        save(store)
        return result
    }

    fun snapshot(): FolderStore = load()

    fun renameFolder(folderId: String, name: String): Boolean = edit { store ->
        val parentId = store.parentOf(folderId)
        val duplicate = store.findFolderByName(parentId, name)
        if (duplicate != null && duplicate != folderId) return@edit false
        store.renameFolder(folderId, name)
    }

    fun createFolder(name: String): String = edit { it.createFolder(name) }

    /** Devuelve la carpeta de la raíz con ese nombre o la crea si no existe. */
    fun findOrCreateRootFolder(name: String): String = edit { store ->
        store.findFolderByName(parentId = null, name = name) ?: store.createFolder(name)
    }

    /**
     * Crea una carpeta en la raíz (o reutiliza la existente con ese nombre) y añade el elemento en
     * una sola operación. Si la creación fuese nueva y el alta fallara, la carpeta se revierte.
     */
    fun createRootFolderAndAddItem(name: String, itemId: String): FolderStore.AddResult = edit { store ->
        val existing = store.findFolderByName(parentId = null, name = name)
        val folderId = existing ?: store.createFolder(name)
        val result = store.addItem(folderId, itemId, allowDuplicateItems)
        if (existing == null && result != FolderStore.AddResult.ADDED) {
            store.removeExternalRef(FolderStore.LOCATION_DRAWER, folderId)
        }
        result
    }

    fun folderExists(folderId: String): Boolean = load().isFolderId(folderId)

    fun folderName(folderId: String): String? = load().folderName(folderId)

    fun folderPath(folderId: String): String? = load().path(folderId)

    fun rootFolderIds(): List<String> = load().rootFolderIds()

    /**
     * Destinos válidos para el selector: todas las carpetas con su ruta completa, excluyendo la
     * propia carpeta y sus descendientes cuando el elemento que se mueve es una carpeta.
     */
    fun destinationsFor(itemId: String): List<FolderDestination> {
        val store = load()
        val movingFolderId = itemId.takeIf { store.isFolderId(it) }
        return store.folderIds()
            .asSequence()
            .filter { id ->
                when {
                    movingFolderId == null -> true
                    id == movingFolderId -> false
                    else -> !store.isDescendant(id, movingFolderId)
                }
            }
            .mapNotNull { id -> store.path(id)?.let { FolderDestination(id, it) } }
            .sortedBy { FolderText.normalize(it.path) }
            .toList()
    }

    fun childrenOf(folderId: String): List<String> = load().childrenOf(folderId)

    fun containingFolders(itemId: String): List<String> = load().containingFolders(itemId)

    fun containsItem(itemId: String): Boolean = load().containsItem(itemId)

    /**
     * Añade [itemId] a [folderId]. Con `allowDuplicateItems = false`, las aplicaciones y los
     * pinned-shortcuts se retiran de cualquier otra carpeta antes de añadirse. Las carpetas siempre
     * se mueven, nunca se duplican.
     */
    fun addItemToFolder(folderId: String, itemId: String): FolderStore.AddResult = edit { store ->
        store.addItem(folderId, itemId, allowDuplicateItems)
    }

    fun removeItemFromFolder(folderId: String, itemId: String): Boolean =
        edit { it.removeFromFolder(folderId, itemId) }

    fun removeFolderFromDrawer(folderId: String): Boolean =
        edit { it.removeExternalRef(FolderStore.LOCATION_DRAWER, folderId) }

    fun removeFolderFromParent(parentId: String, folderId: String): Boolean =
        edit { it.removeFromFolder(parentId, folderId) }

    fun moveFolder(folderId: String, newParentId: String?): FolderStore.MoveResult =
        edit { it.moveFolder(folderId, newParentId) }

    // ------------------------------------------------------- referencias externas

    private fun reconcileExternalRefs(store: FolderStore) {
        store.removeExternalRefsWhere { it.location != FolderStore.LOCATION_DRAWER }

        for (index in 1..MAX_HOME_SLOTS) {
            if (index > prefs.homeAppsNum) break
            val marker = prefs.getAppPackage(index)
            if (!isFolderMarker(marker)) continue
            val name = store.folderName(marker)
            if (name == null) {
                clearHomeSlot(index)
            } else {
                store.addExternalRef(FolderStore.LOCATION_HOME_PREFIX + index, marker)
                setHomeSlotName(index, name)
            }
        }

        if (prefs.swipeLeftEnabled) {
            reconcileSwipe(
                store = store,
                marker = prefs.appPackageSwipeLeft,
                currentName = prefs.appNameSwipeLeft,
                setter = { name -> prefs.appNameSwipeLeft = name },
                clear = { prefs.appPackageSwipeLeft = ""; prefs.appNameSwipeLeft = "" },
            )?.let { store.addExternalRef(FolderStore.LOCATION_SWIPE_LEFT, it) }
        }

        if (prefs.swipeRightEnabled) {
            reconcileSwipe(
                store = store,
                marker = prefs.appPackageSwipeRight,
                currentName = prefs.appNameSwipeRight,
                setter = { name -> prefs.appNameSwipeRight = name },
                clear = { prefs.appPackageSwipeRight = ""; prefs.appNameSwipeRight = "" },
            )?.let { store.addExternalRef(FolderStore.LOCATION_SWIPE_RIGHT, it) }
        }

        store.collectGarbage()
    }

    private fun reconcileSwipe(
        store: FolderStore,
        marker: String,
        currentName: String,
        setter: (String) -> Unit,
        clear: () -> Unit,
    ): String? {
        if (!isFolderMarker(marker)) return null
        val folderName = store.folderName(marker)
        if (folderName == null) {
            clear()
            return null
        }
        if (folderName != currentName) setter(folderName)
        return marker
    }

    private fun syncSlotNames(store: FolderStore) {
        for (index in 1..MAX_HOME_SLOTS) {
            if (index > prefs.homeAppsNum) break
            val marker = prefs.getAppPackage(index)
            if (!isFolderMarker(marker)) continue
            store.folderName(marker)?.let { setHomeSlotName(index, it) }
        }
        if (prefs.swipeLeftEnabled) {
            val marker = prefs.appPackageSwipeLeft
            if (isFolderMarker(marker)) store.folderName(marker)?.let { prefs.appNameSwipeLeft = it }
        }
        if (prefs.swipeRightEnabled) {
            val marker = prefs.appPackageSwipeRight
            if (isFolderMarker(marker)) store.folderName(marker)?.let { prefs.appNameSwipeRight = it }
        }
    }

    private fun save(store: FolderStore) {
        settings.edit()
            .putString(KEY_FOLDERS_DATA, FolderStore.serialize(store.snapshot()))
            .apply()
    }

    private fun setHomeSlotName(index: Int, name: String) {
        when (index) {
            1 -> if (prefs.appName1 != name) prefs.appName1 = name
            2 -> if (prefs.appName2 != name) prefs.appName2 = name
            3 -> if (prefs.appName3 != name) prefs.appName3 = name
            4 -> if (prefs.appName4 != name) prefs.appName4 = name
            5 -> if (prefs.appName5 != name) prefs.appName5 = name
            6 -> if (prefs.appName6 != name) prefs.appName6 = name
            7 -> if (prefs.appName7 != name) prefs.appName7 = name
            8 -> if (prefs.appName8 != name) prefs.appName8 = name
        }
    }

    private fun clearHomeSlot(index: Int) {
        when (index) {
            1 -> { prefs.appName1 = ""; prefs.appPackage1 = "" }
            2 -> { prefs.appName2 = ""; prefs.appPackage2 = "" }
            3 -> { prefs.appName3 = ""; prefs.appPackage3 = "" }
            4 -> { prefs.appName4 = ""; prefs.appPackage4 = "" }
            5 -> { prefs.appName5 = ""; prefs.appPackage5 = "" }
            6 -> { prefs.appName6 = ""; prefs.appPackage6 = "" }
            7 -> { prefs.appName7 = ""; prefs.appPackage7 = "" }
            8 -> { prefs.appName8 = ""; prefs.appPackage8 = "" }
        }
    }

    companion object {
        private const val PREFS_FILENAME = "app.olauncher"
        private const val KEY_FOLDERS_DATA = "FOLDERS_DATA"
        private const val KEY_ALLOW_DUPLICATE_ITEMS = "FOLDERS_ALLOW_DUPLICATE_ITEMS"
        private const val KEY_SHOW_FOLDERS_FIRST = "FOLDERS_SHOW_FOLDERS_FIRST"
        private const val MAX_HOME_SLOTS = 8
    }
}
