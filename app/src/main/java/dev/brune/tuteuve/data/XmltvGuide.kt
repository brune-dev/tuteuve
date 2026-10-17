package dev.brune.tuteuve.data

import java.io.InputStream
import java.io.FilterReader
import java.io.InputStreamReader
import java.io.Reader
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
        if (channel.epgId.isNotBlank()) return byKey[channel.epgId].orEmpty()
        val key = channel.name.trim().lowercase(Locale.ROOT)
        val candidates = (aliases[key]?.map { it.key }.orEmpty() + byNormalizedKey[key].orEmpty()).distinct()
        return if (candidates.size == 1) byKey[candidates.single()].orEmpty() else emptyList()
    }
}

/** Bounded SAX parsing; the reader rejects DTDs before the platform parser sees them. */
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
            RejectingDtdReader(InputStreamReader(BoundedXmlInputStream(input), Charsets.UTF_8)).use { reader ->
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

/** Some Android SAX providers reject security feature flags; blocking DTD input works on each provider. */
private class RejectingDtdReader(source: Reader) : FilterReader(source) {
    private var carry = ""

    override fun read(): Int = super.read().also { value ->
        if (value >= 0) scan(value.toChar().toString())
    }

    override fun read(buffer: CharArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { count ->
            if (count > 0) scan(String(buffer, offset, count))
        }

    private fun scan(chunk: String) {
        val sample = carry + chunk
        if (sample.contains("<!DOCTYPE", ignoreCase = true) ||
            sample.contains("<!ENTITY", ignoreCase = true)) {
            throw java.io.IOException("DTD is not allowed")
        }
        carry = sample.takeLast(8)
    }
}
