package dev.brune.tuteuve.data

import java.io.BufferedInputStream
import java.io.IOException
import java.net.UnknownHostException
import java.util.zip.GZIPInputStream
import javax.net.ssl.SSLException
import okhttp3.Request

/** Imports one user supplied XMLTV feed; its address is never persisted or shown in an error. */
class RemoteXmltvImporter {
    fun import(url: String): EpgGuide {
        if (!SourceAddressPolicy.accepts(url)) throw ImportFailure(ImportFailureReason.ADDRESS)
        try {
            SafeHttpClient.client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) throw ImportFailure(ImportFailureReason.RESPONSE)
                val body = response.body ?: throw ImportFailure(ImportFailureReason.CONTENT)
                return try {
                    BufferedInputStream(body.byteStream()).use { stream ->
                        stream.mark(2)
                        val gzip = stream.read() == 0x1f && stream.read() == 0x8b
                        stream.reset()
                        XmltvParser().parse(if (gzip) GZIPInputStream(stream) else stream)
                    }
                } catch (error: Exception) {
                    throw ImportFailure(if (error is XmltvConfigurationFailure) ImportFailureReason.XML_CONFIG
                    else ImportFailureReason.CONTENT)
                }
            }
        } catch (error: ImportFailure) {
            throw error
        } catch (_: UnknownHostException) {
            throw ImportFailure(ImportFailureReason.DNS)
        } catch (_: SSLException) {
            throw ImportFailure(ImportFailureReason.TLS)
        } catch (_: IOException) {
            throw ImportFailure(ImportFailureReason.NETWORK)
        }
    }
}
