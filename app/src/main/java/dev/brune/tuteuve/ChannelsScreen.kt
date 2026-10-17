package dev.brune.tuteuve

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.brune.tuteuve.data.CatalogIndex
import dev.brune.tuteuve.data.Channel

private val panel = Color(0xFF142539)
private val subtle = Color(0xFFA9BED1)
private val mint = Color(0xFF8DF0CD)

internal data class CatalogUiState(
    val folder: List<String> = emptyList(),
    val query: String = "",
    val grid: Boolean = false,
)

@Composable
internal fun ChannelsScreen(
    channels: List<Channel>,
    index: CatalogIndex,
    uiState: CatalogUiState,
    onUiStateChange: (CatalogUiState) -> Unit,
    importing: Boolean,
    onImport: () -> Unit,
    preview: Channel?,
    onPreview: (Channel) -> Unit,
    onPlay: (Channel) -> Unit,
    onOpenDrawer: () -> Unit,
    lifecycle: Lifecycle,
) {
    val folder = uiState.folder
    val query = uiState.query
    val grid = uiState.grid
    val visible = remember(index, folder, query) { index.channels(folder, query) }
    val childFolders = remember(index, folder) { index.folders(folder) }
    val fullScreenFocus = remember { FocusRequester() }

    BackHandler(folder.isNotEmpty()) { onUiStateChange(uiState.copy(folder = folder.dropLast(1))) }
    LaunchedEffect(preview) {
        if (preview != null) fullScreenFocus.requestFocus()
    }

    Row(
        Modifier.fillMaxSize().padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            Modifier.width(230.dp).fillMaxHeight().background(panel, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text("CARPETAS", color = subtle, fontSize = 13.sp)
            if (folder.isNotEmpty()) {
                Button(onClick = { onUiStateChange(uiState.copy(folder = folder.dropLast(1))) }, modifier = Modifier.fillMaxWidth()) {
                    Text("← Volver")
                }
            }
            Button(
                onClick = { onUiStateChange(uiState.copy(folder = emptyList())) },
                modifier = Modifier.fillMaxWidth().onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft) {
                        onOpenDrawer(); true
                    } else false
                },
            ) { FolderLabel("Todos", channels.size) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(childFolders, key = { it.path.joinToString("\u0000") }) { item ->
                    Button(
                        onClick = { onUiStateChange(uiState.copy(folder = item.path)) },
                        modifier = Modifier.fillMaxWidth().onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft) {
                                if (folder.isEmpty()) onOpenDrawer()
                                else onUiStateChange(uiState.copy(folder = folder.dropLast(1)))
                                true
                            } else false
                        },
                    ) { FolderLabel(item.name, item.channelCount) }
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(folder.lastOrNull() ?: "Todos", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("${visible.size} canales", color = subtle, fontSize = 14.sp)
                }
                Button(onClick = { onUiStateChange(uiState.copy(grid = !grid)) }) { Text(if (grid) "☰ Lista" else "▦ Cuadrícula") }
                Button(onClick = onImport, enabled = !importing) { Text("Añadir fuente") }
            }
            BasicTextField(
                value = query,
                onValueChange = { onUiStateChange(uiState.copy(query = it.take(120))) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 17.sp),
                modifier = Modifier.fillMaxWidth().background(panel, RoundedCornerShape(8.dp)).padding(12.dp),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text("Buscar canal", color = subtle)
                        inner()
                    }
                },
            )
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (visible.isEmpty()) {
                    Text(
                        if (channels.isEmpty()) "Importa una lista M3U para explorar tus canales."
                        else "No hay canales en esta carpeta o búsqueda.",
                        color = subtle,
                        modifier = Modifier.weight(1f),
                    )
                } else if (grid) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(visible.size, key = { "${it}-${visible[it].id}" }) { position ->
                            ChannelButton(visible[position], preview?.id == visible[position].id, onPreview)
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visible.size, key = { "${it}-${visible[it].id}" }) { position ->
                            ChannelButton(visible[position], preview?.id == visible[position].id, onPreview)
                        }
                    }
                }
                Column(Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (preview == null) {
                        Text("Selecciona un canal para previsualizarlo.", color = subtle)
                    } else {
                        PreviewVideo(preview, lifecycle)
                        Text(preview.name, color = Color.White, fontSize = 20.sp)
                        Text(preview.group, color = subtle, fontSize = 14.sp)
                        Button(onClick = { onPlay(preview) }, modifier = Modifier.focusRequester(fullScreenFocus)) {
                            Text("Ver en pantalla completa")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@kotlin.OptIn(ExperimentalFoundationApi::class)
private fun FolderLabel(name: String, count: Int) {
    Column(Modifier.fillMaxWidth()) {
        Text(name, modifier = Modifier.fillMaxWidth().basicMarquee(), maxLines = 1, color = Color.White)
        Text("$count canales", fontSize = 12.sp, color = subtle)
    }
}

@Composable
private fun ChannelButton(channel: Channel, selected: Boolean, onPreview: (Channel) -> Unit) {
    Button(onClick = { onPreview(channel) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text(channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (selected) "Vista previa activa" else channel.group, color = if (selected) mint else subtle, fontSize = 12.sp)
        }
    }
}

@Composable
@OptIn(UnstableApi::class)
private fun PreviewVideo(channel: Channel, lifecycle: Lifecycle) {
    val context = LocalContext.current
    var failed by remember(channel) { mutableStateOf(false) }
    val player = remember(channel) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player, lifecycle) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { failed = true }
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
    Box(Modifier.fillMaxWidth().height(220.dp).background(Color.Black, RoundedCornerShape(10.dp))) {
        AndroidView(factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = false
                isFocusable = false
            }
        }, modifier = Modifier.fillMaxSize())
        if (failed) Text("No se puede reproducir esta señal.", color = Color.White, modifier = Modifier.padding(16.dp))
    }
}
