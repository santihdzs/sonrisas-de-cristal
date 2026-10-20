package mx.tec.sonrisas.ui.screens.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.ui.components.GlifoEstrella
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.TarjetaBase
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@Composable
fun BloqueoScreen(
    titulo: String,
    estrella: ColorEstrella?,
    nombreUsuario: String,
    mensaje: String?,
    onDesbloquear: () -> Unit,
    onCambiarUsuario: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // solo aspecto: los mismos textos y las mismas dos acciones (desbloquear y cambiar de usuario)
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(Espacio.space4),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TarjetaBase(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(Espacio.space6),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Espacio.space4),
                    ) {
                        if (estrella != null) {
                            GlifoEstrella(estrella, tamano = 64.dp)
                        } else {
                            Box(
                                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icono(R.drawable.ic_lock, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        Text(
                            "La app está bloqueada. Solo $nombreUsuario puede desbloquearla.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        if (mensaje != null) {
                            Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        }
                        Button(onClick = onDesbloquear, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Icono(R.drawable.ic_fingerprint, null)
                            Text("Desbloquear", modifier = Modifier.padding(start = Espacio.space2))
                        }
                    }
                }
                TextButton(onClick = onCambiarUsuario, modifier = Modifier.padding(top = Espacio.space3).heightIn(min = 48.dp)) {
                    Text("Cambiar de usuario")
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun BloqueoPreview() {
    SonrisasTheme {
        BloqueoScreen("Sala Maternales", ColorEstrella.ROJO, "Mariana López Hernández", null, {}, {})
    }
}
