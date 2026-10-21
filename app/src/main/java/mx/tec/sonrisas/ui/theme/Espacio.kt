package mx.tec.sonrisas.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object Espacio {
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 20.dp
    val space6 = 24.dp
    val space7 = 28.dp
    val space8 = 32.dp
    val objetivoMinimo = 48.dp
    /** espacio al final de una lista para que el boton flotante no tape la ultima tarjeta */
    val finalConBoton = 96.dp
}

/** radios del rediseno (DISENO.md, seccion 4) */
object Radio {
    val controlChico = 12.dp
    val r16 = 16.dp
    val r20 = 20.dp
    val tarjeta = 24.dp
    val tarjetaTeal = 28.dp
    val encabezado = 32.dp
}

/** sombras suaves: tarjetas y barra inferior */
object Elevacion {
    val tarjeta = 2.dp
    val barraInferior = 10.dp
}

val SonrisasShapes = Shapes(
    extraSmall = RoundedCornerShape(Radio.controlChico),
    small = RoundedCornerShape(Radio.controlChico),
    medium = RoundedCornerShape(Radio.r16),
    large = RoundedCornerShape(Radio.tarjeta),
    extraLarge = RoundedCornerShape(Radio.tarjetaTeal),
)
