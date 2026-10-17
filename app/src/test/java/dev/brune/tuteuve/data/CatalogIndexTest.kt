package dev.brune.tuteuve.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogIndexTest {
    private val channels = listOf(
        Channel("1", "Noticias", "ES / General", "https://example.org/1", "Test"),
        Channel("2", "Cine", "ES / Cine", "https://example.org/2", "Test"),
        Channel("3", "Documental", "ES / Cine", "https://example.org/3", "Test"),
        Channel("4", "Música", "Radio", "https://example.org/4", "Test"),
    )

    @Test fun nestedFoldersCountAllDescendants() {
        val index = CatalogIndex(channels)
        assertEquals(listOf("ES" to 3, "Radio" to 1), index.folders(emptyList()).map { it.name to it.channelCount })
        assertEquals(listOf("Cine" to 2, "General" to 1), index.folders(listOf("ES")).map { it.name to it.channelCount })
        assertEquals(2, index.channels(listOf("ES", "Cine")).size)
    }

    @Test fun searchStaysWithinCurrentFolder() {
        val index = CatalogIndex(channels)
        assertEquals(listOf("2"), index.channels(listOf("ES"), "CINE").map { it.id })
        assertEquals(emptyList<Channel>(), index.channels(listOf("Radio"), "cine"))
    }

    @Test fun aPlainSlashDoesNotInventHierarchy() {
        assertEquals(listOf("Arte/Cultura"), CatalogIndex.folderPath("Arte/Cultura"))
    }
}
