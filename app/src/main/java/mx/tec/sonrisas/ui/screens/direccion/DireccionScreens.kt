package mx.tec.sonrisas.ui.screens.direccion

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.foundation.clickable
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
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.BannerAlergias
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.EstadoChip
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.TipoEstado
import mx.tec.sonrisas.ui.components.TituloSeccion
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        item {
            Column {
                Text("Hoy en la estancia", style = MaterialTheme.typography.headlineSmall)
                Text(
                    listOfNotNull(datos.estancia?.nombre, datos.hoy.formatoLargo()).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                FilledTonalButton(onClick = onInscribir, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icono(R.drawable.ic_person_add, null)
                    Text("Inscribir niño", modifier = Modifier.padding(start = Espacio.space2))
                }
                FilledTonalButton(onClick = onPublicarAviso, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icono(R.drawable.ic_campaign, null)
                    Text("Publicar aviso", modifier = Modifier.padding(start = Espacio.space2))
                }
            }
        }
        items(datos.salas, key = { it.sala.id }) { s ->
            TarjetaSeccion {
                EstrellaSala("Sala ${s.sala.nombre}", s.sala.estrella)
                s.educadora?.let { Text("Educadora: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                    Text("${s.presentes} de ${s.inscritos} presentes", style = MaterialTheme.typography.titleMedium)
                }
                LinearProgressIndicator(
                    progress = { if (s.inscritos == 0) 0f else s.presentes / s.inscritos.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    drawStopIndicator = {},
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                    TextButton(onClick = { onObjetos(s.sala) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icono(R.drawable.ic_checkroom, null)
                        Text("Objetos perdidos", modifier = Modifier.padding(start = Espacio.space2))
                    }
                }
            }
        }
        item { TituloSeccion("Incidentes de hoy") }
        if (datos.incidentes.isEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalAlignment = Alignment.CenterVertically) {
                    Icono(R.drawable.ic_check_circle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Sin incidentes registrados hoy.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(datos.incidentes, key = { it.first.id }) { (inc, nino) ->
            TarjetaSeccion(onClick = { onIncidente(inc) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                    EstadoChip(inc.tipo.etiqueta, TipoEstado.ERROR, R.drawable.ic_error_fill)
                    Text(inc.cuando.formatoHora(), style = MaterialTheme.typography.labelLarge)
                    Text(inc.folio, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(nino.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                Text(inc.descripcion, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TituloSeccion("Últimos avisos", Modifier.weight(1f))
                TextButton(onClick = onVerAvisos, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ver todos") }
            }
        }
        items(datos.avisos, key = { it.id }) { a ->
            TarjetaSeccion {
                EstadoChip(a.tipo.etiqueta, TipoEstado.NEUTRO, a.tipo.icono)
                Text(a.titulo, style = MaterialTheme.typography.titleMedium)
                Text("Publicado el ${a.publicadoEl.formatoFechaHora()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        .filter { busqueda.isBlank() || it.nombreCompleto.contains(busqueda.trim(), ignoreCase = true) }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onInscribir, icon = { Icono(R.drawable.ic_person_add, null) }, text = { Text("Inscribir niño") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space2, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.space2),
        ) {
            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = onBuscar,
                    label = { Text("Buscar por nombre") },
                    leadingIcon = { Icono(R.drawable.ic_search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                    item {
                        FilterChip(salaId == null, { onSala(null) }, { Text("Todas") }, Modifier.heightIn(min = 48.dp))
                    }
                    items(datos.salas, key = { it.id }) { s ->
                        FilterChip(salaId == s.id, { onSala(s.id) }, { Text(s.nombre) }, Modifier.heightIn(min = 48.dp))
                    }
                }
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(!bajas, { onBajas(false) }, SegmentedButtonDefaults.itemShape(0, 2), Modifier.heightIn(min = 48.dp)) { Text("Activos") }
                    SegmentedButton(bajas, { onBajas(true) }, SegmentedButtonDefaults.itemShape(1, 2), Modifier.heightIn(min = 48.dp)) { Text("Baja temporal") }
                }
            }
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
                ListItem(
                    headlineContent = { Text(n.nombreCompleto, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = {
                        val estado = n.estado
                        Text(
                            if (estado is EstadoExpediente.BajaTemporal) "Se elimina el ${estado.eliminarEl.formatoCorto()}"
                            else listOfNotNull(sala?.let { "Sala ${it.nombre}" }, edadTexto(n.fechaNacimiento, datos.hoy)).joinToString(" · "),
                        )
                    },
                    leadingContent = { AvatarNino(n.nombre, n.apellidos, sala?.estrella ?: ColorEstrella.AZUL) },
                    trailingContent = { Icono(R.drawable.ic_chevron_right, null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable { onNino(n) },
                )
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            AvatarNino(n.nombre, n.apellidos, datos.sala?.estrella ?: ColorEstrella.AZUL, tamano = 64.dp)
            Column {
                Text(n.nombreCompleto, style = MaterialTheme.typography.headlineSmall)
                datos.sala?.let { EstrellaSala("Sala ${it.nombre}", it.estrella, tamano = 24.dp) }
            }
        }
        if (baja != null) {
            EstadoChip("Baja temporal desde el ${baja.fechaBaja.formatoCorto()}", TipoEstado.AVISO, R.drawable.ic_warning_fill)
            Text(
                "Si no se reactiva, el expediente y sus fotos se eliminarán el ${baja.eliminarEl.formatoLargo().lowercase()}.",
                style = MaterialTheme.typography.bodyMedium,
            )
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
        if (baja == null) {
            BotonPrincipal("Editar expediente", onEditar, guardando = false)
            OutlinedButton(onClick = { confirmar = "baja" }, enabled = !trabajando, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Dar de baja")
            }
        } else {
            BotonPrincipal("Reactivar", onReactivar, guardando = trabajando)
            OutlinedButton(onClick = { confirmar = "eliminar" }, enabled = !trabajando, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Eliminar definitivamente", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    when (confirmar) {
        "baja" -> AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text("¿Dar de baja a ${n.nombre}?") },
            text = {
                Text(
                    "El expediente pasa a baja temporal durante $DIAS_PERIODO_BAJA días. En ese periodo puedes reactivarlo. " +
                        "Al terminar, el expediente y todas sus fotos se eliminan de forma definitiva.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmar = null; onBaja() }) { Text("Dar de baja") } },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Cancelar") } },
        )
        "eliminar" -> AlertDialog(
            onDismissRequest = { confirmar = null },
            icon = { Icono(R.drawable.ic_delete_forever, null) },
            title = { Text("¿Eliminar definitivamente?") },
            text = { Text("Se borrarán el expediente de ${n.nombreCompleto}, su bitácora y todas las fotos donde aparece. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmar = null; onEliminar() }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Cancelar") } },
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
    Column(modifier.fillMaxSize().padding(vertical = Espacio.space2)) {
        opciones.forEach { (o, accion) ->
            ListItem(
                headlineContent = { Text(o.first, style = MaterialTheme.typography.titleMedium) },
                supportingContent = { Text(o.second) },
                leadingContent = { Icono(o.third, null) },
                trailingContent = { Icono(R.drawable.ic_chevron_right, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.clickable(onClick = accion),
            )
        }
    }
}

@Composable
fun FundacionScreen(
    datos: FundacionDatos,
    onReporte: () -> Unit,
    onRegistrar: () -> Unit,
    onEstancia: (Estancia) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        item {
            Column {
                Text("${datos.total}", style = MaterialTheme.typography.displaySmall)
                Text("niños inscritos en todas las estancias", style = MaterialTheme.typography.bodyLarge)
                Text("${datos.presentes} presentes ahora", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            FilledTonalButton(onClick = onRegistrar, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_add, null)
                Text("Registrar estancia", modifier = Modifier.padding(start = Espacio.space2))
            }
        }
        items(datos.estancias, key = { it.estancia.id }) { r ->
            TarjetaSeccion(
                titulo = r.estancia.nombre,
                icono = R.drawable.ic_apartment,
                onClick = { onEstancia(r.estancia) },
                accion = { Icono(R.drawable.ic_edit, "Editar ${r.estancia.nombre}", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            ) {
                Text(
                    listOf(r.estancia.colonia, r.estancia.telefono).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space4)) {
                    Cifra("${r.inscritos}", "inscritos", Modifier.weight(1f))
                    Cifra("${r.presentesHoy}", "presentes hoy", Modifier.weight(1f))
                    Cifra("${r.promedioMes} %", "asistencia del mes", Modifier.weight(1f))
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
        item { BotonPrincipal("Ver reporte de asistencia", onReporte) }
    }
}

@Composable
private fun Cifra(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(valor, style = MaterialTheme.typography.titleLarge)
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
            {}, {}, {}, {}, {},
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
    SonrisasTheme { ExpedienteScreen(ExpedienteDatos(sofiaEj, salaEj, listOf(tutor), LocalDate.of(2026, 9, 30)), false, {}, {}, {}, {}) }
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
    SonrisasTheme { FundacionScreen(FundacionDatos(listOf(ResumenEstancia(e, 34, 28, 91))), {}, {}, {}) }
}
