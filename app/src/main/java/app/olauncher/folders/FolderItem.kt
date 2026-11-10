package app.olauncher.folders

import app.olauncher.data.AppModel

/**
 * Convenciones de identidad del sistema de carpetas.
 *
 * Las carpetas se identifican con [FOLDER_ID_PREFIX] y ese mismo id se usa como marcador en los
 * campos `appPackage` de los huecos del escritorio y de las acciones de deslizamiento.
 */
const val FOLDER_ID_PREFIX = "folder:"

private const val APP_ITEM_PREFIX = "app:"

/** `true` si [value] es un marcador de carpeta (`folder:<id>`). */
fun isFolderMarker(value: String?): Boolean = value != null && value.startsWith(FOLDER_ID_PREFIX)

/** Identidad estable de una aplicación dentro de una carpeta. */
fun appItemId(packageName: String, user: String): String = "$APP_ITEM_PREFIX$packageName|$user"

/**
 * Identidad usada por [FolderStore] para [AppModel]. Devuelve `null` para elementos que no pueden
 * pertenecer a una carpeta (cabecera de espacio privado o relleno inferior de la lista).
 */
fun itemIdOf(appModel: AppModel): String? = when (appModel) {
    is AppModel.App ->
        if (isFolderMarker(appModel.appPackage)) appModel.appPackage
        else appItemId(appModel.appPackage, appModel.user.toString())

    is AppModel.PinnedShortcut -> appModel.identity
    else -> null
}

/** El marcador de una carpeta coincide con su propio id, que ya lleva el prefijo. */
fun folderMarker(folderId: String): String = folderId
