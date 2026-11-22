package app.olauncher.backup

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Copia y restauración de las preferencias del launcher.
 *
 * Se serializa el contenido completo de `SharedPreferences("app.olauncher")`, incluidas las claves
 * dinámicas (nombres de apps) y los datos de carpetas. El archivo es JSON versionado y legible.
 * La restauración sustituye todas las claves en una única operación.
 */
object PrefsBackup {

    const val PREFS_FILENAME = "app.olauncher"

    private const val FORMAT = "olauncher-backup"
    private const val VERSION = 1
    private const val MAX_BACKUP_BYTES = 5 * 1024 * 1024

    fun export(context: Context): String = encode(sharedPreferences(context).all)

    fun write(context: Context, output: OutputStream) {
        output.write(export(context).toByteArray(Charsets.UTF_8))
    }

    /** Lee el archivo limitando el tamaño. Devuelve `null` si es demasiado grande o falla la E/S. */
    fun read(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            total += read
            if (total > MAX_BACKUP_BYTES) return null
            buffer.write(chunk, 0, read)
        }
        return buffer.toString(Charsets.UTF_8.name())
    }

    /** Sustituye por completo el contenido de las preferencias por [values]. */
    @Suppress("UseKtx")
    fun restore(context: Context, values: Map<String, Any>): Boolean {
        val editor = sharedPreferences(context).edit().clear()
        values.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        return editor.commit()
    }

    private fun sharedPreferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    internal fun encode(values: Map<String, *>): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("createdAt", System.currentTimeMillis())

        val encodedValues = JSONObject()
        values.forEach { (key, value) ->
            encodeValue(value)?.let { encodedValues.put(key, it) }
        }
        root.put("values", encodedValues)
        return root.toString(2)
    }

    internal fun decode(text: String): Map<String, Any>? = try {
        decodeRoot(JSONObject(text))
    } catch (_: Exception) {
        null
    }

    private fun decodeRoot(root: JSONObject): Map<String, Any>? {
        if (root.optString("format") != FORMAT) return null
        val version = root.optInt("version", -1)
        if (version <= 0 || version > VERSION) return null

        val encodedValues = root.optJSONObject("values") ?: return null
        val result = LinkedHashMap<String, Any>()
        val keys = encodedValues.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = encodedValues.optJSONObject(key) ?: return null
            result[key] = decodeValue(entry) ?: return null
        }
        return result
    }

    private fun encodeValue(value: Any?): JSONObject? = when (value) {
        is Boolean -> entry("b", value)
        is Int -> entry("i", value)
        is Long -> entry("l", value)
        is Float -> entry("f", value)
        is Double -> entry("d", value)
        is String -> entry("s", value)
        is Set<*> -> {
            val array = JSONArray()
            value.filterIsInstance<String>().forEach { array.put(it) }
            entry("ss", array)
        }

        else -> null
    }

    private fun decodeValue(entry: JSONObject): Any? = when (entry.optString("t")) {
        "b" -> entry.getBoolean("v")
        "i" -> entry.getInt("v")
        "l" -> entry.getLong("v")
        "f" -> entry.getDouble("v").toFloat()
        "d" -> entry.getDouble("v")
        "s" -> entry.getString("v")
        "ss" -> {
            val array = entry.getJSONArray("v")
            val set = LinkedHashSet<String>()
            for (index in 0 until array.length()) set.add(array.getString(index))
            set
        }

        else -> null
    }

    private fun entry(type: String, value: Any): JSONObject =
        JSONObject().put("t", type).put("v", value)
}
