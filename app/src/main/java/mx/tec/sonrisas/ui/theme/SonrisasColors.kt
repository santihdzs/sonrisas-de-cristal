package mx.tec.sonrisas.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import mx.tec.sonrisas.domain.ColorEstrella

/** un tono en tres intensidades: glifo (iconos y puntos), suave (fondos) y texto (letra sobre el fondo suave) */
@Immutable
data class ParEstrella(val glifo: Color, val suave: Color, val texto: Color)

@Immutable
data class SonrisasColors(
    val exito: Color,
    val exitoContenedor: Color,
    val enExitoContenedor: Color,
    val aviso: Color,
    val avisoContenedor: Color,
    val enAvisoContenedor: Color,
    val superficieElevada: Color,
    /** bloques teal con texto blanco (encabezado y tarjeta teal): primer color del degradado */
    val tealBloque: Color,
    /** segundo color del degradado del encabezado teal */
    val tealOscuro: Color,
    /** teal claro que se lee sobre los bloques teal (por ejemplo "Ya salieron" en la dona de Mi sala) */
    val acentoSobreTeal: Color,
    /** rojo vivo: solo alertas reales y el contador de notificaciones */
    val alertaViva: Color,
    val salio: ParEstrella,
    val sinLlegar: ParEstrella,
    val estrellaAzul: ParEstrella,
    val estrellaRoja: ParEstrella,
    val estrellaNaranja: ParEstrella,
    val estrellaVerde: ParEstrella,
    val estrellaAmarilla: ParEstrella,
    val alimento: ParEstrella,
    val siesta: ParEstrella,
    val panal: ParEstrella,
    val animo: ParEstrella,
    val llegadaSalida: ParEstrella,
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
