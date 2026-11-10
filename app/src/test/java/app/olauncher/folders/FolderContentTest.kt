package app.olauncher.folders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderContentTest {

    private data class Entry(val label: String, val folder: Boolean)

    @Test
    fun `normalizar ignora espacios, mayusculas y acentos`() {
        assertEquals("trabajo", FolderText.normalize("  Trabájo  "))
        assertEquals("documentos", FolderText.normalize("DOCUMENTOS"))
        assertTrue(FolderText.matches("Proyectos", "proyéctos"))
        assertFalse(FolderText.matches("Trabajo", "Personal"))
    }

    @Test
    fun `con carpetas primero se ordenan antes que apps y shortcuts`() {
        val items = listOf(
            Entry("Banana", folder = false),
            Entry("Trabajo", folder = true),
            Entry("Árbol", folder = false),
            Entry("personal", folder = true),
        )

        val sorted = FolderSort.sorted(items, { it.label }, { it.folder }, foldersFirst = true)

        assertEquals(listOf("personal", "Trabajo", "Árbol", "Banana"), sorted.map { it.label })
    }

    @Test
    fun `sin carpetas primero todo se ordena conjuntamente`() {
        val items = listOf(
            Entry("Banana", folder = false),
            Entry("Trabajo", folder = true),
            Entry("Árbol", folder = false),
            Entry("personal", folder = true),
        )

        val sorted = FolderSort.sorted(items, { it.label }, { it.folder }, foldersFirst = false)

        assertEquals(listOf("Árbol", "Banana", "personal", "Trabajo"), sorted.map { it.label })
    }

    @Test
    fun `la ordenacion ignora mayusculas y acentos`() {
        val items = listOf(
            Entry("banana", folder = false),
            Entry("Árbol", folder = false),
            Entry("árbol", folder = false),
            Entry("Casa", folder = false),
        )

        val sorted = FolderSort.sorted(items, { it.label }, { it.folder }, foldersFirst = true)

        assertEquals(listOf("Árbol", "árbol", "banana", "Casa"), sorted.map { it.label })
    }
}
