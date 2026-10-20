package mx.tec.sonrisas.ui.components

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.TemaFoto
import mx.tec.sonrisas.ui.theme.EstiloEtiquetaSeccion
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

enum class FormaAvatar { CIRCULO, CUADRADO }

/** avatar de iniciales con el tono suave de la sala; en las tarjetas de Mi sala va cuadrado redondeado */
@Composable
fun AvatarNino(
    nombre: String,
    apellidos: String,
    estrella: ColorEstrella,
    modifier: Modifier = Modifier,
    tamano: Dp = 48.dp,
    forma: FormaAvatar = FormaAvatar.CIRCULO,
) {
    val par = SonrisasTheme.colors.estrella(estrella)
    // el avatar es un grafico de tamano fijo, asi que la letra no escala con la fuente del sistema
    val tamanoLetra = with(LocalDensity.current) { (tamano * 0.38f).toSp() }
    Box(
        modifier = modifier
            .size(tamano)
            .clip(if (forma == FormaAvatar.CIRCULO) CircleShape else RoundedCornerShape(percent = 28))
            .background(par.suave),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iniciales(nombre, apellidos),
            color = par.texto,
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
    Pildora(texto, fondo, frente, modifier, icono = icono)
}

/** tarjeta blanca con titulo opcional; usa TarjetaBase (sin borde, sombra suave) */
@Composable
fun TarjetaSeccion(
    modifier: Modifier = Modifier,
    titulo: String? = null,
    @DrawableRes icono: Int? = null,
    onClick: (() -> Unit)? = null,
    accion: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    TarjetaBase(modifier = modifier.fillMaxWidth(), onClick = onClick) {
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
                // sin contorno: la opcion elegida se distingue por el fondo teal suave y el texto teal
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (activa) esquema.primaryContainer else esquema.surfaceContainer,
                    contentColor = if (activa) esquema.primary else esquema.onSurface,
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
                // los selectores viven en tarjetas y hojas blancas, asi que el chip sin elegir va gris
                ChipFiltro(
                    texto = etiqueta(opcion),
                    seleccionado = activa,
                    onClick = { onSeleccion(opcion) },
                    icono = icono?.invoke(opcion),
                    fondoSinSeleccionar = MaterialTheme.colorScheme.surfaceContainer,
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
    BannerAviso(modifier = modifier, descripcionIcono = "Atención") {
        lineas.forEach { (titulo, valor) ->
            Text("$titulo: $valor", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** banner de aviso amarillo suave con icono de advertencia, sin borde (por ejemplo "1 niño con alerta de salud") */
@Composable
fun BannerAviso(
    texto: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    BannerAviso(modifier = modifier, onClick = onClick) {
        Text(texto, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun BannerAviso(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    descripcionIcono: String? = null,
    @DrawableRes icono: Int = R.drawable.ic_warning_fill,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val colores = SonrisasTheme.colors
    val cuerpo: @Composable () -> Unit = {
        Row(
            Modifier.padding(Espacio.space4),
            horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icono(icono, descripcionIcono, tint = colores.aviso)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.space1), content = contenido)
        }
    }
    val forma = MaterialTheme.shapes.medium
    if (onClick != null) {
        Surface(onClick = onClick, color = colores.avisoContenedor, contentColor = colores.enAvisoContenedor, shape = forma, modifier = modifier.fillMaxWidth(), content = cuerpo)
    } else {
        Surface(color = colores.avisoContenedor, contentColor = colores.enAvisoContenedor, shape = forma, modifier = modifier.fillMaxWidth(), content = cuerpo)
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

/** etiqueta de seccion: MAYUSCULAS pequeñas, semibold y gris ("NIÑOS DE LA SALA") */
@Composable
fun EtiquetaSeccion(texto: String, modifier: Modifier = Modifier, accion: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = Espacio.space2), verticalAlignment = Alignment.CenterVertically) {
        Text(
            texto.uppercase(LocaleMx),
            style = EstiloEtiquetaSeccion,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // el lector de pantalla lee el texto normal, no deletrea las mayusculas
            modifier = Modifier.weight(1f).semantics { heading(); contentDescription = texto },
        )
        accion?.invoke()
    }
}

/** boton flotante extendido en terracota, el unico uso fuerte del acento */
@Composable
fun BotonFlotante(
    texto: String,
    @DrawableRes icono: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icono(icono, null) },
        text = { Text(texto, style = MaterialTheme.typography.titleMedium) },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
        modifier = modifier,
    )
}

/**
 * dialogo de confirmacion. Si la accion es destructiva (borrar), el boton de confirmar va en rojo apagado.
 */
@Composable
fun DialogoConfirmar(
    titulo: String,
    confirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    mensaje: String? = null,
    destructivo: Boolean = true,
    cancelar: String = "Cancelar",
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = mensaje?.let { { Text(it) } },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                colors = if (destructivo) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.textButtonColors(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(confirmar) }
        },
        dismissButton = { TextButton(onClick = onCancelar, modifier = Modifier.heightIn(min = 48.dp)) { Text(cancelar) } },
        containerColor = SonrisasTheme.colors.superficieElevada,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun ComponentesPreview() {
    SonrisasTheme {
        Column(Modifier.padding(Espacio.space4), verticalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalAlignment = Alignment.CenterVertically) {
                AvatarNino("Sofía", "Ramírez Treviño", ColorEstrella.ROJO)
                AvatarNino("Andrea", "Cantú Elizondo", ColorEstrella.NARANJA, tamano = 56.dp, forma = FormaAvatar.CUADRADO)
                EstrellaSala("Maternales", ColorEstrella.ROJO)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                EstadoChip("Presente desde 8:05", TipoEstado.EXITO, R.drawable.ic_check_circle)
                EstadoChip("Alergia", TipoEstado.AVISO, R.drawable.ic_warning)
            }
            BannerAlergias("Cacahuate", "", "")
            BannerAviso("1 niño con alerta de salud")
            EtiquetaSeccion("Niños de la sala")
            TarjetaSeccion(titulo = "Asistencia", icono = R.drawable.ic_check_circle) {
                Text("Llegada 8:05", style = MaterialTheme.typography.bodyLarge)
            }
            BotonFlotante("Registro grupal", R.drawable.ic_group_add, {})
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
