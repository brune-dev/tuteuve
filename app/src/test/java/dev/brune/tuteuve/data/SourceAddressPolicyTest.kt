package dev.brune.tuteuve.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceAddressPolicyTest {
    @Test fun acceptsHttpsProviderAddressesWithoutContactingThem() {
        assertTrue(SourceAddressPolicy.accepts("https://provider.example/list.m3u?token=private"))
        assertTrue(SourceAddressPolicy.accepts("https://provider.example:8443/"))
    }

    @Test fun rejectsCleartextLocalAndAmbiguousAddresses() {
        listOf(
            "http://provider.example/list.m3u",
            "https://127.0.0.1/list.m3u",
            "https://2130706433/list.m3u",
            "https://[::1]/list.m3u",
            "https://box.local/list.m3u",
            "https://box.lan/list.m3u",
            "https://user:password@provider.example/list.m3u",
            "https://provider.example/list.m3u#fragment",
            " https://provider.example/list.m3u",
            "file:///private/list.m3u",
        ).forEach { assertFalse(it, SourceAddressPolicy.accepts(it)) }
    }
}
