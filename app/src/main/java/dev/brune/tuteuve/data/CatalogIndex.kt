package dev.brune.tuteuve.data

data class CatalogFolder(val path: List<String>, val name: String, val channelCount: Int)

/** Immutable lookup built once per import. Only explicit " / " separators create nesting. */
class CatalogIndex(private val channels: List<Channel>) {
    private val channelEntries = channels.map { it to folderPath(it.group) }
    private val counts = buildMap<List<String>, Int> {
        channelEntries.forEach { (_, path) ->
            for (depth in 1..path.size) {
                val prefix = path.take(depth)
                put(prefix, (get(prefix) ?: 0) + 1)
            }
        }
    }

    fun folders(parent: List<String>): List<CatalogFolder> = counts
        .asSequence()
        .filter { (path, _) -> path.size == parent.size + 1 && path.take(parent.size) == parent }
        .map { (path, count) -> CatalogFolder(path, path.last(), count) }
        .sortedBy { it.name.lowercase() }
        .toList()

    fun channels(parent: List<String>, query: String = ""): List<Channel> {
        val term = query.trim()
        return channelEntries.filter { (channel, path) ->
            path.take(parent.size) == parent &&
                (term.isEmpty() || channel.name.contains(term, ignoreCase = true))
        }.map { it.first }
    }

    companion object {
        fun folderPath(group: String): List<String> = group.split(" / ")
            .map(String::trim)
            .filter(String::isNotEmpty)
            .ifEmpty { listOf("Sin carpeta") }
    }
}
