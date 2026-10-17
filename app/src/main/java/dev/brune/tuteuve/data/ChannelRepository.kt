package dev.brune.tuteuve.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

/** Keeps imported channel URLs inside app-private storage; never logs source contents. */
class ChannelRepository(private val context: Context) {
    private val store = AtomicFile(File(context.filesDir, "channels.json"))
    private val parser = M3uParser()

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

    fun import(uri: Uri): ImportResult {
        val sourceName = displayName(uri)
        val parsed = context.contentResolver.openInputStream(uri)?.use { parser.parse(it, sourceName) }
            ?: error("No se pudo abrir el archivo")
        require(parsed.channels.isNotEmpty()) { "La lista no contiene emisiones HTTPS compatibles" }
        val existing = load().filterNot { it.sourceName == sourceName }
        val merged = (existing + parsed.channels).distinctBy { it.id }
        save(merged)
        return parsed
    }

    fun removeSource(name: String) = save(load().filterNot { it.sourceName == name })

    private fun displayName(uri: Uri): String {
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?.trim()?.take(80)
        return name?.ifBlank { null } ?: "Fuente importada"
    }

    private fun save(channels: List<Channel>) {
        val array = JSONArray()
        channels.forEach { channel ->
            array.put(JSONObject().apply {
                put("id", channel.id)
                put("name", channel.name)
                put("group", channel.group)
                put("url", channel.streamUrl)
                put("source", channel.sourceName)
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
