package dev.brune.tuteuve.data

/** User choices are stored as channel IDs only, never as playlist or stream addresses. */
data class UserLibrary(
    val favorites: Set<String> = emptySet(),
    val lists: Map<String, Set<String>> = emptyMap(),
    val recent: List<String> = emptyList(),
) {
    fun toggleFavorite(id: String): UserLibrary = copy(
        favorites = if (id in favorites) favorites - id else favorites + id,
    )

    fun addList(rawName: String): UserLibrary {
        val name = rawName.trim()
        require(name.isNotEmpty() && name.length <= 60) { "Nombre de lista no válido" }
        require(lists.keys.none { it.equals(name, ignoreCase = true) }) { "La lista ya existe" }
        require(lists.size < 50) { "Demasiadas listas" }
        return copy(lists = lists + (name to emptySet()))
    }

    fun removeList(name: String): UserLibrary = copy(lists = lists - name)

    fun toggleInList(name: String, id: String): UserLibrary {
        val members = lists[name] ?: return this
        return copy(lists = lists + (name to if (id in members) members - id else members + id))
    }

    fun recordPlayed(id: String): UserLibrary = copy(recent = (listOf(id) + recent.filterNot { it == id }).take(50))

    fun retainChannels(ids: Set<String>): UserLibrary = copy(
        favorites = favorites intersect ids,
        lists = lists.mapValues { (_, members) -> members intersect ids },
        recent = recent.filter { it in ids },
    )
}
