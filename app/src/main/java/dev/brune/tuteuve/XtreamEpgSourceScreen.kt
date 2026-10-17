package dev.brune.tuteuve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text

@Composable
internal fun XtreamEpgSourceScreen(
    server: String,
    username: String,
    password: String,
    loading: Boolean,
    onServerChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val serverFocus = remember { FocusRequester() }
    val userFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val submitFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { serverFocus.requestFocus() }
    Column(Modifier.fillMaxWidth().padding(top = 26.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Guía desde Xtream", color = Color.White)
        Text("Introduce los datos de tu fuente autorizada directamente en el dispositivo. Se usarán una vez para importar la guía y no quedarán guardados.",
            color = Color(0xFFA9BED1))
        Text("Servidor HTTPS", color = Color(0xFFA9BED1))
        SourceField(server, onServerChange, placeholder = "https://servidor.example/", maximum = 1_500,
            concealed = true, focusRequester = serverFocus,
            onDown = { keyboard?.hide(); userFocus.requestFocus() })
        Text("Usuario", color = Color(0xFFA9BED1))
        SourceField(username, onUsernameChange, placeholder = "Usuario", maximum = 256,
            concealed = true, focusRequester = userFocus,
            onUp = { keyboard?.hide(); serverFocus.requestFocus() },
            onDown = { keyboard?.hide(); passwordFocus.requestFocus() })
        Text("Contraseña", color = Color(0xFFA9BED1))
        SourceField(password, onPasswordChange, placeholder = "Contraseña", maximum = 256,
            concealed = true, focusRequester = passwordFocus,
            onUp = { keyboard?.hide(); userFocus.requestFocus() },
            onDown = { keyboard?.hide(); submitFocus.requestFocus() })
        Button(onClick = onSubmit, enabled = !loading && server.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.focusRequester(submitFocus)) {
            Text(if (loading) "Importando guía…" else "Importar guía Xtream")
        }
        Text("Solo se acepta HTTPS. Algunas fuentes Xtream no ofrecen el endpoint XMLTV; en ese caso la app mostrará un error sin exponer tus datos.",
            color = Color(0xFFA9BED1))
    }
}
