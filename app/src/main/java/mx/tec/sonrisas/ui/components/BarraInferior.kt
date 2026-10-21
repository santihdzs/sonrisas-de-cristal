package mx.tec.sonrisas.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.theme.Elevacion
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.Radio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

/** barra inferior flotante: pildora blanca separada de los bordes, respetando el area de gestos */
@Composable
fun BarraInferiorFlotante(modifier: Modifier = Modifier, items: @Composable RowScope.() -> Unit) {
    val forma = RoundedCornerShape(Radio.tarjetaTeal)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = Espacio.space4, end = Espacio.space4, top = Espacio.space2, bottom = Espacio.space3),
    ) {
        Surface(
            shape = forma,
            color = SonrisasTheme.colors.superficieElevada,
            modifier = Modifier.fillMaxWidth().sombraSuave(Elevacion.barraInferior, forma),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Espacio.space1, vertical = Espacio.space2).selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                content = items,
            )
        }
    }
}

/**
 * un elemento de la barra. Seleccionado: pildora teal suave detras del icono, icono y etiqueta teal.
 * Sin seleccionar: gris medio. La insignia es un circulo rojo pequeño con el numero de no leidos.
 */
@Composable
fun RowScope.ItemBarraInferior(
    etiqueta: String,
    @DrawableRes icono: Int,
    @DrawableRes iconoSeleccionado: Int,
    seleccionado: Boolean,
    onClick: () -> Unit,
    insignia: Int = 0,
) {
    val esquema = MaterialTheme.colorScheme
    val color = if (seleccionado) esquema.primary else esquema.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(Radio.r16))
            .selectable(selected = seleccionado, role = Role.Tab, onClick = onClick)
            .padding(vertical = Espacio.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.space1, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .width(56.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(if (seleccionado) esquema.primaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            BadgedBox(badge = {
                if (insignia > 0) {
                    Badge(
                        containerColor = SonrisasTheme.colors.alertaViva,
                        contentColor = Color.White,
                        modifier = Modifier.semantics { contentDescription = "$insignia sin leer" },
                    ) { Text("$insignia") }
                }
            }) {
                Icono(if (seleccionado) iconoSeleccionado else icono, null, tint = color)
            }
        }
        // con cinco pestanas y letra al 200 % las etiquetas se cortan; se limitan a 130 %
        val densidad = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(densidad.density, minOf(densidad.fontScale, 1.3f))) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun BarraInferiorPreview() {
    SonrisasTheme {
        BarraInferiorFlotante {
            ItemBarraInferior("Hoy", R.drawable.ic_today, R.drawable.ic_today_fill, true, {})
            ItemBarraInferior("Semana", R.drawable.ic_date_range, R.drawable.ic_date_range_fill, false, {})
            ItemBarraInferior("Fotos", R.drawable.ic_photo_library, R.drawable.ic_photo_library_fill, false, {})
            ItemBarraInferior("Mensajes", R.drawable.ic_chat, R.drawable.ic_chat_fill, false, {}, insignia = 3)
            ItemBarraInferior("Avisos", R.drawable.ic_campaign, R.drawable.ic_campaign_fill, false, {}, insignia = 2)
        }
    }
}
