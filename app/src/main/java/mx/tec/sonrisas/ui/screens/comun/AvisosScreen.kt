package mx.tec.sonrisas.ui.screens.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.ui.components.BotonFlotante
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EtiquetaSeccion
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.Pildora
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.TarjetaTeal
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.components.formatoFechaHora
import mx.tec.sonrisas.ui.components.formatoHora
import mx.tec.sonrisas.ui.components.formatoLargo
import mx.tec.sonrisas.ui.components.icono
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDateTime

@Composable
fun AvisosScreen(
    datos: AvisosDatos,
    modifier: Modifier = Modifier,
    onPublicar: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            // solo la direccion publica avisos: para los demas roles no llega onPublicar
            if (onPublicar != null) BotonFlotante("Publicar aviso", R.drawable.ic_campaign, onPublicar)
        },
    ) { padding ->
        if (datos.avisos.isEmpty()) {
            EstadoVacio(R.drawable.ic_campaign, "Sin avisos por ahora", Modifier.padding(padding), mensaje = "Aquí verás las juntas, suspensiones y campañas de tu estancia.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space2, bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(Espacio.space3),
            ) {
                item { ResumenAvisos(datos) }
                item { EtiquetaSeccion("Últimos avisos") }
                items(datos.avisos, key = { it.id }) { aviso ->
                    TarjetaAviso(aviso, noLeido = aviso.id !in datos.leidos)
                }
            }
        }
    }
}

/** tarjeta teal del patron B con el conteo de avisos y los que faltan por leer */
@Composable
private fun ResumenAvisos(datos: AvisosDatos) {
    val total = datos.avisos.size
    TarjetaTeal {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (total == 1) "1 aviso publicado" else "$total avisos publicados",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    when (datos.noLeidos) {
                        0 -> "Todos leídos"
                        1 -> "1 sin leer"
                        else -> "${datos.noLeidos} sin leer"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icono(R.drawable.ic_campaign, null)
            }
        }
    }
}

/** etiqueta de categoria con su color suave: junta teal, suspension terracota, campaña azul, general gris */
@Composable
private fun EtiquetaTipo(tipo: TipoAviso) {
    val esquema = MaterialTheme.colorScheme
    val colores = SonrisasTheme.colors
    val (fondo, frente) = when (tipo) {
        TipoAviso.JUNTA -> esquema.primaryContainer to esquema.primary
        TipoAviso.SUSPENSION -> esquema.tertiaryContainer to esquema.onTertiaryContainer
        TipoAviso.CAMPANA -> colores.estrellaAzul.suave to colores.estrellaAzul.texto
        TipoAviso.GENERAL -> colores.sinLlegar.suave to colores.sinLlegar.texto
    }
    Pildora(tipo.etiqueta, fondo, frente, icono = tipo.icono)
}

@Composable
fun TarjetaAviso(aviso: Aviso, noLeido: Boolean, modifier: Modifier = Modifier) {
    TarjetaSeccion(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
            EtiquetaTipo(aviso.tipo)
            Box(Modifier.weight(1f))
            if (noLeido) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .semantics { contentDescription = "Sin leer" },
                    )
                    Text("Nuevo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Text(aviso.titulo, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        aviso.fechaEvento?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                Icono(R.drawable.ic_event, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text("${it.toLocalDate().formatoLargo()}, ${it.formatoHora()}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(aviso.cuerpo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(top = Espacio.space1), color = MaterialTheme.colorScheme.outlineVariant)
        Text("Publicado el ${aviso.publicadoEl.formatoFechaHora()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun AvisosPreview() {
    val a = Aviso("1", "e", TipoAviso.JUNTA, "Junta de padres de familia", "Te esperamos en el salón de usos múltiples.", LocalDateTime.of(2026, 10, 9, 17, 30), LocalDateTime.of(2026, 9, 29, 9, 10), "d")
    val b = Aviso("2", "e", TipoAviso.GENERAL, "Colecta de ropa de invierno", "Recibimos chamarras y cobijas.", null, LocalDateTime.of(2026, 9, 18, 9, 10), "d")
    SonrisasTheme { AvisosScreen(AvisosDatos(listOf(a, b), setOf("2")), onPublicar = {}) }
}
