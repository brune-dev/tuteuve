package dev.brune.tuteuve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.brune.tuteuve.data.Channel
import dev.brune.tuteuve.data.CatalogIndex
import dev.brune.tuteuve.data.ChannelRepository
import dev.brune.tuteuve.data.NoCompatibleChannels
import dev.brune.tuteuve.data.RemoteM3uImporter
import java.util.concurrent.Executors

private val Ink = Color(0xFF07111F)
private val Panel = Color(0xFF142539)
private val Mint = Color(0xFF8DF0CD)
private val Subtle = Color(0xFFA9BED1)

private enum class Screen(val title: String) {
    Home("Inicio"), Channels("Canales"), Guide("Guía TV"), Library("Mi biblioteca"), Settings("Ajustes"), Source("Añadir fuente"), Legal("Aviso de uso")
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: ChannelRepository
    private val importer = RemoteM3uImporter()
    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ChannelRepository(applicationContext)
        setContent {
            var channels by remember { mutableStateOf(emptyList<Channel>()) }
            var catalog by remember { mutableStateOf(CatalogIndex(emptyList())) }
            var catalogUi by remember { mutableStateOf(CatalogUiState()) }
            var screen by remember { mutableStateOf(Screen.Home) }
            var playing by remember { mutableStateOf<Channel?>(null) }
            var preview by remember { mutableStateOf<Channel?>(null) }
            var drawerVisible by remember { mutableStateOf(false) }
            var notice by remember { mutableStateOf<String?>(null) }
            var sourceName by remember { mutableStateOf("") }
            var sourceUrl by remember { mutableStateOf("") }
            var importing by remember { mutableStateOf(false) }
            var legalReturn by remember { mutableStateOf(Screen.Settings) }
            var legalAccepted by remember {
                mutableStateOf(getPreferences(MODE_PRIVATE).getBoolean("source_notice_seen", false))
            }
            LaunchedEffect(Unit) {
                io.execute {
                    val stored = runCatching {
                        val loaded = repository.load()
                        loaded to CatalogIndex(loaded)
                    }
                    runOnUiThread {
                        if (isDestroyed) return@runOnUiThread
                        stored.onSuccess { (loaded, built) ->
                            channels = loaded
                            catalog = built
                        }.onFailure {
                            notice = "No se pudieron leer las fuentes guardadas. No se han modificado."
                        }
                    }
                }
            }
            BackHandler(playing != null || drawerVisible || screen != Screen.Home) {
                when {
                    playing != null -> playing = null
                    drawerVisible -> drawerVisible = false
                    screen == Screen.Source -> {
                        sourceUrl = ""
                        screen = Screen.Settings
                    }
                    screen == Screen.Legal -> screen = Screen.Settings
                    else -> screen = Screen.Home
                }
            }

            if (playing != null) {
                PlayerScreen(playing!!, lifecycle, onBack = { playing = null })
            } else {
                Box(Modifier.fillMaxSize().background(Ink).onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && !drawerVisible && screen != Screen.Channels) {
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
                            Screen.Home -> HomeScreen(
                                channelCount = channels.size,
                                onChannels = { screen = Screen.Channels },
                                onAddSource = { screen = Screen.Source },
                            )
                            Screen.Channels -> ChannelsScreen(
                                channels = channels,
                                index = catalog,
                                uiState = catalogUi,
                                onUiStateChange = { catalogUi = it },
                                onAddSource = { screen = Screen.Source },
                                preview = preview,
                                onPreview = { preview = it },
                                onPlay = { playing = it },
                                onOpenDrawer = { drawerVisible = true },
                                lifecycle = lifecycle,
                            )
                            Screen.Guide -> EmptyScreen("La guía se activará cuando conectemos una fuente EPG real.")
                            Screen.Library -> EmptyScreen("Aún no hay grabaciones. La grabación real está pendiente de validación.")
                            Screen.Settings -> SettingsScreen(
                                channels,
                                onAddSource = { screen = Screen.Source },
                                onLegal = { legalReturn = Screen.Settings; screen = Screen.Legal },
                            )
                            Screen.Source -> SourceScreen(
                                name = sourceName,
                                url = sourceUrl,
                                loading = importing,
                                onNameChange = { sourceName = it },
                                onUrlChange = { sourceUrl = it },
                                onSubmit = {
                                    if (!legalAccepted) {
                                        legalReturn = Screen.Source
                                        screen = Screen.Legal
                                    } else if (!importing) {
                                        importing = true
                                        notice = null
                                        val name = sourceName.trim()
                                        val url = sourceUrl.trim()
                                        io.execute {
                                            val outcome = runCatching {
                                                val report = importer.import(url, name)
                                                val loaded = repository.replaceSource(name, report)
                                                Triple(report, loaded, CatalogIndex(loaded))
                                            }
                                            runOnUiThread {
                                                if (isDestroyed) return@runOnUiThread
                                                importing = false
                                                outcome.onSuccess { (report, loaded, built) ->
                                                    channels = loaded
                                                    catalog = built
                                                    sourceUrl = ""
                                                    screen = Screen.Channels
                                                    notice = "${report.channels.size} canales añadidos; ${report.ignoredEntries} entradas omitidas" +
                                                        if (report.httpEntries > 0) " (${report.httpEntries} usan HTTP)" else ""
                                                }.onFailure { error ->
                                                    // Never render exception messages: they may contain a private URL.
                                                    notice = if (error is NoCompatibleChannels && error.httpEntries > 0) {
                                                        "La fuente no contiene emisiones HTTPS compatibles."
                                                    } else "No se pudo añadir la fuente. Comprueba la dirección HTTPS y la conexión."
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                            Screen.Legal -> LegalScreen(
                                acknowledgement = legalReturn == Screen.Source,
                                onBack = {
                                legalAccepted = true
                                getPreferences(MODE_PRIVATE).edit().putBoolean("source_notice_seen", true).apply()
                                screen = legalReturn
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

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }
}

@Composable
private fun HomeScreen(channelCount: Int, onChannels: () -> Unit, onAddSource: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 38.dp)) {
        Text("Tu televisión, organizada a tu manera.", color = Color.White, fontSize = 26.sp)
        Text("$channelCount canales disponibles", color = Subtle, fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onAddSource) { Text("Añadir URL M3U") }
            if (channelCount > 0) Button(onClick = onChannels) { Text("Ver canales") }
        }
        Text("tuteuve no incluye canales ni listas. Utiliza solo fuentes que tengas derecho a usar.", color = Subtle)
    }
}

@Composable
private fun SettingsScreen(channels: List<Channel>, onAddSource: () -> Unit, onLegal: () -> Unit) {
    val sources = remember(channels) { channels.groupingBy { it.sourceName }.eachCount() }
    Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Fuentes locales", color = Color.White, fontSize = 24.sp)
        if (sources.isEmpty()) Text("No hay fuentes importadas.", color = Subtle)
        sources.forEach { (name, count) -> Text("$name · $count canales", color = Subtle) }
        Button(onClick = onAddSource) { Text("Añadir URL M3U") }
        Text("Xtream está en preparación; las credenciales aún no se solicitan.", color = Subtle)
        Button(onClick = onLegal) { Text("Aviso de uso") }
        Text("No hay cuenta, analítica, telemetría ni servidor de tuteuve.", color = Subtle)
    }
}

@Composable
private fun LegalScreen(acknowledgement: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val fullNotice = remember { context.assets.open("DISCLAIMER.txt").bufferedReader().use { it.readText() } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Antes de añadir una fuente", color = Mint, fontSize = 24.sp)
        Text(fullNotice, color = Color.White, fontSize = 17.sp)
        Button(onClick = onBack) { Text(if (acknowledgement) "He leído el aviso" else "Volver a ajustes") }
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
        Screen.entries.filter { it != Screen.Legal && it != Screen.Source }.forEachIndexed { index, screen ->
            Button(onClick = { onSelect(screen) }, modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(firstItem) else Modifier)) {
                Text(if (screen == current) "● ${screen.title}" else screen.title)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("v0.1.0-alpha", color = Subtle, fontSize = 13.sp)
    }
}
