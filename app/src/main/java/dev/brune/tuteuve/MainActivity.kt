package dev.brune.tuteuve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import dev.brune.tuteuve.data.ImportFailure
import dev.brune.tuteuve.data.ImportFailureReason
import dev.brune.tuteuve.data.RemoteM3uImporter
import dev.brune.tuteuve.data.UserLibrary
import dev.brune.tuteuve.data.UserLibraryRepository
import dev.brune.tuteuve.data.EpgGuide
import dev.brune.tuteuve.data.EpgIndex
import dev.brune.tuteuve.data.EpgRepository
import dev.brune.tuteuve.data.RemoteXmltvImporter
import dev.brune.tuteuve.data.XtreamEpgAddress
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

private val Ink = Color(0xFF07111F)
private val Panel = Color(0xFF142539)
private val Mint = Color(0xFF8DF0CD)
private val Subtle = Color(0xFFA9BED1)

private enum class Screen(val title: String) {
    Home("Inicio"), Channels("Canales"), Guide("Guía TV"), Library("Mi biblioteca"), Settings("Ajustes"), Source("Añadir fuente"), EpgSource("Añadir guía"), XtreamEpg("Añadir guía"), Legal("Aviso de uso")
}

@OptIn(ExperimentalLayoutApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var repository: ChannelRepository
    private lateinit var libraryRepository: UserLibraryRepository
    private lateinit var epgRepository: EpgRepository
    private val importer = RemoteM3uImporter()
    private val epgImporter = RemoteXmltvImporter()
    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ChannelRepository(applicationContext)
        libraryRepository = UserLibraryRepository(applicationContext)
        epgRepository = EpgRepository(applicationContext)
        setContent {
            var channels by remember { mutableStateOf(emptyList<Channel>()) }
            var library by remember { mutableStateOf(UserLibrary()) }
            var catalog by remember { mutableStateOf(CatalogIndex(emptyList())) }
            var catalogUi by remember { mutableStateOf(CatalogUiState()) }
            var guideUi by remember { mutableStateOf(GuideUiState()) }
            var guide by remember { mutableStateOf<EpgGuide?>(null) }
            var guideIndex by remember { mutableStateOf<EpgIndex?>(null) }
            var epgUrl by remember { mutableStateOf("") }
            var xtreamServer by remember { mutableStateOf("") }
            var xtreamUser by remember { mutableStateOf("") }
            var xtreamPassword by remember { mutableStateOf("") }
            var importingGuide by remember { mutableStateOf(false) }
            var removingGuide by remember { mutableStateOf(false) }
            var screen by remember { mutableStateOf(Screen.Home) }
            var playing by remember { mutableStateOf<Channel?>(null) }
            var preview by remember { mutableStateOf<Channel?>(null) }
            var drawerVisible by remember { mutableStateOf(false) }
            var notice by remember { mutableStateOf<String?>(null) }
            var sourceName by remember { mutableStateOf("") }
            var sourceUrl by remember { mutableStateOf("") }
            var importing by remember { mutableStateOf(false) }
            var removingSource by remember { mutableStateOf<String?>(null) }
            var sourceReturn by remember { mutableStateOf(Screen.Home) }
            var legalReturn by remember { mutableStateOf(Screen.Settings) }
            var legalAccepted by remember {
                mutableStateOf(getPreferences(MODE_PRIVATE).getBoolean("source_notice_seen", false))
            }
            val keyboard = LocalSoftwareKeyboardController.current
            val imeVisible = WindowInsets.isImeVisible
            val updateLibrary: (UserLibrary) -> Unit = { updated ->
                library = updated
                io.execute {
                    runCatching { libraryRepository.save(updated) }.onFailure {
                        runOnUiThread { if (!isDestroyed) notice = "No se pudieron guardar los cambios de biblioteca." }
                    }
                }
            }
            LaunchedEffect(Unit) {
                io.execute {
                    val stored = runCatching {
                        val loaded = repository.load()
                        loaded to CatalogIndex(loaded)
                    }
                    val storedLibrary = runCatching { libraryRepository.load() }
                    val storedGuide = runCatching { epgRepository.load()?.let { it to EpgIndex(it) } }
                    runOnUiThread {
                        if (isDestroyed) return@runOnUiThread
                        stored.onSuccess { (loaded, built) ->
                            channels = loaded
                            catalog = built
                        }.onFailure {
                            notice = "No se pudieron leer las fuentes guardadas. No se han modificado."
                        }
                        storedLibrary.onSuccess { library = it }.onFailure {
                            notice = "No se pudo leer la biblioteca guardada. No se ha modificado."
                        }
                        storedGuide.onSuccess { stored ->
                            guide = stored?.first
                            guideIndex = stored?.second
                        }.onFailure {
                            notice = "No se pudo leer la guía guardada. No se ha modificado."
                        }
                    }
                }
            }
            BackHandler(playing != null || drawerVisible || screen != Screen.Home) {
                when {
                    imeVisible -> keyboard?.hide()
                    playing != null -> playing = null
                    drawerVisible -> drawerVisible = false
                    screen == Screen.Source -> {
                        sourceUrl = ""
                        screen = sourceReturn
                    }
                    screen == Screen.EpgSource -> {
                        epgUrl = ""
                        screen = Screen.Settings
                    }
                    screen == Screen.XtreamEpg -> {
                        xtreamServer = ""
                        xtreamUser = ""
                        xtreamPassword = ""
                        screen = Screen.Settings
                    }
                    screen == Screen.Legal -> screen = legalReturn
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
                                channels = channels,
                                library = library,
                                onChannels = { screen = Screen.Channels },
                                onPlay = { playing = it; updateLibrary(library.recordPlayed(it.id)) },
                                onAddSource = {
                                    sourceName = ""
                                    sourceUrl = ""
                                    sourceReturn = Screen.Home
                                    screen = Screen.Source
                                },
                            )
                            Screen.Channels -> ChannelsScreen(
                                channels = channels,
                                index = catalog,
                                uiState = catalogUi,
                                onUiStateChange = { catalogUi = it },
                                onAddSource = {
                                    sourceName = ""
                                    sourceUrl = ""
                                    sourceReturn = Screen.Channels
                                    screen = Screen.Source
                                },
                                preview = preview,
                                onPreview = { preview = it },
                                onPlay = { playing = it; updateLibrary(library.recordPlayed(it.id)) },
                                library = library,
                                onToggleFavorite = { updateLibrary(library.toggleFavorite(it.id)) },
                                onToggleInList = { name, channel -> updateLibrary(library.toggleInList(name, channel.id)) },
                                onOpenDrawer = { drawerVisible = true },
                                lifecycle = lifecycle,
                            )
                            Screen.Guide -> GuideScreen(
                                channels = channels, catalog = catalog, guide = guide, index = guideIndex,
                                uiState = guideUi, onUiStateChange = { guideUi = it },
                                onPlay = { playing = it; updateLibrary(library.recordPlayed(it.id)) },
                                onOpenDrawer = { drawerVisible = true },
                                onOpenSettings = { screen = Screen.Settings },
                            )
                            Screen.Library -> LibraryScreen(
                                channels = channels,
                                library = library,
                                onPlay = { playing = it; updateLibrary(library.recordPlayed(it.id)) },
                                onToggleFavorite = { updateLibrary(library.toggleFavorite(it.id)) },
                                onCreateList = { name ->
                                    val result = runCatching { library.addList(name) }
                                    result
                                        .onSuccess { updateLibrary(it); notice = null }
                                        .onFailure { notice = "El nombre de la lista está vacío, repetido o supera el límite." }
                                    result.isSuccess
                                },
                                onRemoveList = { updateLibrary(library.removeList(it)) },
                            )
                            Screen.Settings -> SettingsScreen(
                                channels,
                                guide = guide,
                                removingSource = removingSource,
                                removingGuide = removingGuide,
                                onAddSource = {
                                    sourceName = ""
                                    sourceUrl = ""
                                    sourceReturn = Screen.Settings
                                    screen = Screen.Source
                                },
                                onUpdateSource = { name ->
                                    sourceName = name
                                    sourceUrl = ""
                                    sourceReturn = Screen.Settings
                                    screen = Screen.Source
                                },
                                onRemoveSource = { name ->
                                    if (removingSource == null) {
                                        removingSource = name
                                        io.execute {
                                            val outcome = runCatching {
                                                val loaded = repository.removeSource(name)
                                                loaded to CatalogIndex(loaded)
                                            }
                                            runOnUiThread {
                                                if (isDestroyed) return@runOnUiThread
                                                removingSource = null
                                                outcome.onSuccess { (loaded, built) ->
                                                    channels = loaded
                                                    catalog = built
                                                    if (preview?.sourceName == name) preview = null
                                                    if (sourceName == name) sourceName = ""
                                                    catalogUi = catalogUi.copy(folder = emptyList())
                                                    updateLibrary(library.retainChannels(loaded.mapTo(HashSet()) { it.id }))
                                                    notice = "Fuente retirada del dispositivo."
                                                }.onFailure {
                                                    notice = "No se pudo retirar la fuente."
                                                }
                                            }
                                        }
                                    }
                                },
                                onLegal = { legalReturn = Screen.Settings; screen = Screen.Legal },
                                onAddGuide = { epgUrl = ""; screen = Screen.EpgSource },
                                onAddXtreamGuide = {
                                    xtreamServer = ""
                                    xtreamUser = ""
                                    xtreamPassword = ""
                                    screen = Screen.XtreamEpg
                                },
                                onRemoveGuide = {
                                    if (!removingGuide) {
                                        removingGuide = true
                                        io.execute {
                                            val result = runCatching { epgRepository.clear() }
                                            runOnUiThread {
                                                if (isDestroyed) return@runOnUiThread
                                                removingGuide = false
                                                result.onSuccess { guide = null; guideIndex = null; notice = "Guía retirada del dispositivo." }
                                                    .onFailure { notice = "No se pudo retirar la guía." }
                                            }
                                        }
                                    }
                                },
                            )
                            Screen.Source -> SourceScreen(
                                name = sourceName,
                                url = sourceUrl,
                                updating = channels.any { it.sourceName == sourceName.trim() },
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
                                                    updateLibrary(library.retainChannels(loaded.mapTo(HashSet()) { it.id }))
                                                    sourceUrl = ""
                                                    screen = Screen.Channels
                                                    notice = "${report.channels.size} canales añadidos; ${report.ignoredEntries} entradas omitidas" +
                                                        if (report.httpEntries > 0) " (${report.httpEntries} usan HTTP)" else ""
                                                }.onFailure { error ->
                                                    // Never render exception messages: they may contain a private URL.
                                                    notice = when {
                                                        error is NoCompatibleChannels && error.httpEntries > 0 -> "La fuente no contiene emisiones HTTPS compatibles."
                                                        error is NoCompatibleChannels -> "La fuente no contiene canales compatibles."
                                                        error is ImportFailure -> when (error.reason) {
                                                            ImportFailureReason.ADDRESS -> "La dirección debe ser HTTPS y no puede apuntar a una red local."
                                                            ImportFailureReason.NAME -> "Introduce un nombre de fuente válido."
                                                            ImportFailureReason.RESPONSE -> "El servidor no entregó la lista solicitada."
                                                            ImportFailureReason.CONTENT -> "La respuesta no tiene un formato M3U compatible."
                                                            ImportFailureReason.XML_CONFIG -> "El lector XMLTV no está disponible en este dispositivo."
                                                            ImportFailureReason.DNS -> "No se pudo resolver el servidor o el destino está bloqueado."
                                                            ImportFailureReason.TLS -> "No se pudo verificar la conexión segura del servidor."
                                                            ImportFailureReason.NETWORK -> "No se pudo completar la conexión con el servidor."
                                                        }
                                                        else -> "No se pudo guardar la fuente en el dispositivo."
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                            Screen.EpgSource -> EpgSourceScreen(
                                url = epgUrl, loading = importingGuide,
                                onUrlChange = { epgUrl = it },
                                onSubmit = {
                                    if (!legalAccepted) {
                                        legalReturn = Screen.EpgSource
                                        screen = Screen.Legal
                                    } else if (!importingGuide) {
                                        importingGuide = true
                                        notice = null
                                        val url = epgUrl.trim()
                                        io.execute {
                                            val result = runCatching {
                                                epgImporter.import(url).also(epgRepository::save).let { it to EpgIndex(it) }
                                            }
                                            runOnUiThread {
                                                if (isDestroyed) return@runOnUiThread
                                                importingGuide = false
                                                result.onSuccess {
                                                    guide = it.first
                                                    guideIndex = it.second
                                                    epgUrl = ""
                                                    screen = Screen.Guide
                                                    notice = "Guía importada: ${it.first.programmes.size} programas."
                                                }.onFailure { error ->
                                                    // Never display exception text: it may include a private address.
                                                    notice = when (error) {
                                                        is ImportFailure -> when (error.reason) {
                                                            ImportFailureReason.ADDRESS -> "La dirección XMLTV debe ser HTTPS y pública."
                                                            ImportFailureReason.RESPONSE -> "El servidor no entregó la guía solicitada."
                                                            ImportFailureReason.CONTENT -> "La guía XMLTV no es compatible o supera los límites."
                                                            ImportFailureReason.XML_CONFIG -> "El lector XMLTV no está disponible en este dispositivo."
                                                            ImportFailureReason.DNS -> "No se pudo resolver el servidor o el destino está bloqueado."
                                                            ImportFailureReason.TLS -> "No se pudo verificar la conexión segura del servidor."
                                                            ImportFailureReason.NETWORK -> "No se pudo completar la conexión con el servidor."
                                                            ImportFailureReason.NAME -> "La guía no es válida."
                                                        }
                                                        else -> "No se pudo guardar la guía en el dispositivo."
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                            Screen.XtreamEpg -> XtreamEpgSourceScreen(
                                server = xtreamServer,
                                username = xtreamUser,
                                password = xtreamPassword,
                                loading = importingGuide,
                                onServerChange = { xtreamServer = it },
                                onUsernameChange = { xtreamUser = it },
                                onPasswordChange = { xtreamPassword = it },
                                onSubmit = {
                                    if (!legalAccepted) {
                                        legalReturn = Screen.XtreamEpg
                                        screen = Screen.Legal
                                    } else if (!importingGuide) {
                                        importingGuide = true
                                        notice = null
                                        val server = xtreamServer.trim()
                                        val user = xtreamUser
                                        val password = xtreamPassword
                                        io.execute {
                                            val result = runCatching {
                                                val url = XtreamEpgAddress.create(server, user, password)
                                                epgImporter.import(url).also(epgRepository::save).let { it to EpgIndex(it) }
                                            }
                                            runOnUiThread {
                                                if (isDestroyed) return@runOnUiThread
                                                importingGuide = false
                                                result.onSuccess {
                                                    guide = it.first
                                                    guideIndex = it.second
                                                    xtreamServer = ""
                                                    xtreamUser = ""
                                                    xtreamPassword = ""
                                                    screen = Screen.Guide
                                                    notice = "Guía importada: ${it.first.programmes.size} programas."
                                                }.onFailure { error ->
                                                    notice = when (error) {
                                                        is ImportFailure -> when (error.reason) {
                                                            ImportFailureReason.ADDRESS -> "El servidor debe ser HTTPS y público; comprueba los datos de acceso."
                                                            ImportFailureReason.RESPONSE -> "El servidor no entregó la guía Xtream solicitada."
                                                            ImportFailureReason.CONTENT -> "La guía Xtream no es compatible o supera los límites."
                                                            ImportFailureReason.XML_CONFIG -> "El lector XMLTV no está disponible en este dispositivo."
                                                            ImportFailureReason.DNS -> "No se pudo resolver el servidor o el destino está bloqueado."
                                                            ImportFailureReason.TLS -> "No se pudo verificar la conexión segura del servidor."
                                                            ImportFailureReason.NETWORK -> "No se pudo completar la conexión con el servidor."
                                                            ImportFailureReason.NAME -> "La guía no es válida."
                                                        }
                                                        else -> "No se pudo guardar la guía en el dispositivo."
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                            Screen.Legal -> LegalScreen(
                                acknowledgement = legalReturn == Screen.Source || legalReturn == Screen.EpgSource || legalReturn == Screen.XtreamEpg,
                                onBack = {
                                    if (legalReturn == Screen.Source || legalReturn == Screen.EpgSource || legalReturn == Screen.XtreamEpg) {
                                        legalAccepted = true
                                        getPreferences(MODE_PRIVATE).edit().putBoolean("source_notice_seen", true).apply()
                                    }
                                    screen = legalReturn
                                },
                            )
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
private fun HomeScreen(
    channels: List<Channel>,
    library: UserLibrary,
    onChannels: () -> Unit,
    onPlay: (Channel) -> Unit,
    onAddSource: () -> Unit,
) {
    val channelCount = channels.size
    val featured = remember(channels, library.favorites) {
        channels.filter { it.id in library.favorites }.take(10)
    }
    var featuredPosition by remember { mutableStateOf(0) }
    LaunchedEffect(featured.size) {
        if (featured.isEmpty()) featuredPosition = 0
        else while (true) {
            delay(8000)
            featuredPosition = (featuredPosition + 1) % featured.size
        }
    }
    val addFocus = remember { FocusRequester() }
    val channelsFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { addFocus.requestFocus() }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 38.dp).verticalScroll(rememberScrollState())) {
        Text("Tu televisión, organizada a tu manera.", color = Color.White, fontSize = 26.sp)
        Text("$channelCount canales disponibles", color = Subtle, fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onAddSource, modifier = Modifier.focusRequester(addFocus).onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionRight && channelCount > 0) {
                    channelsFocus.requestFocus(); true
                } else false
            }) { Text("Añadir URL M3U") }
            if (channelCount > 0) Button(onClick = onChannels, modifier = Modifier.focusRequester(channelsFocus).onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft) {
                    addFocus.requestFocus(); true
                } else false
            }) { Text("Ver canales") }
        }
        Text("tuteuve no incluye canales ni listas. Utiliza solo fuentes que tengas derecho a usar.", color = Subtle)
        if (featured.isNotEmpty()) {
            val selected = featured[featuredPosition.coerceIn(featured.indices)]
            Text("En tus favoritos", color = Mint, fontSize = 18.sp)
            Text(selected.name, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("${selected.group} · ${selected.sourceName}", color = Subtle)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                featured.forEachIndexed { position, _ ->
                    Text(if (position == featuredPosition) "●" else "○", color = if (position == featuredPosition) Mint else Subtle)
                }
            }
            Button(onClick = { onPlay(selected) }) { Text("Ver canal") }
        }
    }
}

@Composable
private fun SettingsScreen(
    channels: List<Channel>,
    guide: EpgGuide?,
    removingSource: String?,
    removingGuide: Boolean,
    onAddSource: () -> Unit,
    onUpdateSource: (String) -> Unit,
    onRemoveSource: (String) -> Unit,
    onLegal: () -> Unit,
    onAddGuide: () -> Unit,
    onAddXtreamGuide: () -> Unit,
    onRemoveGuide: () -> Unit,
) {
    val sources = remember(channels) {
        channels.groupingBy { it.sourceName }.eachCount().toList().sortedBy { it.first.lowercase() }
    }
    var pendingRemoval by remember { mutableStateOf<String?>(null) }
    var pendingGuideRemoval by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("Fuentes locales", color = Color.White, fontSize = 24.sp) }
        if (sources.isEmpty()) item { Text("No hay fuentes importadas.", color = Subtle) }
        items(sources, key = { it.first }) { (name, count) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$name · $count canales", color = Subtle, modifier = Modifier.weight(1f))
                    Button(onClick = { onUpdateSource(name) }, enabled = removingSource == null) { Text("Actualizar") }
                    Button(onClick = { pendingRemoval = name }, enabled = removingSource == null) { Text("Quitar") }
                }
                if (pendingRemoval == name) {
                    Text("¿Retirar esta fuente y todos sus canales de este dispositivo?", color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { pendingRemoval = null }) { Text("Cancelar") }
                        Button(onClick = { pendingRemoval = null; onRemoveSource(name) }) { Text("Confirmar retirada") }
                    }
                }
            }
        }
        if (removingSource != null) item { Text("Retirando fuente…", color = Subtle) }
        item { Button(onClick = onAddSource) { Text("Añadir URL M3U") } }
        item { Text("Guía de programación", color = Color.White, fontSize = 24.sp) }
        item { Text(if (guide == null) "No hay guía vinculada." else "${guide.programmes.size} programas guardados.", color = Subtle) }
        item { Button(onClick = onAddGuide, enabled = !removingGuide) { Text(if (guide == null) "Vincular XMLTV" else "Actualizar XMLTV") } }
        item { Button(onClick = onAddXtreamGuide, enabled = !removingGuide) { Text(if (guide == null) "Vincular guía Xtream" else "Actualizar guía Xtream") } }
        if (guide != null) {
            item { Button(onClick = { pendingGuideRemoval = true }, enabled = !removingGuide) { Text("Quitar guía") } }
            if (pendingGuideRemoval) item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { pendingGuideRemoval = false }) { Text("Cancelar") }
                    Button(onClick = { pendingGuideRemoval = false; onRemoveGuide() }) { Text("Confirmar retirada de guía") }
                }
            }
        }
        item { Text("La guía Xtream se importa de forma manual. La importación de canales Xtream sigue en preparación.", color = Subtle) }
        item { Button(onClick = onLegal) { Text("Aviso de uso") } }
        item { Text("No hay cuenta, analítica, telemetría ni servidor de tuteuve.", color = Subtle) }
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
private fun NavigationDrawer(current: Screen, onSelect: (Screen) -> Unit) {
    val firstItem = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItem.requestFocus() }
    Column(Modifier.width(290.dp).fillMaxSize().background(Panel).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("tuteuve", fontFamily = FontFamily.Cursive, color = Mint, fontSize = 31.sp)
        Spacer(Modifier.height(16.dp))
        Screen.entries.filter { it != Screen.Legal && it != Screen.Source && it != Screen.EpgSource && it != Screen.XtreamEpg }.forEachIndexed { index, screen ->
            Button(onClick = { onSelect(screen) }, modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(firstItem) else Modifier)) {
                Text(if (screen == current) "● ${screen.title}" else screen.title)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("v0.1.0-alpha", color = Subtle, fontSize = 13.sp)
    }
}
