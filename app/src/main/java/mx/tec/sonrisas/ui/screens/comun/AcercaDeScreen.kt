package mx.tec.sonrisas.ui.screens.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        LogoFundacion(tamano = 200.dp)
        Text(
            "Fundación Estancias Infantiles Sonrisas de Cristal",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            "Organización sin fines de lucro que opera estancias infantiles para familias de bajos ingresos en Monterrey. " +
                "Esta app reemplaza la bitácora en papel y los mensajes por WhatsApp: las educadoras registran el día de cada niño " +
                "y las familias lo consultan desde su teléfono, con la información protegida y separada por estancia.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "Proyecto universitario TC2007B, Tecnológico de Monterrey.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Versión $version", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun AcercaDePreview() {
    SonrisasTheme { AcercaDeScreen("0.1.0") }
}
