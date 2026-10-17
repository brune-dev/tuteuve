package dev.brune.tuteuve.data

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.security.MessageDigest

/** Parses only metadata and HTTPS media URLs. It never opens a URL from a playlist. */
class M3uParser {
    fun parse(input: InputStream, sourceName: String): ImportResult {
        val reader = BufferedReader(InputStreamReader(LimitedInputStream(input, MAX_BYTES), Charsets.UTF_8))
        val channels = ArrayList<Channel>()
        var lineCount = 0
        var ignored = 0
        var httpEntries = 0
        var pending: Metadata? = null
        while (true) {
            val raw = readBoundedLine(reader) ?: break
            lineCount++
            require(lineCount <= MAX_LINES) { "La lista supera el límite de líneas" }
            val line = raw.trim().trimStart('\uFEFF')
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> pending = parseMetadata(line)
                line.isEmpty() || line.startsWith('#') -> Unit
                else -> {
                    val metadata = pending
                    pending = null
                    val url = safeStreamUrl(line)
                    if (metadata == null || url == null) {
                        ignored++
                        if (line.startsWith("http://", ignoreCase = true)) httpEntries++
                    } else {
                        require(channels.size < MAX_CHANNELS) { "La lista supera el límite de canales" }
                        channels += Channel(
                            id = stableId(sourceName, url),
                            name = metadata.name,
                            group = metadata.group,
                            streamUrl = url,
                            sourceName = sourceName,
                            epgId = metadata.epgId,
                        )
                    }
                }
            }
        }
        return ImportResult(channels, ignored, httpEntries)
    }

    private fun readBoundedLine(reader: BufferedReader): String? {
        val line = StringBuilder()
        while (true) {
            val character = reader.read()
            if (character == -1) return if (line.isEmpty()) null else line.toString()
            if (character == '\n'.code) return line.toString()
            require(line.length < MAX_LINE_LENGTH) { "La lista contiene una línea demasiado larga" }
            line.append(character.toChar())
        }
    }

    private fun parseMetadata(line: String): Metadata? {
        val name = line.substringAfterLast(',', "").trim().take(MAX_NAME_LENGTH)
        if (name.isBlank()) return null
        val group = GROUP_ATTRIBUTE.find(line)?.groupValues?.get(1)?.trim().orEmpty()
            .take(MAX_NAME_LENGTH).ifBlank { "Sin carpeta" }
        val epgId = TVG_ID_ATTRIBUTE.find(line)?.groupValues?.get(1)?.trim().orEmpty()
            .take(MAX_NAME_LENGTH)
        return Metadata(name, group, epgId)
    }

    private fun safeStreamUrl(value: String): String? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        // The first slice supports HTTPS only. This also prevents credentials crossing cleartext HTTP.
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) return null
        if (uri.userInfo != null || uri.fragment != null) return null
        val host = uri.host.lowercase()
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return null
        if (isPrivateIpLiteral(host)) return null
        return uri.toASCIIString()
    }

    private fun isPrivateIpLiteral(host: String): Boolean {
        if (host.contains(':')) return true // IPv6 needs a dedicated allowlist review.
        if (host.startsWith("0x") || host.split('.').any { it.startsWith("0x") }) return true
        if (!host.matches(NUMERIC_HOST)) return false
        val octets = host.split('.').map { it.toIntOrNull() }
        // Reject short, decimal-integer and octal-like addresses rather than letting the OS reinterpret them.
        if (octets.size != 4 || octets.any { it == null || it !in 0..255 } ||
            host.split('.').any { it.length > 1 && it.startsWith('0') }) return true
        val a = octets[0]!!
        val b = octets[1]!!
        return a == 0 || a == 10 || a == 127 || a >= 224 ||
            (a == 100 && b in 64..127) || (a == 169 && b == 254) ||
            (a == 172 && b in 16..31) || (a == 192 && b == 168) ||
            (a == 198 && b in 18..19)
    }

    private fun stableId(source: String, url: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$source\u0000$url".toByteArray(Charsets.UTF_8))
        return bytes.take(12).joinToString("") { "%02x".format(it) }
    }

    private data class Metadata(val name: String, val group: String, val epgId: String)

    private companion object {
        const val MAX_BYTES = 20L * 1024 * 1024
        const val MAX_LINES = 60_000
        const val MAX_CHANNELS = 20_000
        const val MAX_LINE_LENGTH = 8_192
        const val MAX_NAME_LENGTH = 160
        val GROUP_ATTRIBUTE = Regex("""group-title\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
        val TVG_ID_ATTRIBUTE = Regex("""tvg-id\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
        val NUMERIC_HOST = Regex("[0-9.]+")
    }
}

private class LimitedInputStream(private val source: InputStream, private val limit: Long) : InputStream() {
    private var count = 0L

    override fun read(): Int {
        val value = source.read()
        if (value != -1) checkLimit(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = source.read(buffer, offset, length)
        if (read > 0) checkLimit(read)
        return read
    }

    private fun checkLimit(bytes: Int) {
        count += bytes
        require(count <= limit) { "La lista supera el tamaño permitido" }
    }
}
