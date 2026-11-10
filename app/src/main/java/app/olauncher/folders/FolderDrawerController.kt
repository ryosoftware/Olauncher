package app.olauncher.folders

import android.content.Context
import android.os.Process
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import java.text.Collator

/**
 * Estado de navegación y construcción de listas del App Drawer.
 *
 * Mantiene una pila de carpetas abiertas. La lista visible se construye a partir de las
 * aplicaciones que ya tiene cargadas el App Drawer, insertando las filas de carpeta y aplicando la
 * ordenación y el filtrado de pertenencia sin modificar los modelos originales.
 */
class FolderDrawerController(context: Context) {

    private val repository = FolderRepository(context)
    private var store = repository.load()
    private val stack = ArrayDeque<String>()
    private var openedExternally = false

    val isInsideFolder: Boolean
        get() = stack.isNotEmpty()

    val currentFolderId: String?
        get() = stack.lastOrNull()

    fun canHandleBack(): Boolean = stack.isNotEmpty()

    fun reload() {
        store = repository.load()
        while (stack.isNotEmpty() && store.folderName(stack.last()) == null) stack.removeLast()
        if (stack.isEmpty()) openedExternally = false
    }

    /** Abre [folderId]. [external] indica escritorio o deslizamiento. */
    fun open(folderId: String, external: Boolean = false): Boolean {
        if (!store.isFolderId(folderId)) return false
        if (stack.isEmpty()) openedExternally = external
        stack.addLast(folderId)
        return true
    }

    /** Abre [appModel] si es una fila de carpeta. */
    fun openFolderRow(appModel: AppModel, external: Boolean = false): Boolean =
        if (isFolderRow(appModel)) open(appModel.appPackage, external) else false

    fun isFolderRow(appModel: AppModel): Boolean =
        isFolderMarker(appModel.appPackage) && store.isFolderId(appModel.appPackage)

    /**
     * Sube un nivel. Devuelve `false` cuando se ha llegado al contexto inicial abierto desde fuera
     * y el llamador debe cerrar el App Drawer.
     */
    fun goUp(): Boolean {
        if (stack.isEmpty()) return false
        if (openedExternally && stack.size == 1) return false
        stack.removeLast()
        if (stack.isEmpty()) openedExternally = false
        return true
    }

    fun reset() {
        stack.clear()
        openedExternally = false
    }

    /** Reabre la ruta completa hasta [folderId], p. ej. al volver del selector de carpetas. */
    fun openPathTo(folderId: String) {
        reload()
        val chain = store.ancestorIds(folderId)
        stack.clear()
        openedExternally = false
        chain.forEach { stack.addLast(it) }
    }

    /** Nombre de la carpeta abierta, o `null` en la raíz del App Drawer. */
    fun currentFolderName(): String? = currentFolderId?.let { store.folderName(it) }

    fun addItemToFolder(folderId: String, itemId: String): FolderStore.AddResult {
        val result = repository.addItemToFolder(folderId, itemId)
        reload()
        return result
    }

    fun createRootFolderAndAdd(name: String, itemId: String): FolderStore.AddResult {
        val result = repository.createRootFolderAndAddItem(name, itemId)
        reload()
        return result
    }

    fun renameFolder(folderId: String, name: String): Boolean {
        val result = repository.renameFolder(folderId, name)
        reload()
        return result
    }

    fun removeFromCurrentFolder(appModel: AppModel): Boolean {
        val folderId = currentFolderId ?: return false
        val itemId = itemIdOf(appModel) ?: return false
        val result = repository.removeItemFromFolder(folderId, itemId)
        reload()
        return result
    }

    fun removeFolderFromDrawer(appModel: AppModel): Boolean {
        val folderId = appModel.appPackage
        if (!isFolderMarker(folderId)) return false
        val result = repository.removeFolderFromDrawer(folderId)
        reload()
        return result
    }

    /**
     * Lista visible: raíz decorada con carpetas o contenido de la carpeta actual.
     * [extraApps] aporta elementos adicionales (espacio privado) para resolver contenido.
     */
    fun visibleItems(
        apps: List<AppModel>,
        extraApps: List<AppModel>,
        flag: Int,
    ): MutableList<AppModel> {
        val folderId = currentFolderId ?: return rootItems(apps, flag)
        return folderItems(folderId, apps, extraApps)
    }

    private fun rootItems(apps: List<AppModel>, flag: Int): MutableList<AppModel> {
        if (flag == Constants.FLAG_HIDDEN_APPS) return apps.toMutableList()

        val visible = apps.toMutableList()
        // Con allowDuplicateItems=false, la raíz oculta lo que ya vive en alguna carpeta.
        if (flag == Constants.FLAG_LAUNCH_APP && !repository.allowDuplicateItems) {
            visible.removeAll { item -> itemIdOf(item)?.let(store::containsItem) == true }
        }

        val collator = Collator.getInstance()
        val folderRows = store.rootFolderIds().mapNotNull { folderRow(it, collator) }
        if (folderRows.isEmpty()) return visible

        visible.addAll(folderRows)
        return FolderSort.sorted(
            items = visible,
            labelOf = { it.appLabel },
            isFolder = { isFolderMarker(it.appPackage) },
            foldersFirst = repository.showFoldersFirst,
        ).toMutableList()
    }

    private fun folderItems(
        folderId: String,
        apps: List<AppModel>,
        extraApps: List<AppModel>,
    ): MutableList<AppModel> {
        val byId = HashMap<String, AppModel>()
        (apps + extraApps).forEach { item -> itemIdOf(item)?.let { byId[it] = item } }

        val collator = Collator.getInstance()
        val items = store.childrenOf(folderId).mapNotNull { itemId ->
            if (store.isFolderId(itemId)) folderRow(itemId, collator) else byId[itemId]
        }
        return FolderSort.sorted(
            items = items,
            labelOf = { it.appLabel },
            isFolder = { isFolderMarker(it.appPackage) },
            foldersFirst = repository.showFoldersFirst,
        ).toMutableList()
    }

    private fun folderRow(folderId: String, collator: Collator): AppModel.App? {
        val name = store.folderName(folderId) ?: return null
        return AppModel.App(
            appLabel = name,
            key = collator.getCollationKey(name),
            appPackage = folderId,
            activityClassName = null,
            isNew = false,
            user = Process.myUserHandle(),
        )
    }
}
