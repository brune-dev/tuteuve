package dev.brune.tuteuve

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.brune.tuteuve.data.Channel
import dev.brune.tuteuve.data.SafeHttpClient
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val playerInk = Color(0xD907111F)
private val playerMint = Color(0xFF8DF0CD)

private data class PlaybackUi(
    val playing: Boolean = false,
    val live: Boolean = false,
    val seekable: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Int = 0,
)

@Composable
@OptIn(UnstableApi::class)
internal fun PlayerScreen(channel: Channel, lifecycle: Lifecycle, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(channel) {
        val mediaSources = DefaultMediaSourceFactory(OkHttpDataSource.Factory(SafeHttpClient.client))
        ExoPlayer.Builder(context).setMediaSourceFactory(mediaSources).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    var playback by remember(player) { mutableStateOf(player.readUi()) }
    var error by remember(player) { mutableStateOf(false) }
    var controlsVisible by remember(player) { mutableStateOf(true) }
    var interaction by remember(player) { mutableLongStateOf(0L) }
    var position by remember(player) { mutableLongStateOf(0L) }
    val surfaceFocus = remember { FocusRequester() }
    val controlsFocus = remember { FocusRequester() }
    DisposableEffect(player, lifecycle) {
        val listener = object : Player.Listener {
            override fun onEvents(changedPlayer: Player, events: Player.Events) {
                playback = player.readUi()
            }

            override fun onPlayerError(cause: PlaybackException) {
                // Media errors can contain a private URL. Never display exception text.
                error = true
                controlsVisible = true
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        player.addListener(listener)
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }
    BackHandler {
        if (controlsVisible && !error) controlsVisible = false else onBack()
    }
    LaunchedEffect(controlsVisible, interaction, error) {
        if (controlsVisible && !error) {
            delay(5_000)
            controlsVisible = false
        }
    }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) controlsFocus.requestFocus() else surfaceFocus.requestFocus()
    }
    LaunchedEffect(player, controlsVisible) {
        while (controlsVisible) {
            position = player.currentPosition
            delay(500)
        }
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .focusRequester(surfaceFocus)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key == Key.Back || event.key == Key.Escape) false
                else if (!controlsVisible) {
                    controlsVisible = true
                    true
                } else {
                    interaction++
                    false
                }
            }
            .focusable(),
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    isFocusable = false
                }
            },
            update = {
                it.player = player
                it.keepScreenOn = playback.playing
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (controlsVisible) {
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(playerInk).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(channel.name, color = Color.White, fontSize = 25.sp)
                Text(
                    buildString {
                        append(if (playback.live) "EN DIRECTO" else "VÍDEO")
                        if (playback.width > 0 && playback.height > 0) {
                            append(" · ${playback.width}×${playback.height}")
                        }
                        if (playback.fps > 0) append(" · ${playback.fps} FPS")
                    },
                    color = playerMint,
                )
                if (error) {
                    Text("No se puede reproducir esta señal. Comprueba la conexión o el formato.", color = Color.White)
                }
                val duration = player.duration
                if (playback.seekable && duration > 0 && duration < Long.MAX_VALUE) {
                    val fraction = (position.toFloat() / duration).coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth().height(6.dp).background(Color.DarkGray)) {
                        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(playerMint))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onBack, modifier = Modifier.focusRequester(controlsFocus)) { Text("← Volver") }
                    if (error) {
                        Button(onClick = {
                            error = false
                            player.prepare()
                            player.play()
                        }) { Text("Reintentar") }
                    } else {
                        Button(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                            Text(if (playback.playing) "Pausar" else "Reproducir")
                        }
                        if (playback.seekable) {
                            Button(onClick = { player.seekTo((player.currentPosition - 30_000L).coerceAtLeast(0L)) }) {
                                Text("−30 s")
                            }
                        }
                        if (playback.live && playback.seekable) {
                            Button(onClick = { player.seekToDefaultPosition(); player.play() }) { Text("Recuperar directo") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.readUi(): PlaybackUi {
    val frameRate = videoFormat?.frameRate ?: 0f
    return PlaybackUi(
        playing = isPlaying,
        live = isCurrentMediaItemLive,
        seekable = isCurrentMediaItemSeekable,
        width = videoSize.width,
        height = videoSize.height,
        fps = if (frameRate > 0f && frameRate.isFinite()) frameRate.roundToInt() else 0,
    )
}
