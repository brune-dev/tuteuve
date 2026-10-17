package dev.brune.tuteuve.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class FilteringDnsTest {
    @Test fun rejectsLocalAndReservedDestinations() {
        listOf("127.0.0.1", "10.0.0.1", "192.168.1.1", "100.64.0.1", "198.18.0.1", "203.0.113.5", "::1", "fc00::1")
            .forEach { assertFalse(it, FilteringDns.isPublicAddress(InetAddress.getByName(it))) }
    }

    @Test fun allowsPublicLiteralOnlyAfterDnsGate() {
        assertTrue(FilteringDns.isPublicAddress(InetAddress.getByName("8.8.8.8")))
        assertTrue(FilteringDns.isPublicAddress(InetAddress.getByName("2606:4700:4700::1111")))
    }
}
