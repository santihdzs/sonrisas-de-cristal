package mx.tec.sonrisas.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import mx.tec.sonrisas.domain.ColorEstrella

@Immutable
data class ParEstrella(val glifo: Color, val suave: Color)

@Immutable
data class SonrisasColors(
    val exito: Color,
    val exitoContenedor: Color,
    val enExitoContenedor: Color,
    val aviso: Color,
    val avisoContenedor: Color,
    val enAvisoContenedor: Color,
    val superficieElevada: Color,
    val estrellaAzul: ParEstrella,
    val estrellaRoja: ParEstrella,
    val estrellaNaranja: ParEstrella,
    val estrellaVerde: ParEstrella,
    val estrellaAmarilla: ParEstrella,
) {
    fun estrella(color: ColorEstrella): ParEstrella = when (color) {
        ColorEstrella.AZUL -> estrellaAzul
        ColorEstrella.ROJO -> estrellaRoja
        ColorEstrella.NARANJA -> estrellaNaranja
        ColorEstrella.VERDE -> estrellaVerde
        ColorEstrella.AMARILLO -> estrellaAmarilla
    }
}

val LocalSonrisasColors = staticCompositionLocalOf { SonrisasClaro }
