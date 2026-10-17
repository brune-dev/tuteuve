package dev.brune.tuteuve.data

data class Channel(
    val id: String,
    val name: String,
    val group: String,
    val streamUrl: String,
    val sourceName: String,
)

data class ImportResult(
    val channels: List<Channel>,
    val ignoredEntries: Int,
)
