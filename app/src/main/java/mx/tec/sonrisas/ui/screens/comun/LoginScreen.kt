package mx.tec.sonrisas.ui.screens.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.ui.components.FilaSwitch
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@Composable
fun LoginScreen(
    estado: LoginUiState,
    onUsuario: (String) -> Unit,
    onContrasena: (String) -> Unit,
    onMostrarContrasena: () -> Unit,
    onEntrar: () -> Unit,
    servidor: String,
    onServidor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var olvido by remember { mutableStateOf(false) }
    var configurar by remember { mutableStateOf(false) }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.space4, vertical = Espacio.space8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(Espacio.space4)) {
                LogoFundacion(Modifier.align(Alignment.CenterHorizontally), tamano = 160.dp)
                Column {
                    Text("Sonrisas de Cristal", style = MaterialTheme.typography.displaySmall)
                    Text("Estancias infantiles", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.size(Espacio.space2))
                OutlinedTextField(
                    value = estado.usuario,
                    onValueChange = onUsuario,
                    label = { Text("Usuario") },
                    singleLine = true,
                    isError = estado.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = estado.contrasena,
                    onValueChange = onContrasena,
                    label = { Text("Contraseña") },
                    singleLine = true,
                    isError = estado.error != null,
                    supportingText = estado.error?.let { { Text(it) } },
                    visualTransformation = if (estado.mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onEntrar() }),
                    trailingIcon = {
                        IconButton(onClick = onMostrarContrasena) {
                            if (estado.mostrarContrasena) Icono(R.drawable.ic_visibility_off, "Ocultar contraseña")
                            else Icono(R.drawable.ic_visibility, "Mostrar contraseña")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onEntrar,
                    enabled = estado.puedeEntrar,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    if (estado.entrando) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(Espacio.space2))
                        Text("Entrando...")
                    } else {
                        Text("Entrar")
                    }
                }
                TextButton(onClick = { olvido = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("¿Olvidaste tu contraseña?")
                }
                TextButton(onClick = { configurar = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (servidor.isBlank()) "Modo: datos de prueba sin servidor" else "Servidor: $servidor")
                }
            }
        }
    }
    if (olvido) {
        AlertDialog(
            onDismissRequest = { olvido = false },
            title = { Text("Recupera tu acceso") },
            text = { Text("Pide a la dirección de tu estancia que restablezca tu acceso. Te darán una contraseña nueva para entrar.") },
            confirmButton = { TextButton(onClick = { olvido = false }) { Text("Entendido") } },
        )
    }
    if (configurar) {
        DialogoServidor(servidor, onCerrar = { configurar = false }, onGuardar = onServidor)
    }
}

/** la direccion del servidor cambia segun la red (la laptop de la demo) o se apaga para usar los datos de prueba */
@Composable
private fun DialogoServidor(actual: String, onCerrar: () -> Unit, onGuardar: (String) -> Unit) {
    var url by remember { mutableStateOf(actual) }
    var sinServidor by remember { mutableStateOf(actual.isBlank()) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Servidor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                Text("Al guardar, la app se reinicia y hay que volver a entrar.")
                FilaSwitch("Datos de prueba sin servidor", sinServidor, { sinServidor = it })
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Dirección del servidor") },
                    placeholder = { Text("http://192.168.1.20:8080") },
                    singleLine = true,
                    enabled = !sinServidor,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limpia = url.trim().trimEnd('/')
                    onGuardar(if (sinServidor) "" else if (limpia.startsWith("http://") || limpia.startsWith("https://")) limpia else "http://$limpia")
                },
                enabled = sinServidor || url.isNotBlank(),
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

fun iconoRol(rol: Rol): Int = when (rol) {
    Rol.EDUCADORA -> R.drawable.ic_groups
    Rol.FAMILIAR -> R.drawable.ic_family_restroom
    Rol.DIRECCION -> R.drawable.ic_badge
    Rol.FUNDACION -> R.drawable.ic_apartment
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    SonrisasTheme {
        LoginScreen(
            estado = LoginUiState(usuario = "educadora", error = "Usuario o contraseña incorrectos. Revísalos e inténtalo de nuevo."),
            onUsuario = {}, onContrasena = {}, onMostrarContrasena = {}, onEntrar = {},
            servidor = "http://192.168.1.20:8080", onServidor = {},
        )
    }
}
