package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import mx.tec.sonrisas.ui.theme.Elevacion
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.Radio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

/** sombra difusa y clara, sin borde; el diseno no usa contornos en tarjetas */
fun Modifier.sombraSuave(elevacion: Dp, forma: Shape): Modifier = shadow(
    elevation = elevacion,
    shape = forma,
    clip = false,
    ambientColor = Color.Black.copy(alpha = 0.08f),
    spotColor = Color.Black.copy(alpha = 0.14f),
)

/** degradado sutil de los bloques teal (encabezado de Mi sala y tarjeta teal flotante) */
@Composable
fun degradadoTeal(): Brush {
    val colores = SonrisasTheme.colors
    return Brush.linearGradient(listOf(colores.tealBloque, colores.tealOscuro))
}

/** tarjeta blanca base: sin borde, esquinas de 24 y sombra suave */
@Composable
fun TarjetaBase(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    forma: Shape = RoundedCornerShape(Radio.tarjeta),
    color: Color = SonrisasTheme.colors.superficieElevada,
    contenido: @Composable () -> Unit,
) {
    val conSombra = modifier.sombraSuave(Elevacion.tarjeta, forma)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = conSombra, shape = forma, color = color, content = contenido)
    } else {
        Surface(modifier = conSombra, shape = forma, color = color, content = contenido)
    }
}

/**
 * tarjeta teal flotante del patron B (Inicio, Niños, Avisos de direccion): esquinas de 28 en los cuatro
 * lados; quien la usa le pone el margen de 16. El texto dentro es blanco.
 */
@Composable
fun TarjetaTeal(
    modifier: Modifier = Modifier,
    relleno: PaddingValues = PaddingValues(Espacio.space5),
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(Radio.tarjetaTeal)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sombraSuave(Elevacion.tarjeta * 2, forma)
            .clip(forma)
            .background(degradadoTeal())
            .padding(relleno),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { contenido() }
    }
}

/**
 * hoja blanca fija abajo de una pantalla de detalle (acciones rapidas de la ficha, boton de guardar
 * del registro grupal): esquinas superiores redondeadas y sombra suave hacia arriba.
 */
@Composable
fun BarraAccionesInferior(
    modifier: Modifier = Modifier,
    relleno: PaddingValues = PaddingValues(Espacio.space3),
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(topStart = Radio.tarjeta, topEnd = Radio.tarjeta)
    Surface(
        color = SonrisasTheme.colors.superficieElevada,
        shape = forma,
        modifier = modifier.sombraSuave(Elevacion.barraInferior, forma),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(relleno),
            verticalArrangement = Arrangement.spacedBy(Espacio.space2),
            content = contenido,
        )
    }
}
