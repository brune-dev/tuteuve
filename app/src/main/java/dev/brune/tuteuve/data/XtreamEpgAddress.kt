package dev.brune.tuteuve.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Builds the common Xtream XMLTV endpoint without retaining account details. */
object XtreamEpgAddress {
    fun create(server: String, username: String, password: String): String {
        if (!SourceAddressPolicy.accepts(server) || username.isBlank() || password.isBlank() ||
            username.length > 256 || password.length > 256) {
            throw ImportFailure(ImportFailureReason.ADDRESS)
        }
        val base = server.toHttpUrlOrNull() ?: throw ImportFailure(ImportFailureReason.ADDRESS)
        if (base.query != null || base.fragment != null) throw ImportFailure(ImportFailureReason.ADDRESS)
        val url = base.newBuilder()
            .addPathSegment("xmltv.php")
            .addQueryParameter("username", username)
            .addQueryParameter("password", password)
            .build().toString()
        if (!SourceAddressPolicy.accepts(url)) throw ImportFailure(ImportFailureReason.ADDRESS)
        return url
    }
}
