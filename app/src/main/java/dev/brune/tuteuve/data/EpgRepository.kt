package dev.brune.tuteuve.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import org.json.JSONArray
import org.json.JSONObject

/** Stores guide data in app-private, backup-excluded storage without the feed address. */
class EpgRepository(context: Context) {
    private val store = AtomicFile(File(context.noBackupFilesDir, "guide.json"))

    fun load(): EpgGuide? {
        val bytes = try { store.openRead().use { it.readBytes() } }
        catch (_: FileNotFoundException) { return null }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        val namesObject = root.getJSONObject("channels")
        val names = buildMap {
            namesObject.keys().forEach { put(it, namesObject.getString(it)) }
        }
        val items = root.getJSONArray("programmes")
        val programmes = buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONArray(index)
                add(EpgProgram(item.getString(0), item.getLong(1), item.getLong(2),
                    item.getString(3), item.getString(4)))
            }
        }
        return EpgGuide(names, programmes)
    }

    fun save(guide: EpgGuide) {
        val channels = JSONObject().apply { guide.channelNames.forEach { (id, name) -> put(id, name) } }
        val programmes = JSONArray().apply {
            guide.programmes.forEach { item ->
                put(JSONArray().put(item.channelKey).put(item.startMillis).put(item.endMillis)
                    .put(item.title).put(item.description))
            }
        }
        val data = JSONObject().put("channels", channels).put("programmes", programmes)
        val output = store.startWrite()
        try {
            output.write(data.toString().toByteArray(Charsets.UTF_8))
            store.finishWrite(output)
        } catch (error: Exception) {
            store.failWrite(output)
            throw error
        }
    }

    fun clear() {
        store.delete()
    }
}
