package dev.brune.tuteuve.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
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
                    epgId = item.optString("epgId"),
                ))
            }
        }
    }

    /** Replace by display name only after a complete and compatible import. */
    fun replaceSource(name: String, result: ImportResult): List<Channel> {
        if (result.channels.isEmpty()) throw NoCompatibleChannels(result.httpEntries)
        val merged = (load().filterNot { it.sourceName == name } + result.channels)
            .distinctBy { it.id }
        require(merged.size <= 20_000) { "Demasiados canales" }
        writeChannels(merged)
        return merged
    }

    fun removeSource(name: String): List<Channel> {
        val remaining = load().filterNot { it.sourceName == name }
        writeChannels(remaining)
        return remaining
    }

    private fun writeChannels(channels: List<Channel>) {
        val array = JSONArray()
        channels.forEach { channel ->
            array.put(JSONObject().apply {
                put("id", channel.id)
                put("name", channel.name)
                put("group", channel.group)
                put("url", channel.streamUrl)
                put("source", channel.sourceName)
                put("epgId", channel.epgId)
            })
        }
        val stream = store.startWrite()
        try {
            stream.write(array.toString().toByteArray(Charsets.UTF_8))
            store.finishWrite(stream)
        } catch (error: Exception) {
            store.failWrite(stream)
            throw error
        }
    }
}

class NoCompatibleChannels(val httpEntries: Int) : Exception()
