package app.olauncher.folders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderStoreTest {

    private val drawer = FolderStore.LOCATION_DRAWER
    private val appA = appItemId("com.a", "UserHandle{0}")
    private val appB = appItemId("com.b", "UserHandle{0}")
    private val shortcut = "shortcut:3:a.b5:x,y|z4:u{0}"

    // ------------------------------------------------------------------ raíz

    @Test
    fun `crear carpeta la deja en la raiz con una referencia`() {
        val store = FolderStore()
        val id = store.createFolder("Trabajo")

        assertTrue(store.isFolderId(id))
        assertEquals(listOf(id), store.rootFolderIds())
        assertEquals(1, store.referenceCount(id))
        assertEquals("Trabajo", store.path(id))
        assertEquals(null, store.parentOf(id))
    }

    @Test
    fun `los nombres se recortan y no se permite renombrar a vacio`() {
        val store = FolderStore()
        val id = store.createFolder("  Trabajo  ")
        assertEquals("Trabajo", store.folderName(id))
        assertFalse(store.renameFolder(id, "   "))
        assertEquals("Trabajo", store.folderName(id))
        assertTrue(store.renameFolder(id, "  Nuevo  "))
        assertEquals("Nuevo", store.folderName(id))
    }

    // ------------------------------------------------------------- contenido

    @Test
    fun `una app puede estar en varias carpetas`() {
        val store = FolderStore()
        val f1 = store.createFolder("F1")
        val f2 = store.createFolder("F2")

        assertEquals(FolderStore.AddResult.ADDED, store.addToFolder(f1, appA))
        assertEquals(FolderStore.AddResult.ADDED, store.addToFolder(f2, appA))
        assertEquals(setOf(f1, f2), store.containingFolders(appA).toSet())
        assertTrue(store.containsItem(appA))

        assertEquals(FolderStore.AddResult.ALREADY_PRESENT, store.addToFolder(f1, appA))
        assertEquals(listOf(appA), store.childrenOf(f1))
    }

    @Test
    fun `quitar una referencia no afecta a las demas ni al contenido`() {
        val store = FolderStore()
        val f1 = store.createFolder("F1")
        val f2 = store.createFolder("F2")
        store.addToFolder(f1, appA)
        store.addToFolder(f2, appA)

        assertTrue(store.removeFromFolder(f1, appA))
        assertEquals(listOf(f2), store.containingFolders(appA))
        assertEquals(listOf(appA), store.childrenOf(f2))
        assertTrue(store.removeFromFolder(f2, appA))
        assertTrue(store.containingFolders(appA).isEmpty())
    }

    @Test
    fun `con duplicados desactivados la app se mueve entre carpetas`() {
        val store = FolderStore()
        val f1 = store.createFolder("F1")
        val f2 = store.createFolder("F2")
        store.addItem(f1, appA, allowDuplicateItems = false)

        assertEquals(FolderStore.AddResult.ADDED, store.addItem(f2, appA, allowDuplicateItems = false))

        assertEquals(listOf(f2), store.containingFolders(appA))
        assertTrue(store.childrenOf(f1).isEmpty())
    }

    @Test
    fun `con duplicados activados la app permanece en ambas carpetas`() {
        val store = FolderStore()
        val f1 = store.createFolder("F1")
        val f2 = store.createFolder("F2")
        store.addItem(f1, appA, allowDuplicateItems = true)

        assertEquals(FolderStore.AddResult.ADDED, store.addItem(f2, appA, allowDuplicateItems = true))

        assertEquals(setOf(f1, f2), store.containingFolders(appA).toSet())
    }

    @Test
    fun `una carpeta siempre se mueve aunque los duplicados esten activados`() {
        val store = FolderStore()
        val p1 = store.createFolder("P1")
        val p2 = store.createFolder("P2")
        val child = store.createFolder("C")

        assertEquals(FolderStore.AddResult.ADDED, store.addItem(p1, child, allowDuplicateItems = true))
        assertEquals(FolderStore.AddResult.ADDED, store.addItem(p2, child, allowDuplicateItems = true))

        assertEquals(p2, store.parentOf(child))
        assertTrue(store.isFolderId(child))
        assertEquals(1, store.referenceCount(child))
    }

    @Test
    fun `removeItemFromAllFolders respeta la carpeta exceptuada`() {
        val store = FolderStore()
        val f1 = store.createFolder("F1")
        val f2 = store.createFolder("F2")
        val f3 = store.createFolder("F3")
        store.addToFolder(f1, appA)
        store.addToFolder(f2, appA)
        store.addToFolder(f3, appA)

        assertTrue(store.removeItemFromAllFolders(appA, exceptFolderId = f1))
        assertEquals(listOf(f1), store.containingFolders(appA))

        assertTrue(store.removeItemFromAllFolders(appA))
        assertTrue(store.containingFolders(appA).isEmpty())
    }

    // -------------------------------------------------------------- jerarquía

    @Test
    fun `mover una carpeta a otra conserva id, contenido y contador`() {
        val store = FolderStore()
        val parent = store.createFolder("Padre")
        val child = store.createFolder("Hija")
        store.addToFolder(child, appA)

        assertEquals(FolderStore.AddResult.ADDED, store.addToFolder(parent, child))

        assertEquals(parent, store.parentOf(child))
        assertEquals(listOf(child), store.childFolderIds(parent))
        assertEquals(listOf(parent), store.rootFolderIds())
        assertEquals(listOf(appA), store.childrenOf(child))
        assertEquals(1, store.referenceCount(child))
        assertEquals("Padre/Hija", store.path(child))
    }

    @Test
    fun `mover una carpeta entre padres no duplica ni pierde contenido`() {
        val store = FolderStore()
        val p1 = store.createFolder("P1")
        val p2 = store.createFolder("P2")
        val child = store.createFolder("C")
        store.addToFolder(child, appA)
        store.addToFolder(p1, child)

        assertEquals(FolderStore.MoveResult.MOVED, store.moveFolder(child, p2))

        assertEquals(p2, store.parentOf(child))
        assertTrue(store.childFolderIds(p1).isEmpty())
        assertEquals(listOf(child), store.childFolderIds(p2))
        assertEquals(listOf(appA), store.childrenOf(child))
        assertEquals(1, store.referenceCount(child))
    }

    @Test
    fun `mover una carpeta a la raiz la devuelve al App Drawer`() {
        val store = FolderStore()
        val parent = store.createFolder("P")
        val child = store.createFolder("C")
        store.addToFolder(parent, child)

        assertEquals(FolderStore.MoveResult.MOVED, store.moveFolder(child, null))

        assertNull(store.parentOf(child))
        assertTrue(store.childFolderIds(parent).isEmpty())
        assertEquals(1, store.referenceCount(child))
        assertEquals(setOf(parent, child), store.rootFolderIds().toSet())
        assertEquals(FolderStore.MoveResult.ALREADY_IN_PLACE, store.moveFolder(child, null))
    }

    @Test
    fun `no se mueve a la raiz si ya existe una carpeta homonima`() {
        val store = FolderStore()
        val parent = store.createFolder("P")
        val child = store.createFolder("C")
        store.addToFolder(parent, child)
        val rootDuplicate = store.createFolder("c")

        assertEquals(FolderStore.MoveResult.NAME_CONFLICT, store.moveFolder(child, null))

        assertEquals(parent, store.parentOf(child))
        assertEquals(listOf(child), store.childFolderIds(parent))
        assertEquals(setOf(parent, rootDuplicate), store.rootFolderIds().toSet())
    }

    @Test
    fun `no se permiten ciclos`() {
        val store = FolderStore()
        val a = store.createFolder("A")
        val b = store.createFolder("B")
        val c = store.createFolder("C")

        assertEquals(FolderStore.AddResult.WOULD_CREATE_CYCLE, store.addToFolder(a, a))
        assertEquals(FolderStore.AddResult.ADDED, store.addToFolder(a, b))
        assertEquals(FolderStore.AddResult.ADDED, store.addToFolder(b, c))

        assertEquals(FolderStore.AddResult.WOULD_CREATE_CYCLE, store.addToFolder(c, a))
        assertEquals(FolderStore.AddResult.WOULD_CREATE_CYCLE, store.addToFolder(b, a))
        assertTrue(store.canMove(c, a)) // subir una carpeta a un ancestro es válido
        assertFalse(store.canMove(a, c)) // mover un ancestro a su descendiente crea un ciclo
    }

    @Test
    fun `ancestorIds devuelve la cadena completa desde la raiz`() {
        val store = FolderStore()
        val a = store.createFolder("A")
        val b = store.createFolder("B")
        val c = store.createFolder("C")
        store.addToFolder(a, b)
        store.addToFolder(b, c)

        assertEquals(listOf(a, b, c), store.ancestorIds(c))
        assertEquals(listOf(a), store.ancestorIds(a))
        assertTrue(store.ancestorIds("folder:missing").isEmpty())
    }

    @Test
    fun `subtreeFolderIds cubre la raiz y todos los niveles anidados`() {
        val store = FolderStore()
        val a = store.createFolder("A")
        val b = store.createFolder("B")
        val c = store.createFolder("C")
        store.addToFolder(a, b)
        store.addToFolder(b, c)

        assertEquals(setOf(a, b, c), store.subtreeFolderIds(null).toSet())
        assertEquals(listOf(a, b, c), store.subtreeFolderIds(a))
        assertEquals(listOf(b, c), store.subtreeFolderIds(b))
        assertEquals(listOf(c), store.subtreeFolderIds(c))
        assertTrue(store.subtreeFolderIds("folder:missing").isEmpty())
    }

    @Test
    fun `subtreeItemIds incluye contenido y carpetas de todo el subarbol`() {
        val store = FolderStore()
        val a = store.createFolder("A")
        val b = store.createFolder("B")
        val c = store.createFolder("C")
        store.addToFolder(a, b)
        store.addToFolder(b, c)
        store.addToFolder(a, appA)
        store.addToFolder(b, appB)
        store.addToFolder(c, shortcut)

        assertEquals(setOf(b, c, appA, appB, shortcut), store.subtreeItemIds(null).toSet())
        assertEquals(setOf(b, appA, c, appB, shortcut), store.subtreeItemIds(a).toSet())
        assertEquals(setOf(c, appB, shortcut), store.subtreeItemIds(b).toSet())
        assertEquals(setOf(shortcut), store.subtreeItemIds(c).toSet())
    }

    @Test
    fun `nombres duplicados se buscan por nivel ignorando mayusculas y acentos`() {
        val store = FolderStore()
        val trabajo = store.createFolder("Trabajo")
        val docsTrabajo = store.createFolder("Documentos")
        store.addToFolder(trabajo, docsTrabajo)

        val personal = store.createFolder("Personal")
        val docsPersonal = store.createFolder("Documentos")
        store.addToFolder(personal, docsPersonal)

        assertEquals(trabajo, store.findFolderByName(null, "trabájo"))
        assertEquals(docsTrabajo, store.findFolderByName(trabajo, "DOCUMENTOS"))
        assertEquals(docsPersonal, store.findFolderByName(personal, "documentos"))
        assertNull(store.findFolderByName(null, "Documentos"))
        assertNull(store.findFolderByName(null, "Inexistente"))
    }

    // ------------------------------------------------------------- referencias

    @Test
    fun `el contador incluye padre, raiz, escritorio y deslizamiento`() {
        val store = FolderStore()
        val parent = store.createFolder("P")
        val child = store.createFolder("C")
        store.addToFolder(parent, child)
        store.addExternalRef("home:1", child)
        store.addExternalRef(FolderStore.LOCATION_SWIPE_LEFT, child)

        assertEquals(3, store.referenceCount(child))
        assertEquals(
            setOf(FolderStore.LOCATION_PARENT, "home:1", FolderStore.LOCATION_SWIPE_LEFT),
            store.referenceLocations(child).toSet(),
        )

        store.removeExternalRef("home:1", child)
        assertEquals(2, store.referenceCount(child))
        store.removeExternalRef(FolderStore.LOCATION_SWIPE_LEFT, child)
        assertEquals(1, store.referenceCount(child))
        assertTrue(store.isFolderId(child))
    }

    @Test
    fun `las referencias externas sobreviven al mover la carpeta`() {
        val store = FolderStore()
        val parent = store.createFolder("P")
        val child = store.createFolder("C")
        store.addExternalRef("home:2", child)
        store.addToFolder(parent, child)

        assertEquals(setOf("home:2", FolderStore.LOCATION_PARENT), store.referenceLocations(child).toSet())
        assertEquals("P/C", store.path(child))
    }

    @Test
    fun `un movimiento no elimina la carpeta aunque cambie de padre`() {
        val store = FolderStore()
        val p1 = store.createFolder("P1")
        val p2 = store.createFolder("P2")
        val child = store.createFolder("C")
        store.addToFolder(p1, child)

        store.moveFolder(child, p2)

        assertTrue(store.isFolderId(child))
        assertEquals(p2, store.parentOf(child))
    }

    // -------------------------------------------------------------- eliminación

    @Test
    fun `la ultima referencia elimina la carpeta y su contenido`() {
        val store = FolderStore()
        val folder = store.createFolder("F")
        store.addToFolder(folder, appA)

        assertTrue(store.removeExternalRef(drawer, folder))

        assertNull(store.folder(folder))
        assertTrue(store.snapshot().folders.isEmpty())
        assertEquals("", FolderStore.serialize(store.snapshot()))
    }

    @Test
    fun `quitar una carpeta de su padre la elimina si no tiene otras referencias`() {
        val store = FolderStore()
        val parent = store.createFolder("P")
        val child = store.createFolder("C")
        store.addToFolder(parent, child)

        assertTrue(store.removeFromFolder(parent, child))
        assertNull(store.folder(child))
        assertTrue(store.isFolderId(parent))
    }

    @Test
    fun `quitar una carpeta de su padre la conserva si tiene referencias externas`() {
        val store = FolderStore()
        val parent = store.createFolder("P")
        val child = store.createFolder("C")
        store.addToFolder(parent, child)
        store.addExternalRef("home:3", child)

        assertTrue(store.removeFromFolder(parent, child))

        assertTrue(store.isFolderId(child))
        assertNull(store.parentOf(child))
        assertEquals("C", store.path(child))
        assertEquals(1, store.referenceCount(child))
    }

    @Test
    fun `la eliminacion en cascada libera hijas pero respeta las referenciadas`() {
        val store = FolderStore()
        val top = store.createFolder("Top")
        val mid = store.createFolder("Mid")
        val leafOnlyHere = store.createFolder("LeafA")
        val leafShared = store.createFolder("LeafB")
        store.addToFolder(top, mid)
        store.addToFolder(mid, leafOnlyHere)
        store.addToFolder(mid, leafShared)
        store.addToFolder(mid, appA)
        store.addExternalRef("home:1", leafShared)

        store.removeExternalRef(drawer, top)

        assertNull(store.folder(top))
        assertNull(store.folder(mid))
        assertNull(store.folder(leafOnlyHere))
        assertTrue(store.isFolderId(leafShared))
        assertNull(store.parentOf(leafShared))
        assertEquals(1, store.referenceCount(leafShared))
    }

    @Test
    fun `una carpeta compartida por dos padres solo se elimina sin referencias`() {
        val store = FolderStore()
        val p1 = store.createFolder("P1")
        val p2 = store.createFolder("P2")
        val shared = store.createFolder("S")
        store.addToFolder(p1, shared)
        store.addToFolder(p2, shared) // mueve: un solo padre

        assertEquals(p2, store.parentOf(shared))
        assertTrue(store.childFolderIds(p1).isEmpty())

        store.removeExternalRef(drawer, p2)
        assertNull(store.folder(shared))
    }

    // ------------------------------------------------------------ persistencia

    @Test
    fun `serializar y deserializar conserva jerarquia, contenido y referencias`() {
        val store = FolderStore()
        val parent = store.createFolder("Trabajo, \"pro\"\tñ\nlinea2 %")
        val child = store.createFolder("Sub")
        store.addToFolder(parent, child)
        store.addToFolder(parent, shortcut)
        store.addToFolder(child, appB)
        store.addExternalRef("home:2", child)

        val text = FolderStore.serialize(store.snapshot())
        val back = FolderStore(FolderStore.deserialize(text))

        assertEquals(store.snapshot(), back.snapshot())
        assertEquals("Trabajo, \"pro\"\tñ\nlinea2 %", back.folderName(parent))
        assertEquals("Trabajo, \"pro\"\tñ\nlinea2 %/Sub", back.path(child))
    }

    @Test
    fun `texto corrupto o vacio da estado vacio`() {
        assertTrue(FolderStore(FolderStore.deserialize(null)).folderIds().isEmpty())
        assertTrue(FolderStore(FolderStore.deserialize("basura")).folderIds().isEmpty())
        assertTrue(FolderStore(FolderStore.deserialize("folders-v1\nF\tsolo-dos")).folderIds().isEmpty())
    }

    @Test
    fun `al cargar se descartan ciclos`() {
        val snapshot = FolderStore.Snapshot(
            folders = listOf(
                FolderStore.FolderData("folder:a", "A", listOf("folder:b")),
                FolderStore.FolderData("folder:b", "B", listOf("folder:a", appA)),
            ),
            externalRefs = listOf(FolderStore.ExternalRef(drawer, "folder:a")),
        )

        val store = FolderStore(snapshot)

        assertEquals(listOf("folder:b"), store.childrenOf("folder:a"))
        assertEquals(listOf(appA), store.childrenOf("folder:b"))
        assertNull(store.parentOf("folder:a"))
        assertEquals("folder:a", store.parentOf("folder:b"))
    }

    @Test
    fun `al cargar gana el primer padre y se descartan aristas duplicadas`() {
        val snapshot = FolderStore.Snapshot(
            folders = listOf(
                FolderStore.FolderData("folder:p1", "P1", listOf("folder:c")),
                FolderStore.FolderData("folder:p2", "P2", listOf("folder:c")),
                FolderStore.FolderData("folder:c", "C"),
            ),
            externalRefs = listOf(
                FolderStore.ExternalRef(drawer, "folder:p1"),
                FolderStore.ExternalRef(drawer, "folder:p2"),
            ),
        )

        val store = FolderStore(snapshot)

        assertEquals("folder:p1", store.parentOf("folder:c"))
        assertEquals(listOf("folder:c"), store.childrenOf("folder:p1"))
        assertTrue(store.childrenOf("folder:p2").isEmpty())
    }

    @Test
    fun `al cargar se descartan carpetas huerfanas y referencias colgantes`() {
        val snapshot = FolderStore.Snapshot(
            folders = listOf(
                FolderStore.FolderData("folder:a", "A", listOf("folder:ghost", appA)),
                FolderStore.FolderData("folder:orphan", "O", listOf(appA)),
            ),
            externalRefs = listOf(
                FolderStore.ExternalRef(drawer, "folder:a"),
                FolderStore.ExternalRef(drawer, "folder:missing"),
            ),
        )

        val store = FolderStore(snapshot)

        assertEquals(listOf("folder:a"), store.folderIds())
        assertEquals(listOf(appA), store.childrenOf("folder:a"))
        assertNull(store.folder("folder:orphan"))
    }

    @Test
    fun `cargar no admite que una carpeta anidada tenga referencia de raiz`() {
        val snapshot = FolderStore.Snapshot(
            folders = listOf(
                FolderStore.FolderData("folder:p", "P", listOf("folder:c")),
                FolderStore.FolderData("folder:c", "C"),
            ),
            externalRefs = listOf(
                FolderStore.ExternalRef(drawer, "folder:p"),
                FolderStore.ExternalRef(drawer, "folder:c"),
            ),
        )

        val store = FolderStore(snapshot)

        assertEquals(listOf("folder:p"), store.rootFolderIds())
        assertEquals(1, store.referenceCount("folder:c"))
    }
}
