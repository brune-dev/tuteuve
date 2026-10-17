package dev.brune.tuteuve.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

/** App-private, backup-excluded storage. It never contains a channel URL or credential. */
class UserLibraryRepository(context: Context) {
    private val store = AtomicFile(File(context.noBackupFilesDir, "library.json"))

    fun load(): UserLibrary {
        val bytes = try { store.openRead().use { it.readBytes() } }
        catch (_: FileNotFoundException) { return UserLibrary() }
        val data = JSONObject(bytes.toString(Charsets.UTF_8))
        val favorites = data.optJSONArray("favorites").toStrings().toSet()
        val recent = data.optJSONArray("recent").toStrings().take(50)
        val listsObject = data.optJSONObject("lists")
        val lists = buildMap {
            if (listsObject != null) for (name in listsObject.keys()) {
                put(name, listsObject.optJSONArray(name).toStrings().toSet())
            }
        }
        return UserLibrary(favorites, lists, recent)
    }

    fun save(library: UserLibrary) {
        val data = JSONObject().apply {
            put("favorites", JSONArray(library.favorites.sorted()))
            put("recent", JSONArray(library.recent))
            put("lists", JSONObject().apply {
                library.lists.forEach { (name, members) -> put(name, JSONArray(members.sorted())) }
            })
        }
        val stream = store.startWrite()
        try {
            stream.write(data.toString().toByteArray(Charsets.UTF_8))
            store.finishWrite(stream)
        } catch (error: Exception) {
            store.failWrite(stream)
            throw error
        }
    }
}

private fun JSONArray?.toStrings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { getString(it) }
