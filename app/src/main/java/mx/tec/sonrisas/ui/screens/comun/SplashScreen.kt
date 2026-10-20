package mx.tec.sonrisas.ui.screens.comun

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.components.sombraSuave
import mx.tec.sonrisas.ui.theme.Elevacion
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import mx.tec.sonrisas.ui.theme.fondoLogo

/** el logo va tal cual, sobre una caja clara aunque el tema sea oscuro, con sombra suave como las tarjetas */
@Composable
fun LogoFundacion(modifier: Modifier = Modifier, tamano: Dp = 220.dp) {
    Box(
        modifier = modifier
            .sombraSuave(Elevacion.tarjeta, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .background(SonrisasTheme.fondoLogo)
            .padding(Espacio.space4),
    ) {
        Image(
            painter = painterResource(R.drawable.sonrisas_logo),
            contentDescription = "Logotipo de la Fundación Estancias Infantiles Sonrisas de Cristal",
            modifier = Modifier.size(tamano),
        )
    }
}

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        LogoFundacion()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun SplashPreview() {
    SonrisasTheme { SplashScreen() }
}
