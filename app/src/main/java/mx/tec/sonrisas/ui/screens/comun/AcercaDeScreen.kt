package mx.tec.sonrisas.ui.screens.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@Composable
fun AcercaDeScreen(version: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.space4),
    ) {
        LogoFundacion(tamano = 180.dp, modifier = Modifier.padding(top = Espacio.space2))
        TarjetaSeccion {
            Text(
                "Fundación Estancias Infantiles Sonrisas de Cristal",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                "Organización sin fines de lucro que opera estancias infantiles para familias de bajos ingresos en Monterrey. " +
                    "Esta app reemplaza la bitácora en papel y los mensajes por WhatsApp: las educadoras registran el día de cada niño " +
                    "y las familias lo consultan desde su teléfono, con la información protegida y separada por estancia.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TarjetaSeccion {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                Icono(R.drawable.ic_info, null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Proyecto universitario TC2007B, Tecnológico de Monterrey.", style = MaterialTheme.typography.bodyMedium)
                    Text("Versión $version", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun AcercaDePreview() {
    SonrisasTheme { AcercaDeScreen("0.1.0") }
}
