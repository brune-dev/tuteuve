package dev.brune.tuteuve.data

import java.io.InputStream
import java.io.FilterReader
import java.io.InputStreamReader
import java.io.Reader
import java.io.PushbackReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.SAXParserFactory
import javax.xml.parsers.ParserConfigurationException
import org.xml.sax.InputSource
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

data class EpgProgram(
    val channelKey: String,
    val startMillis: Long,
    val endMillis: Long,
    val title: String,
    val description: String,
)

data class EpgGuide(val channelNames: Map<String, String>, val programmes: List<EpgProgram>)

/** Index once per imported guide; ambiguous name-only matches intentionally remain unlinked. */
class EpgIndex(guide: EpgGuide) {
    private val byKey = guide.programmes.groupBy { it.channelKey }
    private val byNormalizedKey = byKey.keys.groupBy { it.lowercase(Locale.ROOT) }
    private val aliases = guide.channelNames.entries.groupBy { it.value.trim().lowercase(Locale.ROOT) }

    fun programmesFor(channel: Channel): List<EpgProgram> {
        if (channel.epgId.isNotBlank()) {
            byKey[channel.epgId]?.let { return it }
            byNormalizedKey[channel.epgId.lowercase(Locale.ROOT)]?.singleOrNull()?.let { return byKey[it].orEmpty() }
        }
        val key = channel.name.trim().lowercase(Locale.ROOT)
        val candidates = (aliases[key]?.map { it.key }.orEmpty() + byNormalizedKey[key].orEmpty()).distinct()
        return if (candidates.size == 1) byKey[candidates.single()].orEmpty() else emptyList()
    }
}

/** Bounded SAX parsing; a harmless XMLTV doctype is removed before SAX sees it. */
class XmltvParser {
    fun parse(input: InputStream): EpgGuide {
        val factory = try { SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
        } } catch (_: Exception) { throw XmltvConfigurationFailure() }
        val names = LinkedHashMap<String, String>()
        val programmes = ArrayList<EpgProgram>()
        val handler = object : DefaultHandler() {
            var depth = 0
            override fun resolveEntity(publicId: String?, systemId: String?): InputSource {
                throw SAXException("External entities are not allowed")
            }
            var channelId = ""
            var programChannel = ""
            var start = 0L
            var end = 0L
            var title = ""
            var description = ""
            var collecting = ""
            val value = StringBuilder()

            override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
                depth++
                if (depth > MAX_DEPTH) throw SAXException("XMLTV nesting limit")
                when (qName) {
                    "channel" -> channelId = attributes.getValue("id").orEmpty().take(MAX_TEXT)
                    "programme" -> {
                        programChannel = attributes.getValue("channel").orEmpty().take(MAX_TEXT)
                        start = parseDate(attributes.getValue("start")) ?: 0L
                        end = parseDate(attributes.getValue("stop")) ?: 0L
                        title = ""
                        description = ""
                    }
                    "display-name", "title", "desc" -> {
                        collecting = qName
                        value.setLength(0)
                    }
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (collecting.isNotEmpty() && value.length < MAX_TEXT) {
                    value.append(ch, start, length.coerceAtMost(MAX_TEXT - value.length))
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String) {
                when (qName) {
                    "display-name" -> if (channelId.isNotBlank() && channelId !in names) {
                        names[channelId] = value.toString().trim()
                        if (names.size > MAX_CHANNELS) throw SAXException("Límite de canales XMLTV")
                    }
                    "title" -> title = value.toString().trim()
                    "desc" -> description = value.toString().trim()
                    "programme" -> {
                        if (programChannel.isNotBlank() && start > 0 && end > start && title.isNotBlank()) {
                            programmes += EpgProgram(programChannel, start, end, title, description)
                            if (programmes.size > MAX_PROGRAMMES) throw SAXException("Límite de programas XMLTV")
                        }
                        programChannel = ""
                    }
                    "channel" -> channelId = ""
                }
                if (qName == collecting) collecting = ""
                depth--
            }
        }
        try {
            SafeXmltvReader(InputStreamReader(BoundedXmlInputStream(input), Charsets.UTF_8)).use { reader ->
                factory.newSAXParser().parse(InputSource(reader), handler)
            }
        }
        catch (_: ParserConfigurationException) { throw XmltvConfigurationFailure() }
        require(programmes.isNotEmpty()) { "Guía XMLTV sin programas válidos" }
        return EpgGuide(names, programmes.sortedWith(compareBy(EpgProgram::channelKey, EpgProgram::startMillis)))
    }

    private fun parseDate(raw: String?): Long? {
        val match = DATE.matchEntire(raw?.trim().orEmpty()) ?: return null
        val date = match.groupValues[1] + match.groupValues[2].ifEmpty { "00" }
        val zone = match.groupValues[3].ifEmpty { "+0000" }
        return runCatching {
            SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse("$date $zone")?.time
        }.getOrNull()
    }

    private companion object {
        const val MAX_TEXT = 2_048
        const val MAX_CHANNELS = 20_000
        const val MAX_PROGRAMMES = 80_000
        const val MAX_DEPTH = 32
        val DATE = Regex("^(\\d{12})(\\d{2})?\\s*([+-]\\d{4})?$")
    }
}

class XmltvConfigurationFailure : Exception()

private class BoundedXmlInputStream(private val source: InputStream) : InputStream() {
    private var count = 0L
    override fun read(): Int = source.read().also { if (it >= 0) checkLimit(1) }
    override fun read(bytes: ByteArray, offset: Int, length: Int): Int =
        source.read(bytes, offset, length).also { if (it > 0) checkLimit(it) }
    override fun close() = source.close()
    private fun checkLimit(read: Int) {
        count += read
        require(count <= 60L * 1024 * 1024) { "Guía XMLTV demasiado grande" }
    }
}

/** Xtream XMLTV often declares xmltv.dtd. Strip only a simple declaration, never fetch it. */
private class SafeXmltvReader(source: Reader) : FilterReader(source) {
    private val input = PushbackReader(source, 8)
    private val doctype = Regex("<!DOCTYPE\\s+tv\\s+SYSTEM\\s+([\"'])[^\"'<>\\[\\]]{1,200}\\1\\s*>", RegexOption.IGNORE_CASE)

    override fun read(): Int {
        val next = input.read()
        if (next != '<'.code) return next
        val lookahead = CharArray(8)
        var count = 0
        while (count < lookahead.size) {
            val character = input.read()
            if (character < 0) break
            lookahead[count++] = character.toChar()
        }
        val tag = "<" + if (count > 0) String(lookahead, 0, count) else ""
        if (tag.startsWith("<!ENTITY", ignoreCase = true)) throw java.io.IOException("Entity declarations are not allowed")
        if (tag.startsWith("<!DOCTYPE", ignoreCase = true)) {
            val declaration = StringBuilder(tag)
            while (declaration.length <= 256 && declaration.last() != '>') {
                val character = input.read()
                if (character < 0) break
                declaration.append(character.toChar())
            }
            if (!doctype.matches(declaration.toString())) throw java.io.IOException("Unsupported DTD")
            return ' '.code
        }
        if (count > 0) input.unread(lookahead, 0, count)
        return next
    }

    override fun read(buffer: CharArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        var count = 0
        while (count < length) {
            val next = read()
            if (next < 0) break
            buffer[offset + count++] = next.toChar()
        }
        return if (count == 0) -1 else count
    }

    override fun close() = input.close()
}
