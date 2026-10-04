package mx.tec.sonrisas.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.TemaFoto
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalTime

@Composable
fun Icono(
    @DrawableRes id: Int,
    descripcion: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(painter = painterResource(id), contentDescription = descripcion, modifier = modifier, tint = tint)
}

@Composable
fun AvatarNino(
    nombre: String,
    apellidos: String,
    estrella: ColorEstrella,
    modifier: Modifier = Modifier,
    tamano: Dp = 48.dp,
) {
    val par = SonrisasTheme.colors.estrella(estrella)
    // el avatar es un grafico de tamano fijo, asi que la letra no escala con la fuente del sistema
    val tamanoLetra = with(LocalDensity.current) { (tamano * 0.38f).toSp() }
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(par.suave),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iniciales(nombre, apellidos),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = tamanoLetra,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(lineHeight = tamanoLetra),
            maxLines = 1,
        )
    }
}

@Composable
fun GlifoEstrella(estrella: ColorEstrella, modifier: Modifier = Modifier, tamano: Dp = 32.dp) {
    val par = SonrisasTheme.colors.estrella(estrella)
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(par.suave),
        contentAlignment = Alignment.Center,
    ) {
        Icono(R.drawable.ic_star_fill, null, Modifier.size(tamano * 0.62f), tint = par.glifo)
    }
}

@Composable
fun EstrellaSala(
    nombre: String,
    estrella: ColorEstrella,
    modifier: Modifier = Modifier,
    tamano: Dp = 32.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
    ) {
        GlifoEstrella(estrella, tamano = tamano)
        Text(nombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

enum class TipoEstado { EXITO, AVISO, ERROR, INFO, NEUTRO }

@Composable
fun EstadoChip(
    texto: String,
    tipo: TipoEstado,
    @DrawableRes icono: Int,
    modifier: Modifier = Modifier,
) {
    val colores = SonrisasTheme.colors
    val esquema = MaterialTheme.colorScheme
    val (fondo, frente) = when (tipo) {
        TipoEstado.EXITO -> colores.exitoContenedor to colores.enExitoContenedor
        TipoEstado.AVISO -> colores.avisoContenedor to colores.enAvisoContenedor
        TipoEstado.ERROR -> esquema.errorContainer to esquema.onErrorContainer
        TipoEstado.INFO -> esquema.primaryContainer to esquema.onPrimaryContainer
        TipoEstado.NEUTRO -> esquema.surfaceContainerHighest to esquema.onSurfaceVariant
    }
    Surface(color = fondo, contentColor = frente, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = Espacio.space2, vertical = Espacio.space1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.space1),
        ) {
            Icono(icono, null, Modifier.size(16.dp))
            Text(texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun TarjetaSeccion(
    modifier: Modifier = Modifier,
    titulo: String? = null,
    @DrawableRes icono: Int? = null,
    onClick: (() -> Unit)? = null,
    accion: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val colores = CardDefaults.outlinedCardColors(containerColor = SonrisasTheme.colors.superficieElevada)
    val borde = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val cuerpo: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Espacio.space4),
            verticalArrangement = Arrangement.spacedBy(Espacio.space2),
        ) {
            if (titulo != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icono != null) {
                        Icono(icono, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(Espacio.space2))
                    }
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    accion?.invoke()
                }
            }
            contenido()
        }
    }
    if (onClick != null) {
        OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colores, border = borde, content = cuerpo)
    } else {
        OutlinedCard(modifier = modifier.fillMaxWidth(), colors = colores, border = borde, content = cuerpo)
    }
}

@Composable
fun EstadoVacio(
    @DrawableRes icono: Int,
    titulo: String,
    modifier: Modifier = Modifier,
    mensaje: String? = null,
    accion: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Espacio.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icono(icono, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (mensaje != null) {
            Text(
                mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        accion?.invoke()
    }
}

@Composable
fun CargandoView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().semantics { contentDescription = "Cargando" },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(mensaje: String, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EstadoVacio(
            icono = R.drawable.ic_error,
            titulo = "Algo salió mal",
            mensaje = mensaje,
            accion = { Button(onClick = onReintentar, modifier = Modifier.heightIn(min = 48.dp)) { Text("Reintentar") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoHora(
    etiqueta: String,
    hora: LocalTime,
    onCambio: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierto by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(hora.formatoHora(), style = MaterialTheme.typography.titleLarge)
        }
        TextButton(onClick = { abierto = true }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Cambiar")
        }
    }
    if (abierto) {
        val estado = rememberTimePickerState(initialHour = hora.hour, initialMinute = hora.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text(etiqueta) },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(onClick = {
                    onCambio(LocalTime.of(estado.hour, estado.minute))
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SelectorOpciones(
    opciones: List<T>,
    seleccionada: T?,
    onSeleccion: (T) -> Unit,
    etiqueta: (T) -> String,
    modifier: Modifier = Modifier,
    icono: ((T) -> Int)? = null,
    grande: Boolean = false,
) {
    if (grande) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
            opciones.forEach { opcion ->
                val activa = opcion == seleccionada
                val esquema = MaterialTheme.colorScheme
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (activa) esquema.primaryContainer else SonrisasTheme.colors.superficieElevada,
                    contentColor = if (activa) esquema.onPrimaryContainer else esquema.onSurface,
                    border = BorderStroke(if (activa) 2.dp else 1.dp, if (activa) esquema.primary else esquema.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 88.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .selectable(selected = activa, role = Role.RadioButton, onClick = { onSeleccion(opcion) }),
                ) {
                    Column(
                        modifier = Modifier.padding(Espacio.space3),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Espacio.space1, Alignment.CenterVertically),
                    ) {
                        if (icono != null) Icono(icono(opcion), null, Modifier.size(36.dp))
                        Text(etiqueta(opcion), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    } else {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
            verticalArrangement = Arrangement.spacedBy(Espacio.space1),
        ) {
            opciones.forEach { opcion ->
                val activa = opcion == seleccionada
                FilterChip(
                    selected = activa,
                    onClick = { onSeleccion(opcion) },
                    label = { Text(etiqueta(opcion), style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = when {
                        activa -> ({ Icono(R.drawable.ic_check, null, Modifier.size(FilterChipDefaults.IconSize)) })
                        icono != null -> ({ Icono(icono(opcion), null, Modifier.size(FilterChipDefaults.IconSize)) })
                        else -> null
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
fun FotoActividad(
    tema: TemaFoto,
    estrella: ColorEstrella,
    modifier: Modifier = Modifier,
    tamanoIcono: Dp = 40.dp,
    descripcion: String? = null,
) {
    val par = SonrisasTheme.colors.estrella(estrella)
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(par.suave)
            .clearAndSetSemantics { contentDescription = descripcion ?: "Foto de ${tema.etiqueta.lowercase()}" },
        contentAlignment = Alignment.Center,
    ) {
        Icono(tema.icono, null, Modifier.size(tamanoIcono), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BannerAlergias(
    alergias: String,
    padecimientos: String,
    medicamentos: String,
    modifier: Modifier = Modifier,
) {
    val lineas = buildList {
        if (alergias.isNotBlank()) add("Alergias" to alergias)
        if (padecimientos.isNotBlank()) add("Padecimientos" to padecimientos)
        if (medicamentos.isNotBlank()) add("Medicamentos" to medicamentos)
    }
    if (lineas.isEmpty()) return
    val colores = SonrisasTheme.colors
    Surface(
        color = colores.avisoContenedor,
        contentColor = colores.enAvisoContenedor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Espacio.space4), horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            Icono(R.drawable.ic_warning_fill, "Atención")
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                lineas.forEach { (titulo, valor) ->
                    Text("$titulo: $valor", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** acceso a una seccion de la sala (objetos perdidos, material); la insignia cuenta lo nuevo */
@Composable
fun BotonAcceso(
    texto: String,
    @DrawableRes icono: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    insignia: Int = 0,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Icono(icono, null)
        Text(texto, modifier = Modifier.padding(start = Espacio.space2))
        if (insignia > 0) {
            Badge(
                modifier = Modifier.padding(start = Espacio.space2).semantics { contentDescription = if (insignia == 1) "1 nuevo" else "$insignia nuevos" },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Text("$insignia") }
        }
    }
}

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(top = Espacio.space2),
    )
}

@Preview(showBackground = true)
@Composable
private fun ComponentesPreview() {
    SonrisasTheme {
        Column(Modifier.padding(Espacio.space4), verticalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalAlignment = Alignment.CenterVertically) {
                AvatarNino("Sofía", "Ramírez Treviño", ColorEstrella.ROJO)
                EstrellaSala("Maternales", ColorEstrella.ROJO)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                EstadoChip("Presente desde 8:05", TipoEstado.EXITO, R.drawable.ic_check_circle)
                EstadoChip("Alergia", TipoEstado.AVISO, R.drawable.ic_warning)
            }
            BannerAlergias("Cacahuate", "", "")
            CampoHora("Hora", LocalTime.of(8, 5), {})
            SelectorOpciones(
                opciones = listOf("Nada", "Poco", "Todo", "Pidió más"),
                seleccionada = "Todo",
                onSeleccion = {},
                etiqueta = { it },
            )
            FotoActividad(TemaFoto.PINTURA, ColorEstrella.AZUL, Modifier.size(96.dp))
        }
    }
}
