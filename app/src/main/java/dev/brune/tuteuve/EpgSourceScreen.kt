package dev.brune.tuteuve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text

@Composable
internal fun EpgSourceScreen(url: String, loading: Boolean, onUrlChange: (String) -> Unit, onSubmit: () -> Unit) {
    val fieldFocus = remember { FocusRequester() }
    val revealFocus = remember { FocusRequester() }
    val submitFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var reveal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { fieldFocus.requestFocus() }
    Column(Modifier.fillMaxWidth().padding(top = 26.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Guía XMLTV", color = Color.White)
        Text("Introduce una URL HTTPS de una guía que tengas derecho a usar. Se aceptan XML y XML comprimido con gzip.",
            color = Color(0xFFA9BED1))
        SourceField(url, onUrlChange, placeholder = "https://…", maximum = 2_048, concealed = !reveal,
            focusRequester = fieldFocus,
            onDown = { keyboard?.hide(); revealFocus.requestFocus() },
        )
        Button(onClick = { reveal = !reveal }, modifier = Modifier.focusRequester(revealFocus).onPreviewKeyEvent {
            if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                Key.DirectionUp -> { fieldFocus.requestFocus(); true }
                Key.DirectionDown -> { if (!loading && url.isNotBlank()) submitFocus.requestFocus(); true }
                else -> false
            }
        }) { Text(if (reveal) "Ocultar dirección" else "Mostrar dirección") }
        Text("La dirección se usa una vez para importar. No queda guardada; para actualizar deberás introducirla de nuevo.",
            color = Color(0xFFA9BED1))
        Button(onClick = onSubmit, enabled = !loading && url.isNotBlank(),
            modifier = Modifier.focusRequester(submitFocus).onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionUp) {
                    revealFocus.requestFocus(); true
                } else false
            }) {
            Text(if (loading) "Importando guía…" else "Importar guía")
        }
    }
}
