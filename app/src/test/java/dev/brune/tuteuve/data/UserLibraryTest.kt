package dev.brune.tuteuve.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserLibraryTest {
    @Test fun favoritesAndListsRemainIndependent() {
        val library = UserLibrary().addList("Deportes").toggleFavorite("a")
            .toggleInList("Deportes", "b")
        assertTrue("a" in library.favorites)
        assertFalse("a" in library.lists.getValue("Deportes"))
        assertTrue("b" in library.lists.getValue("Deportes"))
        assertFalse("b" in library.favorites)
    }

    @Test fun recentIsUniqueBoundedAndPrunedWithRemovedSources() {
        val played = (0..55).fold(UserLibrary()) { state, number -> state.recordPlayed(number.toString()) }
            .recordPlayed("55")
        assertEquals(50, played.recent.size)
        assertEquals("55", played.recent.first())
        val pruned = played.toggleFavorite("55").addList("Mi lista")
            .toggleInList("Mi lista", "55").retainChannels(setOf("10"))
        assertTrue(pruned.favorites.isEmpty())
        assertTrue(pruned.lists.getValue("Mi lista").isEmpty())
        assertEquals(listOf("10"), pruned.recent)
    }
}
