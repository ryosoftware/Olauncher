package app.olauncher.folders

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Normalización de nombres: ignora espacios exteriores, mayúsculas/minúsculas y acentos. */
object FolderText {

    private val combiningMarks = Regex("\\p{M}+")

    fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)

    fun matches(a: String, b: String): Boolean = normalize(a) == normalize(b)
}

/**
 * Ordenación de elementos de una lista (raíz del App Drawer o contenido de una carpeta).
 * La comparación alfabética usa fuerza primaria del [Collator], por lo que ignora mayúsculas,
 * minúsculas y acentos. Si [foldersFirst] es `true`, las carpetas van antes.
 */
object FolderSort {

    fun <T> sorted(
        items: List<T>,
        labelOf: (T) -> String,
        isFolder: (T) -> Boolean,
        foldersFirst: Boolean,
    ): List<T> {
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        val comparator = Comparator<T> { a, b ->
            if (foldersFirst) {
                val aIsFolder = isFolder(a)
                val bIsFolder = isFolder(b)
                if (aIsFolder != bIsFolder) return@Comparator if (aIsFolder) -1 else 1
            }
            collator.compare(labelOf(a), labelOf(b))
        }
        return items.sortedWith(comparator)
    }
}
