package dev.brune.tuteuve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.brune.tuteuve.data.Channel
import dev.brune.tuteuve.data.UserLibrary

private val subtle = Color(0xFFA9BED1)
private val panel = Color(0xFF142539)

@Composable
internal fun LibraryScreen(
    channels: List<Channel>,
    library: UserLibrary,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onCreateList: (String) -> Boolean,
    onRemoveList: (String) -> Unit,
) {
    val byId = remember(channels) { channels.associateBy { it.id } }
    var newListName by remember { mutableStateOf("") }
    var pendingRemoval by remember { mutableStateOf<String?>(null) }
    fun createList() {
        if (newListName.isNotBlank() && onCreateList(newListName)) newListName = ""
    }
    LazyColumn(Modifier.fillMaxSize().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Favoritos", fontSize = 24.sp, color = Color.White) }
        val favorites = library.favorites.mapNotNull(byId::get).sortedBy { it.name.lowercase() }
        if (favorites.isEmpty()) item { Text("Aún no has marcado canales favoritos.", color = subtle) }
        items(favorites, key = { "favorite-${it.id}" }) { channel ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onPlay(channel) }, modifier = Modifier.weight(1f)) { Text(channel.name) }
                Button(onClick = { onToggleFavorite(channel) }) { Text("Quitar favorito") }
            }
        }
        item { Text("Mis listas", fontSize = 24.sp, color = Color.White, modifier = Modifier.padding(top = 18.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicTextField(
                    value = newListName,
                    onValueChange = { newListName = it.take(60) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { createList() }),
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                    modifier = Modifier.weight(1f).background(panel, RoundedCornerShape(8.dp)).padding(14.dp),
                    decorationBox = { inner ->
                        androidx.compose.foundation.layout.Box {
                            if (newListName.isEmpty()) Text("Nombre de una lista nueva", color = subtle)
                            inner()
                        }
                    },
                )
                Button(onClick = { createList() }, enabled = newListName.isNotBlank()) {
                    Text("Crear lista")
                }
            }
        }
        if (library.lists.isEmpty()) item { Text("Crea una lista para organizar tus canales.", color = subtle) }
        library.lists.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (name, ids) ->
            item(key = "list-$name") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("$name · ${ids.size} canales", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        Button(onClick = { pendingRemoval = name }) { Text("Eliminar lista") }
                    }
                    if (pendingRemoval == name) {
                        Text("¿Eliminar esta lista? Los canales permanecerán en sus fuentes.", color = subtle)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { pendingRemoval = null }) { Text("Cancelar") }
                            Button(onClick = { pendingRemoval = null; onRemoveList(name) }) { Text("Confirmar") }
                        }
                    }
                }
            }
            items(ids.mapNotNull(byId::get).sortedBy { it.name.lowercase() }, key = { "$name-${it.id}" }) { channel ->
                Button(onClick = { onPlay(channel) }, modifier = Modifier.fillMaxWidth()) { Text(channel.name) }
            }
        }
        item { Text("Vistos recientemente", fontSize = 24.sp, color = Color.White, modifier = Modifier.padding(top = 18.dp)) }
        val recent = library.recent.mapNotNull(byId::get)
        if (recent.isEmpty()) item { Text("Todavía no has abierto ningún canal.", color = subtle) }
        items(recent, key = { "recent-${it.id}" }) { channel ->
            Button(onClick = { onPlay(channel) }, modifier = Modifier.fillMaxWidth()) { Text(channel.name) }
        }
        item { Text("Las grabaciones aparecerán aquí cuando la función de grabación real esté disponible.", color = subtle) }
    }
}
