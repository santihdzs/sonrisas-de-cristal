package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

/** una parte de la dona y su renglon en la leyenda */
data class SegmentoDona(val etiqueta: String, val valor: Int, val color: Color)

/**
 * dona con leyenda para un bloque teal (encabezado de Mi sala). El centro muestra el primer
 * segmento ("9 presentes"). Los segmentos se dibujan en orden empezando arriba, en el sentido del reloj.
 */
@Composable
fun DonaConLeyenda(
    segmentos: List<SegmentoDona>,
    textoCentro: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 104.dp,
) {
    val principal = segmentos.firstOrNull()?.valor ?: 0
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.space6),
    ) {
        Dona(
            segmentos,
            Modifier
                .size(tamano)
                .clearAndSetSemantics { contentDescription = "$principal $textoCentro" },
        ) {
            // la dona es un grafico de tamano fijo, asi que su texto no escala con la fuente del sistema
            val densidad = LocalDensity.current
            val grande = with(densidad) { (tamano * 0.24f).toSp() }
            val chico = with(densidad) { (tamano * 0.11f).toSp() }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$principal", fontSize = grande, lineHeight = grande, fontWeight = FontWeight.Bold)
                Text(textoCentro, fontSize = chico, lineHeight = chico, color = LocalContentColor.current.copy(alpha = 0.85f), textAlign = TextAlign.Center)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
            segmentos.forEach { s ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(s.color))
                    Text(s.etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = Espacio.space2))
                    Text("${s.valor}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun Dona(
    segmentos: List<SegmentoDona>,
    modifier: Modifier = Modifier,
    grosor: Dp = 12.dp,
    pista: Color = Color.White.copy(alpha = 0.18f),
    centro: @Composable () -> Unit,
) {
    val total = segmentos.sumOf { it.valor }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val trazo = grosor.toPx()
            val lado = size.minDimension - trazo
            val origen = Offset((size.width - lado) / 2, (size.height - lado) / 2)
            val area = Size(lado, lado)
            drawArc(pista, 0f, 360f, useCenter = false, topLeft = origen, size = area, style = Stroke(trazo))
            if (total > 0) {
                var inicio = -90f
                segmentos.forEach { s ->
                    val barrido = 360f * s.valor / total
                    if (barrido > 0f) {
                        drawArc(s.color, inicio, barrido, useCenter = false, topLeft = origen, size = area, style = Stroke(trazo, cap = StrokeCap.Butt))
                    }
                    inicio += barrido
                }
            }
        }
        centro()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2E7D74)
@Composable
private fun DonaPreview() {
    SonrisasTheme {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            DonaConLeyenda(
                listOf(
                    SegmentoDona("Presentes", 9, Color.White),
                    SegmentoDona("Sin llegar", 3, Color.White.copy(alpha = 0.4f)),
                    SegmentoDona("Ya salieron", 2, SonrisasTheme.colors.acentoSobreTeal),
                ),
                textoCentro = "presentes",
                modifier = Modifier.padding(Espacio.space4),
            )
        }
    }
}
