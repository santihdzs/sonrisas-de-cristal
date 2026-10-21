package mx.tec.sonrisas.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.theme.Elevacion
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

/**
 * chip de filtro en pildora, sin contorno. Seleccionado: teal con palomita y texto blanco.
 * Sin seleccionar: blanco (o gris si va sobre una tarjeta blanca). El contador opcional va despues del texto ("Presentes 9").
 */
@Composable
fun ChipFiltro(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contador: Int? = null,
    @DrawableRes icono: Int? = null,
    fondoSinSeleccionar: Color = SonrisasTheme.colors.superficieElevada,
) {
    val esquema = MaterialTheme.colorScheme
    // surface con onClick ya reserva la zona tactil minima de 48
    Surface(
        selected = seleccionado,
        onClick = onClick,
        shape = CircleShape,
        color = if (seleccionado) esquema.primary else fondoSinSeleccionar,
        contentColor = if (seleccionado) esquema.onPrimary else esquema.onSurface,
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Espacio.space4, vertical = Espacio.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when {
                seleccionado -> Icono(R.drawable.ic_check, null, Modifier.size(18.dp))
                icono != null -> Icono(icono, null, Modifier.size(18.dp))
            }
            Text(if (contador != null) "$texto $contador" else texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** selector segmentado en pildora (por ejemplo Activos / Baja temporal) */
@Composable
fun <T> SelectorSegmentado(
    opciones: List<T>,
    seleccionada: T,
    onSeleccion: (T) -> Unit,
    etiqueta: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val esquema = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = SonrisasTheme.colors.superficieElevada,
        modifier = modifier.fillMaxWidth().sombraSuave(Elevacion.tarjeta, CircleShape),
    ) {
        Row(Modifier.padding(Espacio.space1).selectableGroup()) {
            opciones.forEach { opcion ->
                val activa = opcion == seleccionada
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(CircleShape)
                        .background(if (activa) esquema.primaryContainer else Color.Transparent)
                        .selectable(selected = activa, role = Role.Tab, onClick = { onSeleccion(opcion) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val color = if (activa) esquema.primary else esquema.onSurfaceVariant
                        if (activa) Icono(R.drawable.ic_check, null, Modifier.size(18.dp), tint = color)
                        Text(etiqueta(opcion), style = MaterialTheme.typography.labelLarge, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

enum class EstadoPildora { PRESENTE, SALIO, SALUD, SIN_LLEGAR }

/** pildora de estado de un niño: presente, salio, salud o sin llegar. Fondo suave y texto oscuro del mismo tono */
@Composable
fun PildoraEstado(texto: String, estado: EstadoPildora, modifier: Modifier = Modifier) {
    val c = SonrisasTheme.colors
    when (estado) {
        EstadoPildora.PRESENTE -> Pildora(texto, c.exitoContenedor, c.enExitoContenedor, modifier, punto = c.exito)
        EstadoPildora.SALIO -> Pildora(texto, c.salio.suave, c.salio.texto, modifier, icono = R.drawable.ic_logout)
        EstadoPildora.SALUD -> Pildora(texto, c.avisoContenedor, c.enAvisoContenedor, modifier, icono = R.drawable.ic_warning_fill, colorIcono = c.aviso)
        EstadoPildora.SIN_LLEGAR -> Pildora(texto, c.sinLlegar.suave, c.sinLlegar.texto, modifier, punto = c.sinLlegar.glifo)
    }
}

/** base de todas las pildoras: totalmente redondeada, con punto o icono opcional, contenido centrado */
@Composable
internal fun Pildora(
    texto: String,
    fondo: Color,
    frente: Color,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    punto: Color? = null,
    colorIcono: Color = frente,
) {
    Surface(color = fondo, contentColor = frente, shape = CircleShape, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = Espacio.space1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            if (punto != null) Box(Modifier.size(8.dp).clip(CircleShape).background(punto))
            if (icono != null) Icono(icono, null, Modifier.size(16.dp), tint = colorIcono)
            Text(texto, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun ChipsPreview() {
    SonrisasTheme {
        Column(Modifier.padding(Espacio.space4), verticalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                ChipFiltro("Todos", true, {}, contador = 14)
                ChipFiltro("Presentes", false, {}, contador = 9)
            }
            SelectorSegmentado(listOf("Activos", "Baja temporal"), "Activos", {}, { it })
            PildoraEstado("Presente desde 8:05", EstadoPildora.PRESENTE, Modifier.fillMaxWidth())
            PildoraEstado("Salió 11:15", EstadoPildora.SALIO, Modifier.fillMaxWidth())
            PildoraEstado("Alergia", EstadoPildora.SALUD)
            PildoraEstado("Sin llegar", EstadoPildora.SIN_LLEGAR, Modifier.fillMaxWidth())
        }
    }
}
