package app.olauncher.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsBackupTest {

    @Test
    fun `round trips every supported value type`() {
        val values = mapOf(
            "boolean" to true,
            "int" to 42,
            "long" to 1_756_000_000_000L,
            "float" to 1.5f,
            "double" to 2.25,
            "string" to "Trabajo/Documentos\tcon salto\n",
            "set" to linkedSetOf("uno", "dos"),
            "folders" to "folders-v1\nF\tfolder:1\tInbox\t\n",
        )

        val decoded = PrefsBackup.decode(PrefsBackup.encode(values))!!

        assertEquals(true, decoded["boolean"])
        assertEquals(42, decoded["int"])
        assertEquals(1_756_000_000_000L, decoded["long"])
        assertEquals(1.5f, decoded["float"])
        assertEquals(2.25, decoded["double"])
        assertEquals("Trabajo/Documentos\tcon salto\n", decoded["string"])
        assertEquals(setOf("uno", "dos"), decoded["set"])
        assertEquals("folders-v1\nF\tfolder:1\tInbox\t\n", decoded["folders"])
    }

    @Test
    fun `round trips an empty prefs map`() {
        assertEquals(emptyMap<String, Any>(), PrefsBackup.decode(PrefsBackup.encode(emptyMap<String, Any>())))
    }

    @Test
    fun `skips values with unsupported types`() {
        val values = mapOf<String, Any>(
            "valid" to "value",
            "unsupported" to Any(),
        )

        val decoded = PrefsBackup.decode(PrefsBackup.encode(values))!!

        assertEquals("value", decoded["valid"])
        assertFalse(decoded.containsKey("unsupported"))
    }

    @Test
    fun `rejects malformed json`() {
        assertNull(PrefsBackup.decode("this is not json"))
    }

    @Test
    fun `rejects a different format`() {
        assertNull(
            PrefsBackup.decode(
                """{"format":"other","version":1,"values":{}}"""
            )
        )
    }

    @Test
    fun `rejects unsupported versions`() {
        assertNull(
            PrefsBackup.decode(
                """{"format":"olauncher-backup","version":2,"values":{}}"""
            )
        )
        assertNull(
            PrefsBackup.decode(
                """{"format":"olauncher-backup","version":0,"values":{}}"""
            )
        )
    }

    @Test
    fun `rejects missing values or unknown entry types`() {
        assertNull(
            PrefsBackup.decode(
                """{"format":"olauncher-backup","version":1}"""
            )
        )
        assertNull(
            PrefsBackup.decode(
                """{"format":"olauncher-backup","version":1,"values":{"key":{"t":"unknown","v":1}}}"""
            )
        )
        assertNull(
            PrefsBackup.decode(
                """{"format":"olauncher-backup","version":1,"values":{"key":{"t":"i","v":"not a number"}}}"""
            )
        )
    }

    @Test
    fun `accepts a valid file with no preferences`() {
        val decoded = PrefsBackup.decode(
            """{"format":"olauncher-backup","version":1,"values":{}}"""
        )
        assertTrue(decoded != null)
        assertTrue(decoded!!.isEmpty())
    }
}
