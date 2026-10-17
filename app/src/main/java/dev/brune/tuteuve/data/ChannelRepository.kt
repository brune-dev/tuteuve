package dev.brune.tuteuve.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import java.io.File
import java.io.FileNotFoundException

/** Keeps imported channel URLs inside app-private storage; never logs source contents. */
class ChannelRepository(private val context: Context) {
    // Android excludes noBackupFilesDir even on devices that still allow device-to-device transfer.
    private val store = AtomicFile(File(context.noBackupFilesDir, "channels.json"))

    fun load(): List<Channel> {
        val data = try {
            store.openRead().use { it.readBytes() }
        } catch (_: FileNotFoundException) {
            return emptyList()
        }
        val array = JSONArray(data.toString(Charsets.UTF_8))
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(Channel(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    group = item.getString("group"),
                    streamUrl = item.getString("url"),
                    sourceName = item.getString("source"),
                ))
            }
        }
    }
}
