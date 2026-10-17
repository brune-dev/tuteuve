package dev.brune.tuteuve.data

import java.net.URI

/** First gate for user supplied M3U and Xtream addresses. It performs no DNS or network I/O. */
object SourceAddressPolicy {
    private val hostPattern = Regex("[a-z0-9][a-z0-9.-]*")

    fun accepts(value: String): Boolean {
        if (value.length !in 1..2_048 || value != value.trim()) return false
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.userInfo != null || uri.fragment != null) return false
        if (uri.port !in -1..65_535 || uri.port == 0) return false
        val host = uri.host?.lowercase()?.trimEnd('.') ?: return false
        if (host.isEmpty() || host.contains("..") || host.contains(':')) return false
        if (host == "localhost" || listOf(".localhost", ".local", ".lan", ".home", ".internal")
                .any(host::endsWith)) return false
        // Reject numeric and encoded IP spelling; the network adapter must also check resolved IPs.
        if (host.all { it.isDigit() || it == '.' } || host.startsWith("0x") ||
            host.split('.').any { it.startsWith("0x") }) return false
        return host.matches(hostPattern)
    }
}
