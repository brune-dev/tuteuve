package dev.brune.tuteuve.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamEpgAddressTest {
    @Test fun encodesCredentialsInXtreamXmltvUrl() {
        assertEquals(
            "https://provider.example/xmltv.php?username=a%2Bb&password=p%26q",
            XtreamEpgAddress.create("https://provider.example/", "a+b", "p&q"),
        )
    }

    @Test fun rejectsHttpAndLocalServer() {
        assertTrue(runCatching { XtreamEpgAddress.create("http://provider.example", "u", "p") }.isFailure)
        assertTrue(runCatching { XtreamEpgAddress.create("https://localhost", "u", "p") }.isFailure)
    }
}
