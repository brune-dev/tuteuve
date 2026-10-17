package dev.brune.tuteuve.data

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/** Apply the same HTTPS, DNS and redirect policy to preview, full screen and HLS child requests. */
@UnstableApi
fun secureMediaSources(): DefaultMediaSourceFactory =
    DefaultMediaSourceFactory(OkHttpDataSource.Factory(SafeHttpClient.client))
