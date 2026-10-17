package dev.brune.tuteuve.data

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XmltvParserTest {
    private fun parse(xml: String) = XmltvParser().parse(ByteArrayInputStream(xml.toByteArray()))

    @Test fun parsesRealDurationsAndAssociatesByTvgId() {
        val guide = parse("""
            <tv>
              <channel id="doc.es"><display-name>Atlas Doc</display-name></channel>
              <programme channel="doc.es" start="20261017100000 +0200" stop="20261017103500 +0200">
                <title>Océanos &amp; mares</title><desc>Un documental.</desc>
              </programme>
              <programme channel="doc.es" start="20261017103500 +0200" stop="20261017123000 +0200">
                <title>La tierra</title>
              </programme>
            </tv>
        """.trimIndent())
        val programmes = EpgIndex(guide).programmesFor(Channel("1", "Otro nombre", "Documentales",
            "https://example.org/live", "Prueba", "doc.es"))
        assertEquals(2, programmes.size)
        assertEquals(35L * 60_000, programmes[0].endMillis - programmes[0].startMillis)
        assertEquals(115L * 60_000, programmes[1].endMillis - programmes[1].startMillis)
        assertEquals("Océanos & mares", programmes[0].title)
    }

    @Test fun nameFallbackRequiresUniqueXmltvChannel() {
        val guide = parse("""
            <tv>
              <channel id="a"><display-name>Uno</display-name></channel>
              <channel id="b"><display-name>Uno</display-name></channel>
              <programme channel="a" start="202610171000 +0200" stop="202610171100 +0200"><title>A</title></programme>
              <programme channel="b" start="202610171000 +0200" stop="202610171100 +0200"><title>B</title></programme>
            </tv>
        """.trimIndent())
        val channel = Channel("1", "Uno", "General", "https://example.org/live", "Prueba")
        assertTrue(EpgIndex(guide).programmesFor(channel).isEmpty())
    }

    @Test fun rejectsDoctypeAndExternalEntities() {
        val result = runCatching { parse("""
            <!DOCTYPE tv [<!ENTITY leaked SYSTEM "file:///secret">]>
            <tv><programme channel="a" start="202610171000 +0200" stop="202610171100 +0200">
            <title>&leaked;</title></programme></tv>
        """.trimIndent()) }
        assertTrue(result.isFailure)
    }

    @Test fun stripsSimpleXtreamDoctypeWithoutResolvingDtd() {
        val guide = parse("""
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE tv SYSTEM "xmltv.dtd">
            <tv><programme channel="a" start="202610171000 +0200" stop="202610171100 +0200">
            <title>Programa seguro</title></programme></tv>
        """.trimIndent())
        assertEquals("Programa seguro", guide.programmes.single().title)
    }

    @Test fun rejectsExcessiveNesting() {
        val nested = "<tv>" + "<x>".repeat(40) + "</x>".repeat(40) + "</tv>"
        assertTrue(runCatching { parse(nested) }.isFailure)
    }

    @Test fun skipsInvalidAndEndedBeforeStarted() {
        val guide = parse("""
            <tv>
              <programme channel="a" start="202610171200 +0200" stop="202610171100 +0200"><title>Wrong</title></programme>
              <programme channel="a" start="nonsense" stop="202610171100 +0200"><title>Wrong</title></programme>
              <programme channel="a" start="202610171300 +0200" stop="202610171400 +0200"><title>Valid</title></programme>
            </tv>
        """.trimIndent())
        assertEquals(listOf("Valid"), guide.programmes.map { it.title })
    }
}
