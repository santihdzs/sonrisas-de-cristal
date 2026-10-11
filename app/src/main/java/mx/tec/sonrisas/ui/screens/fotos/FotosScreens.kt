package mx.tec.sonrisas.ui.screens.fotos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TemaFoto
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.domain.UsoFoto
import mx.tec.sonrisas.domain.puedeAparecerEn
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.EstadoChip
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EtiquetaCampo
import mx.tec.sonrisas.ui.components.FotoActividad
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.TipoEstado
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.components.formatoFechaHora
import mx.tec.sonrisas.ui.components.icono
import mx.tec.sonrisas.ui.components.primerNombre
import mx.tec.sonrisas.ui.components.relativoA
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun GaleriaFotos(
    datos: GaleriaDatos,
    onFoto: (Foto) -> Unit,
    modifier: Modifier = Modifier,
    vacioMensaje: String = "Aquí aparecerán las fotos de las actividades.",
    onAgregar: (() -> Unit)? = null,
    encabezado: (@Composable () -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (onAgregar != null) {
                ExtendedFloatingActionButton(
                    onClick = onAgregar,
                    icon = { Icono(R.drawable.ic_add_a_photo, null) },
                    text = { Text("Agregar foto") },
                )
            }
        },
    ) { padding ->
        val estadoGrid = rememberLazyGridState()
        // la lista se ancla por clave; si llega una foto nueva arriba, se regresa al inicio para mostrarla
        val masReciente = datos.grupos.firstOrNull()?.fotos?.firstOrNull()?.id
        var ultimaVista by rememberSaveable { mutableStateOf(masReciente) }
        LaunchedEffect(masReciente) {
            if (masReciente != ultimaVista) {
                estadoGrid.scrollToItem(0)
                ultimaVista = masReciente
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            state = estadoGrid,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space2, bottom = 128.dp),
            horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
            verticalArrangement = Arrangement.spacedBy(Espacio.space2),
        ) {
            if (encabezado != null) item(span = { GridItemSpan(maxLineSpan) }) { encabezado() }
            if (datos.grupos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EstadoVacio(R.drawable.ic_photo_library, "Todavía no hay fotos", mensaje = vacioMensaje)
                }
            }
            datos.grupos.forEach { grupo ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "dia-${grupo.fecha}") {
                    Text(
                        grupo.fecha.relativoA(datos.hoy),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = Espacio.space3, bottom = Espacio.space1),
                    )
                }
                items(grupo.fotos, key = { it.id }) { foto ->
                    Column(
                        modifier = Modifier.clickable(onClickLabel = "Ver foto") { onFoto(foto) },
                        verticalArrangement = Arrangement.spacedBy(Espacio.space1),
                    ) {
                        FotoActividad(
                            foto.tema,
                            datos.salas[foto.salaId]?.estrella ?: ColorEstrella.AZUL,
                            Modifier.fillMaxWidth().aspectRatio(1f),
                            descripcion = foto.descripcion,
                        )
                        Text(
                            foto.descripcion,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private val oraciones = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgregarFotoScreen(
    datos: AgregarFotoDatos,
    form: AgregarFotoForm,
    onCambiar: (AgregarFotoForm) -> Unit,
    onUso: (UsoFoto) -> Unit,
    onAlternar: (Nino) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        Text("Elige la foto", style = MaterialTheme.typography.titleLarge)
        Text(
            "En la versión final la cámara vive dentro de la app y la foto no pasa por la galería del teléfono. En esta demostración elige una imagen de ejemplo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
            verticalArrangement = Arrangement.spacedBy(Espacio.space2),
            maxItemsInEachRow = 3,
        ) {
            TemaFoto.entries.forEach { tema ->
                val activo = form.tema == tema
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .selectable(activo, role = Role.RadioButton) { onCambiar(form.copy(tema = tema)) }
                        .then(
                            if (activo) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), MaterialTheme.shapes.medium)
                            else Modifier,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FotoActividad(tema, datos.sala.estrella, Modifier.fillMaxWidth().aspectRatio(1f))
                    Text(tema.etiqueta, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = Espacio.space1))
                }
            }
        }
        OutlinedTextField(
            value = form.descripcion,
            onValueChange = { onCambiar(form.copy(descripcion = it)) },
            label = { Text("Descripción") },
            supportingText = { Text("Por ejemplo: pintamos con las manos.") },
            keyboardOptions = oraciones,
            modifier = Modifier.fillMaxWidth(),
        )
        EtiquetaCampo("¿Quién puede verla?")
        Column(Modifier.selectableGroup()) {
            UsoFoto.entries.forEach { uso ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(form.uso == uso, role = Role.RadioButton) { onUso(uso) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
                ) {
                    RadioButton(selected = form.uso == uso, onClick = null)
                    Column {
                        Text(uso.etiqueta, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (uso == UsoFoto.INTERNA) "Solo la verán las familias de los niños que etiquetes."
                            else "Además puede publicarse en las redes de la fundación.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        EtiquetaCampo("¿Quién aparece?")
        if (datos.ninos.isEmpty()) {
            Text(
                "Todavía no llega nadie hoy. Registra las llegadas para poder etiquetar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
            verticalArrangement = Arrangement.spacedBy(Espacio.space2),
        ) {
            datos.ninos.forEach { n ->
                NinoEtiquetable(n, datos.sala.estrella, n.id in form.etiquetados, n.puedeAparecerEn(form.uso)) { onAlternar(n) }
            }
        }
        form.faltante?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        form.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        BotonPrincipal("Guardar foto", onGuardar, habilitado = form.puedeGuardar, guardando = form.guardando)
    }
}

@Composable
private fun NinoEtiquetable(n: Nino, estrella: ColorEstrella, marcado: Boolean, permitido: Boolean, onClick: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(88.dp)
            .clip(MaterialTheme.shapes.medium)
            .toggleable(marcado, enabled = permitido, role = Role.Checkbox, onValueChange = { onClick() })
            .semantics {
                if (!permitido) stateDescription = "Sin autorización de imagen"
            }
            .padding(Espacio.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.space1),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AvatarNino(
                n.nombre, n.apellidos, estrella, tamano = 56.dp,
                modifier = Modifier
                    .alpha(if (permitido) 1f else 0.38f)
                    .then(if (marcado) Modifier.border(3.dp, esquema.primary, CircleShape) else Modifier),
            )
            if (marcado) {
                Surface(shape = CircleShape, color = esquema.primary, contentColor = esquema.onPrimary, modifier = Modifier.align(Alignment.BottomEnd)) {
                    Icono(R.drawable.ic_check, null, Modifier.size(18.dp))
                }
            }
            if (!permitido) {
                Icono(R.drawable.ic_lock, null, Modifier.size(24.dp), tint = esquema.onSurface)
            }
        }
        Text(primerNombre(n.nombre), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!permitido) {
            Text("Sin autorización de imagen", style = MaterialTheme.typography.labelMedium, color = esquema.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FotoDetalleScreen(
    datos: FotoDetalleDatos?,
    borrando: Boolean,
    onBorrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (datos == null) {
        EstadoVacio(R.drawable.ic_image, "Esta foto ya no está disponible", modifier, mensaje = "Se borró o ya no tienes acceso a ella.")
        return
    }
    var confirmar by remember { mutableStateOf(false) }
    val foto = datos.foto
    val esFamilia = datos.rol == Rol.FAMILIAR
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        FotoActividad(
            foto.tema, datos.sala?.estrella ?: ColorEstrella.AZUL,
            Modifier.fillMaxWidth().aspectRatio(4f / 3f), tamanoIcono = 96.dp, descripcion = foto.descripcion,
        )
        Text(foto.descripcion, style = MaterialTheme.typography.headlineSmall)
        Text(
            listOfNotNull(foto.cuando.formatoFechaHora(), datos.sala?.let { "Sala ${it.nombre}" }).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EstadoChip(
            if (foto.uso == UsoFoto.INTERNA) "Solo para las familias" else "También para redes",
            TipoEstado.NEUTRO,
            if (foto.uso == UsoFoto.INTERNA) R.drawable.ic_lock else R.drawable.ic_public,
        )
        Text(if (esFamilia) "Aparece" else "Aparecen", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space3), verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
            datos.etiquetados.forEach { n ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                    AvatarNino(n.nombre, n.apellidos, datos.sala?.estrella ?: ColorEstrella.AZUL, tamano = 32.dp)
                    Text(if (esFamilia) n.nombre else n.nombreCompleto, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (esFamilia) {
            Text(
                "Por privacidad solo ves el nombre de tu hijo o hija.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = { confirmar = true },
            enabled = !borrando,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icono(R.drawable.ic_delete, null)
            Text(if (esFamilia) "Pedir que se borre" else "Borrar foto", modifier = Modifier.padding(start = Espacio.space2))
        }
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            icon = { Icono(R.drawable.ic_delete, null) },
            title = { Text("¿Borrar esta foto?") },
            text = { Text("Esta foto se borrará para todas las familias. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = false
                    onBorrar()
                }) { Text("Borrar foto", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } },
        )
    }
}

private val salaEjemplo = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
private val sofia = Nino("1", "Sofía", "Ramírez Treviño", LocalDate.of(2024, 4, 18), "e", "s", emptyList(), emptyList())
private val diego = Nino("2", "Diego", "Garza Leal", LocalDate.of(2024, 1, 2), "e", "s", emptyList(), emptyList(), permisoImagenInterna = false)

@Preview(showBackground = true)
@Composable
private fun GaleriaPreview() {
    val hoy = LocalDate.of(2026, 9, 30)
    val fotos = listOf(
        Foto("f1", "s", LocalDateTime.of(2026, 9, 29, 10, 0), "Pintamos con las manos", listOf("1"), UsoFoto.INTERNA, TemaFoto.PINTURA),
        Foto("f2", "s", LocalDateTime.of(2026, 9, 29, 11, 0), "Regamos las plantas", listOf("1"), UsoFoto.INTERNA, TemaFoto.JARDIN),
        Foto("f3", "s", LocalDateTime.of(2026, 9, 25, 9, 0), "Hora del cuento", listOf("1"), UsoFoto.REDES, TemaFoto.LECTURA),
    )
    SonrisasTheme { GaleriaFotos(GaleriaDatos(agrupar(fotos), mapOf("s" to salaEjemplo), hoy), {}, onAgregar = {}) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun AgregarFotoPreview() {
    SonrisasTheme {
        AgregarFotoScreen(
            AgregarFotoDatos(salaEjemplo, listOf(sofia, diego)),
            AgregarFotoForm(tema = TemaFoto.MUSICA, etiquetados = setOf("1")),
            {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FotoDetallePreview() {
    val foto = Foto("f1", "s", LocalDateTime.of(2026, 9, 29, 10, 0), "Pintamos con las manos", listOf("1"), UsoFoto.INTERNA, TemaFoto.PINTURA)
    SonrisasTheme { FotoDetalleScreen(FotoDetalleDatos(foto, salaEjemplo, listOf(sofia), Rol.FAMILIAR, LocalDate.of(2026, 9, 30)), false, {}) }
}
