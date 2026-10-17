package dev.brune.tuteuve

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text

private val sourcePanel = Color(0xFF142539)
private val sourceMint = Color(0xFF8DF0CD)
private val sourceSubtle = Color(0xFFA9BED1)

@Composable
internal fun SourceScreen(
    name: String,
    url: String,
    updating: Boolean,
    loading: Boolean,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val nameFocus = remember { FocusRequester() }
    val urlFocus = remember { FocusRequester() }
    val submitFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxWidth().padding(top = 26.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("URL M3U", color = Color.White, fontSize = 23.sp)
        Text("Ponle un nombre y pega la dirección HTTPS de una lista que tengas derecho a usar.", color = sourceSubtle)
        Text("Nombre de la fuente", color = sourceSubtle)
        SourceField(name, onNameChange, placeholder = "Mi fuente", maximum = 80,
            focusRequester = nameFocus,
            onDown = { keyboard?.hide(); urlFocus.requestFocus() },
        )
        Text("Dirección privada", color = sourceSubtle)
        SourceField(url, onUrlChange, placeholder = "https://…", maximum = 2_048, concealed = true,
            focusRequester = urlFocus,
            onUp = { keyboard?.hide(); nameFocus.requestFocus() },
            onDown = { keyboard?.hide(); if (!loading && name.isNotBlank() && url.isNotBlank()) submitFocus.requestFocus() },
        )
        Text("La dirección se oculta al escribir y no se guarda. Actualizar una fuente reemplaza sus canales.", color = sourceSubtle)
        Button(onClick = onSubmit, enabled = !loading && name.isNotBlank() && url.isNotBlank(),
            modifier = Modifier.focusRequester(submitFocus).onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionUp) {
                    urlFocus.requestFocus(); true
                } else false
            },
        ) {
            Text(if (loading) "Conectando…" else if (updating) "Actualizar fuente" else "Añadir fuente")
        }
        Text("Xtream se añadirá con protección específica de credenciales.", color = sourceSubtle)
    }
}

@Composable
internal fun SourceField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maximum: Int,
    concealed: Boolean = false,
    focusRequester: FocusRequester,
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maximum)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (concealed) KeyboardType.Password else KeyboardType.Text,
            imeAction = if (concealed) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { onDown?.invoke() }, onDone = { onDown?.invoke() }),
        visualTransformation = if (concealed) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
        modifier = Modifier.fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false
                else when (event.key) {
                    Key.DirectionUp -> if (onUp != null) { onUp(); true } else false
                    Key.DirectionDown -> if (onDown != null) { onDown(); true } else false
                    else -> false
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 2.dp else 1.dp, if (focused) sourceMint else sourceSubtle, RoundedCornerShape(8.dp))
            .background(sourcePanel, RoundedCornerShape(8.dp))
            .padding(14.dp),
        decorationBox = { inner ->
            androidx.compose.foundation.layout.Box {
                if (value.isEmpty()) Text(placeholder, color = sourceSubtle)
                inner()
            }
        },
    )
}
