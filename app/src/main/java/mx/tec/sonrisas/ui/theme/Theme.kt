package mx.tec.sonrisas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun SonrisasTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // sin color dinamico: la paleta sale del logo de la fundacion
    CompositionLocalProvider(LocalSonrisasColors provides if (oscuro) SonrisasOscuro else SonrisasClaro) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = SonrisasTypography,
            shapes = SonrisasShapes,
            content = content,
        )
    }
}

object SonrisasTheme {
    val colors: SonrisasColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSonrisasColors.current
}
