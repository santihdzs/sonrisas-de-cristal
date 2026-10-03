package mx.tec.sonrisas.ui.components

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.Credenciales
import mx.tec.sonrisas.ui.theme.Espacio

/** la contrasena temporal solo se ve aqui; al cerrar ya no se puede recuperar */
@Composable
fun DialogoCredenciales(credenciales: List<Credenciales>, onCerrar: () -> Unit) {
    if (credenciales.isEmpty()) return
    val portapapeles = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copiado by remember(credenciales) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = {},
        icon = { Icono(R.drawable.ic_key, null) },
        title = { Text(if (credenciales.size == 1) "Cuenta creada" else "Cuentas creadas") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                Text(
                    "Entrégale estos datos a la persona. La contraseña es temporal y no se volverá a mostrar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                credenciales.forEachIndexed { i, c ->
                    if (i > 0) HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                        Text(c.nombre, style = MaterialTheme.typography.titleMedium)
                        DatoCredencial("Usuario", c.usuario)
                        DatoCredencial("Contraseña", c.contrasena)
                    }
                }
                if (copiado) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                        Icono(R.drawable.ic_check, null)
                        Text("Copiamos los datos.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar, modifier = Modifier.heightIn(min = 48.dp)) { Text("Listo") } },
        dismissButton = {
            TextButton(
                onClick = {
                    val texto = credenciales.joinToString("\n\n") { "${it.nombre}\nUsuario: ${it.usuario}\nContraseña: ${it.contrasena}" }
                    scope.launch { portapapeles.setClipEntry(ClipEntry(ClipData.newPlainText("Cuenta de Sonrisas de Cristal", texto))) }
                    copiado = true
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icono(R.drawable.ic_content_copy, null)
                Text("Copiar", modifier = Modifier.padding(start = Espacio.space2))
            }
        },
    )
}

@Composable
private fun DatoCredencial(etiqueta: String, valor: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
        Text("$etiqueta:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace))
    }
}
