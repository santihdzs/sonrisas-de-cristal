package mx.tec.sonrisas.ui.screens.direccion

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import mx.tec.sonrisas.ui.theme.Radio
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import mx.tec.sonrisas.ui.components.EtiquetaSeccion
import mx.tec.sonrisas.ui.components.BotonFlotante
import mx.tec.sonrisas.ui.components.BannerAviso
import mx.tec.sonrisas.ui.components.DialogoConfirmar
import mx.tec.sonrisas.ui.components.FormaAvatar
import androidx.compose.material3.ButtonDefaults
import mx.tec.sonrisas.ui.components.ChipFiltro
import mx.tec.sonrisas.ui.components.SelectorSegmentado
import mx.tec.sonrisas.ui.components.sombraSuave
import mx.tec.sonrisas.ui.theme.Elevacion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import mx.tec.sonrisas.ui.components.Pildora
import mx.tec.sonrisas.ui.components.ProgresoDelgado
import mx.tec.sonrisas.ui.components.TarjetaBase
import mx.tec.sonrisas.ui.components.TarjetaTeal
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.data.ResumenEstancia
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.DIAS_PERIODO_BAJA
import mx.tec.sonrisas.domain.EstadoExpediente
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.PersonaAutorizada
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.BannerAlergias
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.EstadoChip
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.TipoEstado
import mx.tec.sonrisas.ui.components.coincideCon
import mx.tec.sonrisas.ui.components.edadTexto
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.components.formatoCorto
import mx.tec.sonrisas.ui.components.formatoFechaHora
import mx.tec.sonrisas.ui.components.formatoHora
import mx.tec.sonrisas.ui.components.formatoLargo
import mx.tec.sonrisas.ui.components.icono
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InicioScreen(
    datos: InicioDatos,
    onIncidente: (Incidente) -> Unit,
    onInscribir: () -> Unit,
    onPublicarAviso: () -> Unit,
    onVerAvisos: () -> Unit,
    onObjetos: (Sala) -> Unit,
    onMaterial: (Sala) -> Unit,
    onImprimir: (Sala) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space1, bottom = Espacio.space6),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        item {
            Text(
                listOfNotNull(datos.estancia?.nombre, datos.hoy.formatoLargo()).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { ResumenHoy(datos, onInscribir, onPublicarAviso) }
        item { EtiquetaSeccion("Salas") }
        items(datos.salas, key = { it.sala.id }) { s -> TarjetaSalaInicio(s, onMaterial, onImprimir, onObjetos) }
        item { EtiquetaSeccion("Incidentes de hoy") }
        if (datos.incidentes.isEmpty()) {
            item {
                TarjetaBase(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(Espacio.space4),
                        horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(SonrisasTheme.colors.exitoContenedor), contentAlignment = Alignment.Center) {
                            Icono(R.drawable.ic_check, null, Modifier.size(20.dp), tint = SonrisasTheme.colors.exito)
                        }
                        Text("Sin incidentes registrados hoy.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        items(datos.incidentes, key = { it.first.id }) { (inc, nino) ->
            val esquema = MaterialTheme.colorScheme
            TarjetaSeccion(onClick = { onIncidente(inc) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                    Pildora(inc.tipo.etiqueta, esquema.errorContainer, esquema.onErrorContainer, icono = R.drawable.ic_error_fill, colorIcono = esquema.error)
                    Text(inc.cuando.formatoHora(), style = MaterialTheme.typography.labelLarge)
                    Text(inc.folio, style = MaterialTheme.typography.labelMedium, color = esquema.onSurfaceVariant)
                }
                Text(nino.nombreCompleto, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(inc.descripcion, style = MaterialTheme.typography.bodyMedium, color = esquema.onSurfaceVariant)
            }
        }
        item {
            EtiquetaSeccion("Últimos avisos", accion = {
                TextButton(onClick = onVerAvisos, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ver todos") }
            })
        }
        if (datos.avisos.isEmpty()) {
            item { Text("Todavía no hay avisos publicados.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(datos.avisos, key = { it.id }) { a ->
            TarjetaBase(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(Espacio.space4),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icono(a.tipo.icono, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(a.titulo, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "${a.tipo.etiqueta} · Publicado el ${a.publicadoEl.formatoFechaHora()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** tarjeta teal del patron B: presentes de toda la estancia y las dos acciones rapidas */
@Composable
private fun ResumenHoy(datos: InicioDatos, onInscribir: () -> Unit, onPublicarAviso: () -> Unit) {
    val presentes = datos.salas.sumOf { it.presentes }
    val inscritos = datos.salas.sumOf { it.inscritos }
    val fraccion = if (inscritos == 0) 0f else presentes / inscritos.toFloat()
    TarjetaTeal {
        Text("Hoy en la estancia", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$presentes de $inscritos presentes", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("${(fraccion * 100).roundToInt()} %", style = MaterialTheme.typography.titleMedium)
        }
        ProgresoDelgado(fraccion, Modifier.padding(top = Espacio.space2, bottom = Espacio.space4))
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            BotonTranslucido("Inscribir niño", R.drawable.ic_person_add, onInscribir, Modifier.weight(1f))
            BotonTranslucido("Publicar aviso", R.drawable.ic_campaign, onPublicarAviso, Modifier.weight(1f))
        }
    }
}

/** boton en pildora blanca translucida para usar dentro de la tarjeta teal */
@Composable
private fun BotonTranslucido(texto: String, @DrawableRes icono: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Row(
            Modifier.padding(horizontal = Espacio.space3, vertical = Espacio.space3),
            horizontalArrangement = Arrangement.spacedBy(Espacio.space2, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icono(icono, null, Modifier.size(20.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** sala con su estrella, educadora, presentes y una barra del color de la sala; abajo sus accesos */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaSalaInicio(s: SalaResumen, onMaterial: (Sala) -> Unit, onImprimir: (Sala) -> Unit, onObjetos: (Sala) -> Unit) {
    val par = SonrisasTheme.colors.estrella(s.sala.estrella)
    val fraccion = if (s.inscritos == 0) 0f else s.presentes / s.inscritos.toFloat()
    TarjetaSeccion {
        EstrellaSala("Sala ${s.sala.nombre}", s.sala.estrella)
        s.educadora?.let { Text("Educadora: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${s.presentes} de ${s.inscritos} presentes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${(fraccion * 100).roundToInt()} %", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ProgresoDelgado(fraccion, color = par.glifo, pista = MaterialTheme.colorScheme.surfaceContainer)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space1)) {
            TextButton(onClick = { onMaterial(s.sala) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_folder, null)
                Text("Material", modifier = Modifier.padding(start = Espacio.space2))
            }
            TextButton(onClick = { onImprimir(s.sala) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_print, null)
                Text("Bitácora", modifier = Modifier.padding(start = Espacio.space2))
            }
            TextButton(onClick = { onObjetos(s.sala) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_checkroom, null)
                Text("Objetos perdidos", modifier = Modifier.padding(start = Espacio.space2))
            }
        }
    }
}

@Composable
fun NinosScreen(
    datos: NinosDatos,
    busqueda: String,
    salaId: String?,
    bajas: Boolean,
    onBuscar: (String) -> Unit,
    onSala: (String?) -> Unit,
    onBajas: (Boolean) -> Unit,
    onNino: (Nino) -> Unit,
    onInscribir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val salas = datos.salas.associateBy { it.id }
    val filtrados = datos.ninos
        .filter { if (bajas) it.estado is EstadoExpediente.BajaTemporal else it.activo }
        .filter { salaId == null || it.salaId == salaId }
        .filter { it.nombreCompleto.coincideCon(busqueda) }
    val activos = datos.ninos.count { it.activo }
    val enBaja = datos.ninos.count { it.estado is EstadoExpediente.BajaTemporal }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = { BotonFlotante("Inscribir niño", R.drawable.ic_person_add, onInscribir) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space1, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            // tarjeta teal del patron B con el conteo de niños
            item {
                TarjetaTeal {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (activos == 1) "1 niño activo" else "$activos niños activos", style = MaterialTheme.typography.headlineSmall)
                            if (enBaja > 0) {
                                Text("$enBaja en baja temporal", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                            Icono(R.drawable.ic_groups, null)
                        }
                    }
                }
            }
            item {
                // buscador en pildora blanca, sin contorno
                TextField(
                    value = busqueda,
                    onValueChange = onBuscar,
                    placeholder = { Text("Buscar por nombre") },
                    leadingIcon = { Icono(R.drawable.ic_search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SonrisasTheme.colors.superficieElevada,
                        unfocusedContainerColor = SonrisasTheme.colors.superficieElevada,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().sombraSuave(Elevacion.tarjeta, CircleShape).semantics { contentDescription = "Buscar por nombre" },
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                    item { ChipFiltro("Todas", salaId == null, { onSala(null) }) }
                    items(datos.salas, key = { it.id }) { s -> ChipFiltro(s.nombre, salaId == s.id, { onSala(s.id) }) }
                }
            }
            item {
                SelectorSegmentado(
                    opciones = listOf(false, true),
                    seleccionada = bajas,
                    onSeleccion = onBajas,
                    etiqueta = { if (it) "Baja temporal" else "Activos" },
                )
            }
            item { EtiquetaSeccion(if (bajas) "Baja temporal" else "Niños") }
            if (filtrados.isEmpty()) {
                item {
                    EstadoVacio(
                        R.drawable.ic_child_care,
                        if (bajas) "No hay expedientes en baja temporal" else "No encontramos niños",
                        mensaje = if (bajas) null else "Revisa la búsqueda o el filtro de sala.",
                    )
                }
            }
            items(filtrados, key = { it.id }) { n ->
                val sala = salas[n.salaId]
                val estado = n.estado
                TarjetaBase(Modifier.fillMaxWidth(), onClick = { onNino(n) }) {
                    Row(
                        Modifier.padding(horizontal = Espacio.space4, vertical = Espacio.space3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
                    ) {
                        AvatarNino(n.nombre, n.apellidos, sala?.estrella ?: ColorEstrella.AZUL)
                        Column(Modifier.weight(1f)) {
                            Text(n.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (estado is EstadoExpediente.BajaTemporal) "Se elimina el ${estado.eliminarEl.formatoCorto()}"
                                else listOfNotNull(sala?.let { "Sala ${it.nombre}" }, edadTexto(n.fechaNacimiento, datos.hoy)).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (estado is EstadoExpediente.BajaTemporal) SonrisasTheme.colors.enAvisoContenedor else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icono(R.drawable.ic_chevron_right, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpedienteScreen(
    datos: ExpedienteDatos?,
    trabajando: Boolean,
    onEditar: () -> Unit,
    onBaja: () -> Unit,
    onReactivar: () -> Unit,
    onEliminar: () -> Unit,
    onImprimir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (datos == null) {
        EstadoVacio(R.drawable.ic_badge, "Este expediente ya no existe", modifier, mensaje = "Se eliminó de forma definitiva junto con sus fotos.")
        return
    }
    var confirmar by remember { mutableStateOf<String?>(null) }
    val n = datos.nino
    val baja = n.estado as? EstadoExpediente.BajaTemporal
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        TarjetaBase(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(Espacio.space4), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                AvatarNino(n.nombre, n.apellidos, datos.sala?.estrella ?: ColorEstrella.AZUL, tamano = 64.dp, forma = FormaAvatar.CUADRADO)
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                    Text(n.nombreCompleto, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    datos.sala?.let { EstrellaSala("Sala ${it.nombre}", it.estrella, tamano = 24.dp) }
                }
            }
        }
        if (baja != null) {
            // eliminacion temporal: aviso amarillo con la fecha en que se borra definitivamente
            BannerAviso {
                Text("Baja temporal desde el ${baja.fechaBaja.formatoCorto()}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Si no se reactiva, el expediente y sus fotos se eliminarán el ${baja.eliminarEl.formatoLargo().lowercase()}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        TarjetaSeccion(titulo = "Datos generales", icono = R.drawable.ic_badge) {
            Dato("Fecha de nacimiento", n.fechaNacimiento.formatoCorto())
            Dato("Edad", edadTexto(n.fechaNacimiento, datos.hoy))
            Dato("Sala", datos.sala?.nombre ?: "")
        }
        TarjetaSeccion(titulo = "Información médica", icono = R.drawable.ic_medical_services) {
            if (n.tieneInfoMedica) BannerAlergias(n.alergias, n.padecimientos, n.medicamentos)
            else Text("Sin alergias, padecimientos ni medicamentos registrados.", style = MaterialTheme.typography.bodyLarge)
        }
        TarjetaSeccion(titulo = "Tutores", icono = R.drawable.ic_family_restroom) {
            datos.tutores.forEachIndexed { i, t ->
                if (i > 0) HorizontalDivider()
                Persona(t.nombre, t.parentesco, t.telefono)
                val cuenta = datos.cuentas[t.id]
                if (cuenta != null) EstadoChip("Con acceso a la app · ${cuenta.usuario}", TipoEstado.EXITO, R.drawable.ic_check_circle_fill)
                else EstadoChip("Sin acceso a la app", TipoEstado.NEUTRO, R.drawable.ic_lock)
            }
        }
        TarjetaSeccion(titulo = "Personas autorizadas para recogerlo", icono = R.drawable.ic_person) {
            n.personasAutorizadas.forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider()
                Persona(p.nombre, p.parentesco, p.telefono)
            }
        }
        TarjetaSeccion(titulo = "Autorizaciones de imagen", icono = R.drawable.ic_image) {
            Autorizacion("Actividades internas de la estancia", n.permisoImagenInterna)
            Autorizacion("Redes sociales de la fundación", n.permisoImagenRedes)
        }
        FilledTonalButton(onClick = onImprimir, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icono(R.drawable.ic_print, null)
            Text("Imprimir bitácora", modifier = Modifier.padding(start = Espacio.space2))
        }
        // dar de baja (eliminacion temporal) y eliminar son destructivas: rojo apagado y siempre confirman
        val rojoApagado = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
        )
        if (baja == null) {
            BotonPrincipal("Editar expediente", onEditar, guardando = false)
            FilledTonalButton(onClick = { confirmar = "baja" }, enabled = !trabajando, colors = rojoApagado, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Dar de baja")
            }
        } else {
            BotonPrincipal("Reactivar", onReactivar, guardando = trabajando)
            FilledTonalButton(onClick = { confirmar = "eliminar" }, enabled = !trabajando, colors = rojoApagado, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_delete_forever, null)
                Text("Eliminar definitivamente", modifier = Modifier.padding(start = Espacio.space2))
            }
        }
    }
    when (confirmar) {
        "baja" -> DialogoConfirmar(
            titulo = "¿Dar de baja a ${n.nombre}?",
            mensaje = "El expediente pasa a baja temporal durante $DIAS_PERIODO_BAJA días. En ese periodo puedes reactivarlo. " +
                "Al terminar, el expediente y todas sus fotos se eliminan de forma definitiva.",
            confirmar = "Dar de baja",
            onConfirmar = { confirmar = null; onBaja() },
            onCancelar = { confirmar = null },
        )
        "eliminar" -> DialogoConfirmar(
            titulo = "¿Eliminar definitivamente?",
            mensaje = "Se borrarán el expediente de ${n.nombreCompleto}, su bitácora y todas las fotos donde aparece. No se puede deshacer.",
            confirmar = "Eliminar",
            onConfirmar = { confirmar = null; onEliminar() },
            onCancelar = { confirmar = null },
        )
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Persona(nombre: String, parentesco: String, telefono: String) {
    Column {
        Text(nombre, style = MaterialTheme.typography.titleMedium)
        Text("$parentesco · $telefono", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Autorizacion(texto: String, otorgada: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (otorgada) EstadoChip("Autorizada", TipoEstado.EXITO, R.drawable.ic_check_circle_fill)
        else EstadoChip("Sin autorización", TipoEstado.NEUTRO, R.drawable.ic_lock)
    }
}

@Composable
fun MasScreen(
    onAsistencia: () -> Unit,
    onHorarios: () -> Unit,
    onActividad: () -> Unit,
    onSalas: () -> Unit,
    onPersonal: () -> Unit,
    onPerfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opciones = listOf(
        Triple("Salas", "Registrar salas y elegir el color de su estrella", R.drawable.ic_meeting_room) to onSalas,
        Triple("Personal", "Educadoras, su sala y su acceso a la app", R.drawable.ic_badge) to onPersonal,
        Triple("Reporte de asistencia", "Asistencia por sala y por mes", R.drawable.ic_bar_chart) to onAsistencia,
        Triple("Horarios", "Actividades de cada sala y actividades especiales", R.drawable.ic_calendar_month) to onHorarios,
        Triple("Registro de actividad", "Quién capturó o consultó cada dato", R.drawable.ic_history) to onActividad,
        Triple("Mi perfil", "Cuenta, desbloqueo con huella y cerrar sesión", R.drawable.ic_account_circle) to onPerfil,
    )
    // cada herramienta es una tarjeta con su icono en circulo teal suave
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Espacio.space4, vertical = Espacio.space2),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        opciones.forEach { (o, accion) ->
            TarjetaBase(Modifier.fillMaxWidth(), onClick = accion) {
                Row(
                    Modifier.padding(horizontal = Espacio.space4, vertical = Espacio.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
                ) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icono(o.third, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(o.first, style = MaterialTheme.typography.titleMedium)
                        Text(o.second, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icono(R.drawable.ic_chevron_right, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun FundacionScreen(
    datos: FundacionDatos,
    onReporte: () -> Unit,
    onRegistrar: () -> Unit,
    onEstancia: (Estancia) -> Unit,
    onDesbloquear: (Usuario) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = { BotonFlotante("Registrar estancia", R.drawable.ic_add, onRegistrar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space1, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            // tarjeta teal del patron B con los totales de la fundacion y el acceso al reporte
            item {
                val fraccion = if (datos.total == 0) 0f else datos.presentes / datos.total.toFloat()
                TarjetaTeal {
                    Text("Todas las estancias", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                    Text(if (datos.total == 1) "1 niño inscrito" else "${datos.total} niños inscritos", style = MaterialTheme.typography.headlineSmall)
                    Text("${datos.presentes} presentes ahora", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    ProgresoDelgado(fraccion, Modifier.padding(top = Espacio.space3, bottom = Espacio.space4))
                    BotonTranslucido("Ver reporte de asistencia", R.drawable.ic_bar_chart, onReporte, Modifier.fillMaxWidth())
                }
            }
            item { EtiquetaSeccion("Estancias") }
            items(datos.estancias, key = { it.estancia.id }) { r ->
                TarjetaSeccion(onClick = { onEstancia(r.estancia) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icono(R.drawable.ic_apartment, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(r.estancia.nombre, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                listOf(r.estancia.colonia, r.estancia.telefono).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icono(R.drawable.ic_edit, "Editar ${r.estancia.nombre}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.padding(top = Espacio.space2), horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                        Cifra("${r.inscritos}", "inscritos", Modifier.weight(1f))
                        Cifra("${r.presentesHoy}", "presentes hoy", Modifier.weight(1f))
                        Cifra("${r.promedioMes} %", "asistencia del mes", Modifier.weight(1f))
                    }
                }
            }
            // P4: cuentas con bloqueo largo por intentos fallidos desde varios lugares; solo aparece si hay alguna
            if (datos.bloqueadas.isNotEmpty()) item { EtiquetaSeccion("Cuentas bloqueadas") }
            items(datos.bloqueadas, key = { "bloqueo-${it.id}" }) { u ->
                TarjetaSeccion {
                    Text(u.nombre, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Usuario: ${u.usuario}. Hubo intentos fallidos de entrar desde varios lugares; se desbloquea sola en 3 días.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(onClick = { onDesbloquear(u) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Desbloquear")
                    }
                }
            }
            item {
                Text(
                    "La fundación ve datos consolidados. Las bitácoras, expedientes y fotos de cada niño solo los ve el personal de su estancia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** cifra en un cuadro gris claro: valor grande y etiqueta abajo */
@Composable
private fun Cifra(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(Radio.r16))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = Espacio.space3, vertical = Espacio.space2),
    ) {
        Text(valor, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val salaEj = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
private val mamaEj = PersonaAutorizada("p1", "Laura Treviño Garza", "Mamá", "81 2345 6789")
private val sofiaEj = Nino("1", "Sofía", "Ramírez Treviño", LocalDate.of(2024, 4, 18), "e", "s", listOf("p1"), listOf(mamaEj), alergias = "Cacahuate", permisoImagenRedes = false)

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun InicioPreview() {
    val inc = Incidente("i", "INC-0003", "1", LocalDateTime.of(2026, 9, 30, 10, 40), TipoIncidente.GOLPE, null, "Golpe leve en la rodilla", "Compresa fría", "u")
    val aviso = Aviso("a", "e", TipoAviso.JUNTA, "Junta de padres de familia", "", null, LocalDateTime.of(2026, 9, 29, 9, 0), "d")
    SonrisasTheme {
        InicioScreen(
            InicioDatos(Estancia("e", "Estancia San Bernabé", "San Bernabé"), listOf(SalaResumen(salaEj, "Mariana López", 12, 14)), listOf(inc to sofiaEj), listOf(aviso), LocalDate.of(2026, 9, 30)),
            {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NinosPreview() {
    SonrisasTheme { NinosScreen(NinosDatos(listOf(sofiaEj), listOf(salaEj), LocalDate.of(2026, 9, 30)), "", null, false, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ExpedientePreview() {
    val tutor = Tutor("p1", "Laura Treviño Garza", "Mamá", "81 2345 6789")
    SonrisasTheme { ExpedienteScreen(ExpedienteDatos(sofiaEj, salaEj, listOf(tutor), LocalDate.of(2026, 9, 30)), false, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun MasPreview() {
    SonrisasTheme { MasScreen({}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun FundacionPreview() {
    val e = Estancia("e", "Estancia San Bernabé", "San Bernabé, Monterrey")
    SonrisasTheme { FundacionScreen(FundacionDatos(listOf(ResumenEstancia(e, 34, 28, 91))), {}, {}, {}, {}) }
}
