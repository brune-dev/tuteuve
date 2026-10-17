package dev.brune.tuteuve.data

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** Fetches only an explicitly entered HTTPS playlist, without redirects or local-network targets. */
class RemoteM3uImporter {
    private val client = SafeHttpClient.client
    private val parser = M3uParser()

    fun import(url: String, sourceName: String): ImportResult {
        require(SourceAddressPolicy.accepts(url)) { "Dirección no permitida" }
        require(sourceName.isNotBlank() && sourceName.length <= 80) { "Nombre no permitido" }
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Respuesta no válida" }
            val body = response.body ?: error("Respuesta vacía")
            return body.byteStream().use { parser.parse(it, sourceName) }
        }
    }
}

/** One transport policy for playlist downloads and every playback request, including HLS segments. */
object SafeHttpClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .dns(FilteringDns)
        .proxy(Proxy.NO_PROXY)
        .followRedirects(false)
        .followSslRedirects(false)
        .addInterceptor { chain ->
            if (!SourceAddressPolicy.accepts(chain.request().url.toString())) {
                throw IOException("Destino no permitido")
            }
            chain.proceed(chain.request())
        }
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
}

internal object FilteringDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        if (addresses.isEmpty() || addresses.any { !isPublicAddress(it) }) {
            throw UnknownHostException("Destino no permitido")
        }
        return addresses
    }

    fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address
        if (bytes.size == 4) {
            val a = bytes[0].toInt() and 0xff
            val b = bytes[1].toInt() and 0xff
            val c = bytes[2].toInt() and 0xff
            return a in 1..223 && !(a == 100 && b in 64..127) &&
                !(a == 192 && b == 0 && c == 0) &&
                !(a == 198 && b in 18..19) &&
                !(a == 192 && b == 0 && c == 2) &&
                !(a == 198 && b == 51 && c == 100) &&
                !(a == 203 && b == 0 && c == 113)
        }
        if (bytes.size == 16) {
            val first = bytes[0].toInt() and 0xff
            val second = bytes[1].toInt() and 0xff
            return first in 0x20..0x3f &&
                !(first == 0x20 && second == 0x01 &&
                    (bytes[2].toInt() and 0xff) == 0x0d && (bytes[3].toInt() and 0xff) == 0xb8)
        }
        return false
    }
}
