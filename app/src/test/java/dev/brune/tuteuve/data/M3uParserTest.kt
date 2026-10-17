package dev.brune.tuteuve.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class M3uParserTest {
    private val parser = M3uParser()

    @Test fun importsHttpsAndGroupsWithoutOpeningStreams() {
        val result = parse("""
            #EXTM3U
            #EXTINF:-1 group-title="Documentales",Canal de prueba
            https://example.org/live/test.m3u8
        """.trimIndent())
        assertEquals(1, result.channels.size)
        assertEquals("Documentales", result.channels.single().group)
        assertEquals("Canal de prueba", result.channels.single().name)
        assertEquals(0, result.ignoredEntries)
    }

    @Test fun ignoresCleartextLocalAndEmbeddedCredentials() {
        val result = parse("""
            #EXTINF:-1,HTTP
            http://example.org/live
            #EXTINF:-1,Local
            https://127.0.0.1/live
            #EXTINF:-1,Router
            https://192.168.1.1/live
            #EXTINF:-1,User info
            https://name:secret@example.org/live
        """.trimIndent())
        assertTrue(result.channels.isEmpty())
        assertEquals(4, result.ignoredEntries)
        assertEquals(1, result.httpEntries)
    }

    @Test fun rejectsOversizedLineBeforeSavingAnything() {
        val data = "#EXTINF:-1,${"x".repeat(8_200)}\nhttps://example.org/live"
        val failure = runCatching { parse(data) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test fun ignoresUrlWithoutMetadata() {
        val result = parse("https://example.org/live")
        assertTrue(result.channels.isEmpty())
        assertEquals(1, result.ignoredEntries)
    }

    private fun parse(text: String): ImportResult =
        ByteArrayInputStream(text.toByteArray()).use { parser.parse(it, "Prueba") }
}
