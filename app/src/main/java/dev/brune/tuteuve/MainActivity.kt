package dev.brune.tuteuve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.brune.tuteuve.data.Channel
import dev.brune.tuteuve.data.ChannelRepository
import dev.brune.tuteuve.data.NoCompatibleChannels

private val Ink = Color(0xFF07111F)
private val Panel = Color(0xFF142539)
private val Mint = Color(0xFF8DF0CD)
private val Subtle = Color(0xFFA9BED1)

private enum class Screen(val title: String) {
    Home("Inicio"), Channels("Canales"), Guide("Guía TV"), Library("Mi biblioteca"), Settings("Ajustes"), Legal("Aviso de uso")
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: ChannelRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ChannelRepository(applicationContext)
        setContent {
            var channels by remember { mutableStateOf(emptyList<Channel>()) }
            var screen by remember { mutableStateOf(Screen.Home) }
            var playing by remember { mutableStateOf<Channel?>(null) }
            var drawerVisible by remember { mutableStateOf(false) }
            var notice by remember { mutableStateOf<String?>(null) }
            var importing by remember { mutableStateOf(false) }
            var legalAccepted by remember {
                mutableStateOf(getPreferences(MODE_PRIVATE).getBoolean("source_notice_seen", false))
            }
            LaunchedEffect(Unit) {
                Thread {
                    val stored = runCatching { repository.load() }
                    runOnUiThread {
                        stored.onSuccess { channels = it }.onFailure {
                            notice = "No se pudieron leer las fuentes guardadas. No se han modificado."
                        }
                    }
                }.start()
            }
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    importing = true
                    // A local document is opened only for this import; no URI grant is retained.
                    Thread {
                        val result = runCatching { repository.import(uri) }
                        runOnUiThread {
                            importing = false
                            result.onSuccess {
                                channels = repository.load()
                                screen = Screen.Channels
                                notice = "${it.channels.size} canales importados; ${it.ignoredEntries} entradas omitidas" +
                                    (if (it.httpEntries > 0) " (${it.httpEntries} usan HTTP y requieren revisión)" else "")
                            }.onFailure { error ->
                                // Never display arbitrary exception text: provider errors can contain private URLs.
                                notice = if (error is NoCompatibleChannels && error.httpEntries > 0) {
                                    "No hay emisiones HTTPS. ${error.httpEntries} usan HTTP y requieren revisión de privacidad."
                                } else {
                                    "No se pudo importar la lista. Comprueba el archivo y usa emisiones HTTPS."
                                }
                            }
                        }
                    }.start()
                }
            }
            val importAction = {
                if (legalAccepted) picker.launch(arrayOf("*/*")) else screen = Screen.Legal
            }

            BackHandler(playing != null || drawerVisible || screen != Screen.Home) {
                when {
                    playing != null -> playing = null
                    drawerVisible -> drawerVisible = false
                    screen == Screen.Legal -> screen = Screen.Settings
                    else -> screen = Screen.Home
                }
            }

            if (playing != null) {
                PlayerScreen(playing!!, onBack = { playing = null })
            } else {
                Box(Modifier.fillMaxSize().background(Ink).onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && !drawerVisible) {
                        drawerVisible = true
                        true
                    } else false
                }) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 28.dp)) {
                        Text(screen.title, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        if (notice != null) {
                            Text(notice!!, color = Mint, fontSize = 16.sp)
                            Spacer(Modifier.height(12.dp))
                        }
                        when (screen) {
                            Screen.Home -> HomeScreen(channels.size, importing, importAction) {
                                screen = Screen.Channels
                            }
                            Screen.Channels -> ChannelsScreen(channels, importAction) { playing = it }
                            Screen.Guide -> EmptyScreen("La guía se activará cuando conectemos una fuente EPG real.")
                            Screen.Library -> EmptyScreen("Aún no hay grabaciones. La grabación real está pendiente de validación.")
                            Screen.Settings -> SettingsScreen(channels, importAction, onLegal = { screen = Screen.Legal })
                            Screen.Legal -> LegalScreen(onContinue = {
                                legalAccepted = true
                                getPreferences(MODE_PRIVATE).edit().putBoolean("source_notice_seen", true).apply()
                                screen = Screen.Settings
                                picker.launch(arrayOf("*/*"))
                            })
                        }
                    }
                    if (drawerVisible) {
                        NavigationDrawer(current = screen, onSelect = {
                            screen = it
                            drawerVisible = false
                            notice = null
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(channelCount: Int, importing: Boolean, onImport: () -> Unit, onChannels: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 38.dp)) {
        Text("Tu televisión, organizada a tu manera.", color = Color.White, fontSize = 26.sp)
        Text("$channelCount canales disponibles", color = Subtle, fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onImport, enabled = !importing) { Text(if (importing) "Importando…" else "Añadir lista M3U") }
            if (channelCount > 0) Button(onClick = onChannels) { Text("Ver canales") }
        }
        Text("tuteuve no incluye canales ni listas. Utiliza solo fuentes que tengas derecho a usar.", color = Subtle)
    }
}

@Composable
private fun ChannelsScreen(channels: List<Channel>, onImport: () -> Unit, onPlay: (Channel) -> Unit) {
    var folder by remember { mutableStateOf<String?>(null) }
    val folders = remember(channels) { channels.groupingBy { it.group }.eachCount().toSortedMap() }
    val visible = remember(channels, folder) { if (folder == null) channels else channels.filter { it.group == folder } }
    Row(Modifier.fillMaxSize().padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.width(260.dp).background(Panel).padding(16.dp)) {
            Text("CARPETAS", color = Subtle, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { folder = null }, modifier = Modifier.fillMaxWidth()) { Text("Todos · ${channels.size}") }
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(folders.entries.toList(), key = { it.key }) { item ->
                    Button(onClick = { folder = item.key }, modifier = Modifier.fillMaxWidth()) {
                        Text("${item.key} · ${item.value}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(folder ?: "Todos", color = Color.White, fontSize = 24.sp)
                Text("${visible.size} canales", color = Subtle)
                Button(onClick = onImport) { Text("Añadir fuente") }
            }
            Spacer(Modifier.height(14.dp))
            if (visible.isEmpty()) {
                Text("Importa una lista M3U para explorar tus canales.", color = Subtle)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(visible, key = { it.id }) { channel ->
                        Button(onClick = { onPlay(channel) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${channel.group} · ${channel.sourceName}", color = Subtle, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(channels: List<Channel>, onImport: () -> Unit, onLegal: () -> Unit) {
    val sources = remember(channels) { channels.groupingBy { it.sourceName }.eachCount() }
    Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Fuentes locales", color = Color.White, fontSize = 24.sp)
        if (sources.isEmpty()) Text("No hay fuentes importadas.", color = Subtle)
        sources.forEach { (name, count) -> Text("$name · $count canales", color = Subtle) }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onImport) { Text("Añadir lista M3U") }
            Button(onClick = onLegal) { Text("Aviso de uso") }
        }
        Text("No hay cuenta, analítica, telemetría ni servidor de tuteuve.", color = Subtle)
    }
}

@Composable
private fun LegalScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val fullNotice = remember { context.assets.open("DISCLAIMER.txt").bufferedReader().use { it.readText() } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Antes de añadir una fuente", color = Mint, fontSize = 24.sp)
        Text(fullNotice, color = Color.White, fontSize = 17.sp)
        Button(onClick = onContinue) { Text("Entendido · Elegir mi archivo") }
    }
}

@Composable
private fun EmptyScreen(message: String) {
    Text(message, color = Subtle, fontSize = 20.sp, modifier = Modifier.padding(top = 38.dp))
}

@Composable
private fun NavigationDrawer(current: Screen, onSelect: (Screen) -> Unit) {
    val firstItem = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItem.requestFocus() }
    Column(Modifier.width(290.dp).fillMaxSize().background(Panel).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("tuteuve", fontFamily = FontFamily.Cursive, color = Mint, fontSize = 31.sp)
        Spacer(Modifier.height(16.dp))
        Screen.entries.filter { it != Screen.Legal }.forEachIndexed { index, screen ->
            Button(onClick = { onSelect(screen) }, modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(firstItem) else Modifier)) {
                Text(if (screen == current) "● ${screen.title}" else screen.title)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("DEMO · 0.1.0-alpha", color = Subtle, fontSize = 13.sp)
    }
}

@Composable
private fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(channel.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                controllerShowTimeoutMs = 5_000
                requestFocus()
            }
        }, modifier = Modifier.fillMaxSize())
        Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(onClick = onBack) { Text("← Volver") }
            Text(channel.name, color = Color.White, fontSize = 21.sp)
        }
    }
}
