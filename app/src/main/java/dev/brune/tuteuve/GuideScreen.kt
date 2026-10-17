package dev.brune.tuteuve

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.brune.tuteuve.data.CatalogIndex
import dev.brune.tuteuve.data.Channel
import dev.brune.tuteuve.data.EpgGuide
import dev.brune.tuteuve.data.EpgIndex
import dev.brune.tuteuve.data.EpgProgram
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val guidePanel = Color(0xFF142539)
private val guideSubtle = Color(0xFFA9BED1)
private val guideMint = Color(0xFF8DF0CD)

internal data class GuideUiState(val folder: List<String> = emptyList(), val dayOffset: Int = 0)

@Composable
internal fun GuideScreen(
    channels: List<Channel>,
    catalog: CatalogIndex,
    guide: EpgGuide?,
    uiState: GuideUiState,
    onUiStateChange: (GuideUiState) -> Unit,
    onPlay: (Channel) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val index = remember(guide) { guide?.let(::EpgIndex) }
    val folderChannels = remember(catalog, uiState.folder) { catalog.channels(uiState.folder) }
    val folders = remember(catalog, uiState.folder) { catalog.folders(uiState.folder) }
    val dayStart = remember(uiState.dayOffset) {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, uiState.dayOffset)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val dayEnd = remember(dayStart) {
        Calendar.getInstance().apply { timeInMillis = dayStart; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
    }
    val rootFocus = remember { FocusRequester() }
    var selected by remember { mutableStateOf<Pair<Channel, EpgProgram>?>(null) }
    LaunchedEffect(Unit) { rootFocus.requestFocus() }
    BackHandler(selected != null || uiState.folder.isNotEmpty()) {
        if (selected != null) selected = null
        else onUiStateChange(uiState.copy(folder = uiState.folder.dropLast(1)))
    }

    Column(Modifier.fillMaxSize().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { selected = null; onUiStateChange(uiState.copy(dayOffset = 0)) }) { Text("Hoy") }
            Button(onClick = { selected = null; onUiStateChange(uiState.copy(dayOffset = 1)) }) { Text("Mañana") }
            Text(if (guide == null) "Sin guía vinculada" else "${guide.programmes.size} programas en la guía",
                color = guideSubtle, modifier = Modifier.padding(top = 12.dp))
        }
        if (selected != null) {
            val (channel, program) = selected!!
            val now = System.currentTimeMillis()
            Column(Modifier.fillMaxWidth().background(guidePanel, RoundedCornerShape(10.dp)).padding(14.dp)) {
                Text(program.title, color = Color.White, fontSize = 21.sp)
                Text("${channel.name} · ${channel.group} · ${hour(program.startMillis)}–${hour(program.endMillis)}",
                    color = guideSubtle)
                if (program.description.isNotBlank()) Text(program.description, color = guideSubtle, maxLines = 2)
                Text(when {
                    now < program.startMillis -> "Próximamente · Los avisos aún no están disponibles"
                    now < program.endMillis -> "En directo · La grabación aún no está disponible"
                    else -> "Finalizado"
                }, color = guideMint)
                if (now in program.startMillis until program.endMillis) {
                    Button(onClick = { onPlay(channel) }) { Text("Ver programa") }
                }
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("CARPETAS", color = guideSubtle)
                if (uiState.folder.isNotEmpty()) {
                    Button(onClick = { onUiStateChange(uiState.copy(folder = uiState.folder.dropLast(1))) },
                        modifier = Modifier.fillMaxWidth()) { Text("← Volver") }
                }
                Button(onClick = { onUiStateChange(uiState.copy(folder = emptyList())) },
                    modifier = Modifier.fillMaxWidth().focusRequester(rootFocus).onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && uiState.folder.isEmpty()) {
                            onOpenDrawer(); true
                        } else false
                    }) { Text("Todos · ${channels.size}") }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders, key = { it.path.joinToString("\u0000") }) { folder ->
                        Button(onClick = { onUiStateChange(uiState.copy(folder = folder.path)) }, modifier = Modifier.fillMaxWidth()) {
                            GuideFolderLabel(folder.name, folder.channelCount)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text("${if (uiState.dayOffset == 0) "Hoy" else "Mañana"} · ${uiState.folder.lastOrNull() ?: "Todos"}",
                    color = Color.White, fontSize = 24.sp)
                if (guide == null) {
                    Text("Vincula una URL XMLTV HTTPS desde Ajustes para consultar datos reales.", color = guideSubtle)
                    Button(onClick = onOpenSettings) { Text("Abrir ajustes") }
                } else if (folderChannels.isEmpty()) {
                    Text("No hay canales en esta carpeta.", color = guideSubtle)
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(folderChannels, key = { it.id }) { channel ->
                            val programmes = remember(index, channel, dayStart) {
                                index?.programmesFor(channel).orEmpty()
                                    .filter { it.startMillis < dayEnd && it.endMillis > dayStart }
                            }
                            Column(Modifier.fillMaxWidth().background(guidePanel, RoundedCornerShape(8.dp)).padding(12.dp)) {
                                Text(channel.name, color = Color.White, fontSize = 17.sp)
                                Text(channel.group, color = guideSubtle, fontSize = 12.sp)
                                if (programmes.isEmpty()) {
                                    Text("Sin datos de programación", color = guideSubtle)
                                } else {
                                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        var cursor = maxOf(programmes.first().startMillis, dayStart)
                                        programmes.forEach { program ->
                                            val start = maxOf(program.startMillis, dayStart)
                                            val end = minOf(program.endMillis, dayEnd)
                                            if (start > cursor) {
                                                Spacer(Modifier.width(((start - cursor) / 30_000).coerceAtMost(2880).toInt().dp))
                                            }
                                            if (end > maxOf(start, cursor)) {
                                                val minutes = ((end - maxOf(start, cursor)) / 60_000).coerceAtLeast(1)
                                                Button(onClick = { selected = channel to program },
                                                    modifier = Modifier.width((minutes * 2).coerceAtMost(2880).toInt().dp).height(88.dp)) {
                                                    Column {
                                                        Text("${hour(program.startMillis)}–${hour(program.endMillis)}", fontSize = 11.sp)
                                                        Text(program.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }
                                            cursor = maxOf(cursor, end)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@kotlin.OptIn(ExperimentalFoundationApi::class)
private fun GuideFolderLabel(name: String, count: Int) {
    Column {
        Text(name, modifier = Modifier.fillMaxWidth().basicMarquee(), maxLines = 1)
        Text("$count canales", color = guideSubtle, fontSize = 12.sp)
    }
}

private fun hour(time: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
