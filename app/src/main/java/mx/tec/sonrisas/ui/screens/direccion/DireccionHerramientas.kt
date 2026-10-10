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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.FilaReporte
import mx.tec.sonrisas.domain.FilaSala
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.ReporteAsistencia
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.CampoFecha
import mx.tec.sonrisas.ui.components.CampoHora
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.HojaModal
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.formatoHora
import mx.tec.sonrisas.ui.components.formatoMes
import mx.tec.sonrisas.ui.components.icono
import mx.tec.sonrisas.ui.components.relativoA
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

@Composable
private fun ChipsSala(salas: List<Sala>, seleccion: String?, onSala: (String?) -> Unit, conTodas: Boolean = true) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
        if (conTodas) item { FilterChip(seleccion == null, { onSala(null) }, { Text("Todas") }, Modifier.heightIn(min = 48.dp)) }
        items(salas, key = { it.id }) { s ->
            FilterChip(seleccion == s.id, { onSala(s.id) }, { Text(s.nombre) }, Modifier.heightIn(min = 48.dp))
        }
    }
}

@Composable
fun ReporteScreen(
    datos: ReporteDatos,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onSala: (String?) -> Unit,
    onEstancia: (String) -> Unit,
    onCompartir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = datos.reporte
    val salas = datos.salas.associateBy { it.id }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                BotonPrincipal("Compartir reporte", onCompartir, Modifier.padding(Espacio.space4))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Espacio.space4),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            if (datos.esFundacion) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                        items(datos.estancias, key = { it.id }) { e ->
                            FilterChip(datos.estanciaId == e.id, { onEstancia(e.id) }, { Text(e.nombre) }, Modifier.heightIn(min = 48.dp))
                        }
                    }
                }
            }
            item { ChipsSala(datos.salas, datos.salaId, onSala) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAnterior) { Icono(R.drawable.ic_chevron_left, "Mes anterior") }
                    Text(r.mes.atDay(1).formatoMes(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onSiguiente, enabled = datos.puedeAvanzar) { Icono(R.drawable.ic_chevron_right, "Mes siguiente") }
                }
            }
            item {
                TarjetaSeccion {
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space4)) {
                        Column(Modifier.weight(1f)) {
                            Text("${r.promedio} %", style = MaterialTheme.typography.displaySmall)
                            Text("asistencia promedio", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("${r.diasHabiles}", style = MaterialTheme.typography.headlineSmall)
                            Text("días hábiles", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (datos.salaId == null && r.salas.isNotEmpty()) {
                item { TituloSeccion("Por sala") }
                items(r.salas, key = { "sala-${it.salaId}" }) { s ->
                    FilaPorcentaje(s.nombre, "${s.inscritos} niños", s.promedio, salas[s.salaId]?.estrella)
                }
            }
            if (r.filas.isNotEmpty()) {
                item { TituloSeccion("Por niño") }
                items(r.filas, key = { it.ninoId }) { f ->
                    FilaPorcentaje(f.nombre, "${f.asistidos} de ${f.habiles} días", f.porcentaje, null)
                }
            } else if (datos.esFundacion) {
                item {
                    Text(
                        "El detalle por niño solo lo ve la dirección de cada estancia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaPorcentaje(titulo: String, detalle: String, porcentaje: Int, estrella: ColorEstrella?) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3), modifier = Modifier.padding(vertical = Espacio.space2)) {
            Column(Modifier.weight(1f)) {
                if (estrella != null) EstrellaSala(titulo, estrella, tamano = 24.dp) else Text(titulo, style = MaterialTheme.typography.bodyLarge)
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$porcentaje %", style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()
    }
}

private val oraciones = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

@Composable
fun HorariosScreen(
    datos: HorariosDatos,
    hoja: HojaHorario?,
    onSala: (String) -> Unit,
    onNuevoBloque: () -> Unit,
    onEditarBloque: (BloqueHorario) -> Unit,
    onBorrarBloque: (String) -> Unit,
    onNuevaEspecial: () -> Unit,
    onBorrarEspecial: (String) -> Unit,
    onCambiarHoja: (HojaHorario) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var aBorrar by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space2),
    ) {
        item { ChipsSala(datos.salas, datos.sala.id, { it?.let(onSala) }, conTodas = false) }
        item {
            Column(Modifier.padding(top = Espacio.space2)) {
                EstrellaSala("Sala ${datos.sala.nombre}", datos.sala.estrella)
                Text(
                    "El horario se configura una vez y las familias lo ven en \"Ahora mismo\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { TituloSeccion("Actividades especiales") }
        if (datos.especiales.isEmpty()) {
            item { Text("No hay actividades especiales próximas.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(datos.especiales, key = { it.id }) { e ->
            TarjetaSeccion {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.titulo, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${e.fecha.relativoA(datos.hoy)}, ${e.inicio.formatoHora()} a ${e.fin.formatoHora()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { aBorrar = e.id to true }) { Icono(R.drawable.ic_delete, "Borrar ${e.titulo}") }
                }
            }
        }
        item {
            OutlinedButton(onClick = onNuevaEspecial, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_event, null)
                Text("Publicar actividad especial", modifier = Modifier.padding(start = Espacio.space2))
            }
        }
        item { TituloSeccion("Horario regular") }
        if (datos.bloques.isEmpty()) {
            item { EstadoVacio(R.drawable.ic_calendar_month, "Esta sala no tiene horario", mensaje = "Agrega el primer bloque de actividades.") }
        }
        items(datos.bloques, key = { it.id }) { b ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
            ) {
                Text(
                    "${b.inicio.formatoHora()} a ${b.fin.formatoHora()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(b.actividad, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { onEditarBloque(b) }) { Icono(R.drawable.ic_edit, "Editar ${b.actividad}") }
                IconButton(onClick = { aBorrar = b.id to false }) { Icono(R.drawable.ic_delete, "Borrar ${b.actividad}") }
            }
        }
        item {
            OutlinedButton(onClick = onNuevoBloque, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_add, null)
                Text("Agregar bloque", modifier = Modifier.padding(start = Espacio.space2))
            }
        }
    }

    when (hoja) {
        is HojaHorario.Bloque -> HojaModal(
            if (hoja.id == null) "Nuevo bloque" else "Editar bloque", onCerrar, subtitulo = "Sala ${datos.sala.nombre}",
            boton = { BotonPrincipal("Guardar bloque", onGuardar, guardando = hoja.guardando) },
        ) {
            CampoHora("Inicio", hoja.inicio, { onCambiarHoja(hoja.copy(inicio = it)) })
            CampoHora("Fin", hoja.fin, { onCambiarHoja(hoja.copy(fin = it)) })
            OutlinedTextField(
                value = hoja.actividad,
                onValueChange = { onCambiarHoja(hoja.copy(actividad = it)) },
                label = { Text("Actividad") },
                singleLine = true,
                keyboardOptions = oraciones,
                modifier = Modifier.fillMaxWidth(),
            )
            hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        is HojaHorario.Especial -> HojaModal(
            "Actividad especial", onCerrar, subtitulo = "Reemplaza al horario regular durante esas horas",
            boton = { BotonPrincipal("Publicar actividad", onGuardar, guardando = hoja.guardando) },
        ) {
            OutlinedTextField(
                value = hoja.titulo,
                onValueChange = { onCambiarHoja(hoja.copy(titulo = it)) },
                label = { Text("Nombre de la actividad") },
                singleLine = true,
                keyboardOptions = oraciones,
                modifier = Modifier.fillMaxWidth(),
            )
            CampoFecha("Fecha", hoja.fecha, { it?.let { f -> onCambiarHoja(hoja.copy(fecha = f)) } })
            CampoHora("Inicio", hoja.inicio, { onCambiarHoja(hoja.copy(inicio = it)) })
            CampoHora("Fin", hoja.fin, { onCambiarHoja(hoja.copy(fin = it)) })
            hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        null -> Unit
    }

    aBorrar?.let { (id, especial) ->
        AlertDialog(
            onDismissRequest = { aBorrar = null },
            title = { Text(if (especial) "¿Borrar la actividad especial?" else "¿Borrar este bloque?") },
            text = { Text("Las familias dejarán de verlo en el horario.") },
            confirmButton = {
                TextButton(onClick = {
                    if (especial) onBorrarEspecial(id) else onBorrarBloque(id)
                    aBorrar = null
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { aBorrar = null }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActividadScreen(
    datos: ActividadDatos,
    filtro: FiltroActividad,
    onFiltro: (FiltroActividad) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtrados = datos.registros.filter { filtro.acciones == null || it.accion in filtro.acciones }
    val porDia = filtrados.groupBy { it.cuando.toLocalDate() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space2),
    ) {
        item {
            // con tantas categorias, una sola fila deslizable ocupa menos que varias filas de chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                items(FiltroActividad.entries) { f ->
                    FilterChip(f == filtro, { onFiltro(f) }, { Text(f.etiqueta) }, Modifier.heightIn(min = 48.dp))
                }
            }
        }
        if (filtrados.isEmpty()) {
            item { EstadoVacio(R.drawable.ic_history, "Sin registros con este filtro") }
        }
        porDia.forEach { (fecha, registros) ->
            item(key = "dia-$fecha") {
                Text(fecha.relativoA(datos.hoy), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Espacio.space3))
            }
            items(registros, key = { it.id }) { r -> FilaRegistro(r, r.ninoId?.let { datos.ninos[it]?.nombreCompleto }) }
        }
    }
}

@Composable
private fun FilaRegistro(r: RegistroActividad, nino: String?) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Espacio.space1),
        horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        Icono(r.accion.icono, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(r.detalle, style = MaterialTheme.typography.bodyLarge)
            Text(
                listOfNotNull(r.cuando.formatoHora(), r.personaNombre, nino?.let { "Niño: $it" }).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val salasEj = listOf(
    Sala("a", "e", "Lactantes", TipoSala.LACTANTES, ColorEstrella.AZUL),
    Sala("b", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ReportePreview() {
    val mes = YearMonth.of(2026, 9)
    val r = ReporteAsistencia(
        mes, 21,
        listOf(FilaReporte("1", "Sofía Ramírez Treviño", "b", 19, 21), FilaReporte("2", "Diego Garza Leal", "b", 17, 21)),
        listOf(FilaSala("a", "Lactantes", 8, 88), FilaSala("b", "Maternales", 14, 90)),
    )
    SonrisasTheme {
        ReporteScreen(ReporteDatos(r, salasEj, listOf(Estancia("e", "Estancia San Bernabé", "")), false, mes, null, "e"), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HorariosPreview() {
    val hoy = LocalDate.of(2026, 9, 30)
    val bloques = listOf(
        BloqueHorario("1", "b", LocalTime.of(7, 15), LocalTime.of(8, 30), "Recepción"),
        BloqueHorario("2", "b", LocalTime.of(8, 30), LocalTime.of(9, 15), "Desayuno"),
    )
    val especial = ActividadEspecial("e", "b", hoy, LocalTime.of(11, 15), LocalTime.of(12, 15), "Visita del cuentacuentos")
    SonrisasTheme {
        HorariosScreen(HorariosDatos(salasEj, salasEj[1], bloques, listOf(especial), hoy), null, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ActividadPreview() {
    val hoy = LocalDate.of(2026, 9, 30)
    val registros = listOf(
        RegistroActividad("1", LocalDateTime.of(hoy, LocalTime.of(10, 41)), "u", "Mariana López Hernández", AccionRegistro.CAPTURA, "Registró el incidente INC-0003"),
        RegistroActividad("2", LocalDateTime.of(hoy, LocalTime.of(9, 12)), "f", "Laura Treviño Garza", AccionRegistro.FOTO_CONSULTADA, "Consultó la foto \"Pintamos con las manos\""),
    )
    SonrisasTheme { ActividadScreen(ActividadDatos(registros, emptyMap(), hoy), FiltroActividad.TODO, {}) }
}
