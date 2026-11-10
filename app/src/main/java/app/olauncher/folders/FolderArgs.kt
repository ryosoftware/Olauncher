package app.olauncher.folders

/** Carpeta que debe abrirse al entrar en el App Drawer. */
const val ARG_FOLDER_ID = "folderId"

/** `true` si el App Drawer se abrió desde el escritorio o una acción de deslizamiento. */
const val ARG_FOLDER_EXTERNAL = "folderExternal"

/** Elemento (app, pinned-shortcut o carpeta) que se va a añadir a una carpeta. */
const val ARG_PICKER_ITEM_ID = "folderPickerItemId"

/** Carpeta que estaba abierta en el App Drawer al lanzar el selector. */
const val ARG_PICKER_CURRENT_FOLDER = "folderPickerCurrentFolder"

/** Clave de resultado para restaurar la carpeta abierta al volver del selector. */
const val FOLDER_PICKER_RESULT = "folderPickerResult"
