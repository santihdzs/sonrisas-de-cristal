package mx.tec.sonrisas.ui.screens.direccion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import mx.tec.sonrisas.ui.components.BarraAccionesInferior
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.ProgresoDelgado
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.text.style.TextAlign
import mx.tec.sonrisas.ui.components.ChipFiltro
import mx.tec.sonrisas.ui.components.DialogoConfirmar
import mx.tec.sonrisas.ui.components.EtiquetaSeccion
import mx.tec.sonrisas.ui.components.GlifoEstrella
import mx.tec.sonrisas.ui.components.TarjetaBase
import mx.tec.sonrisas.ui.components.TarjetaTeal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        if (conTodas) item { ChipFiltro("Todas", seleccion == null, { onSala(null) }) }
        items(salas, key = { it.id }) { s -> ChipFiltro(s.nombre, seleccion == s.id, { onSala(s.id) }) }
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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            BarraAccionesInferior(relleno = PaddingValues(Espacio.space4)) {
                BotonPrincipal("Compartir reporte", onCompartir)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space1, bottom = Espacio.space4),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            if (datos.esFundacion) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                        items(datos.estancias, key = { it.id }) { e ->
                            ChipFiltro(e.nombre, datos.estanciaId == e.id, { onEstancia(e.id) })
                        }
                    }
                }
            }
            item { ChipsSala(datos.salas, datos.salaId, onSala) }
            // mes y cifras del mes en tarjeta teal (patron B)
            item {
                TarjetaTeal {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onAnterior) { Icono(R.drawable.ic_chevron_left, "Mes anterior") }
                        Text(r.mes.atDay(1).formatoMes(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = onSiguiente,
                            enabled = datos.puedeAvanzar,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White, disabledContentColor = Color.White.copy(alpha = 0.35f)),
                        ) { Icono(R.drawable.ic_chevron_right, "Mes siguiente") }
                    }
                    Row(Modifier.padding(top = Espacio.space3), horizontalArrangement = Arrangement.spacedBy(Espacio.space4)) {
                        Column(Modifier.weight(1f)) {
                            Text("${r.promedio} %", style = MaterialTheme.typography.displaySmall)
                            Text("asistencia promedio", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("${r.diasHabiles}", style = MaterialTheme.typography.headlineSmall)
                            Text("días hábiles", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                    ProgresoDelgado(r.promedio / 100f, Modifier.padding(top = Espacio.space3))
                }
            }
            if (datos.salaId == null && r.salas.isNotEmpty()) {
                item { EtiquetaSeccion("Por sala") }
                item {
                    TarjetaSeccion {
                        r.salas.forEachIndexed { i, s ->
                            FilaPorcentaje(s.nombre, "${s.inscritos} niños", s.promedio, salas[s.salaId]?.estrella, primera = i == 0)
                        }
                    }
                }
            }
            if (r.filas.isNotEmpty()) {
                item { EtiquetaSeccion("Por niño") }
                item {
                    TarjetaSeccion {
                        r.filas.forEachIndexed { i, f ->
                            FilaPorcentaje(f.nombre, "${f.asistidos} de ${f.habiles} días", f.porcentaje, null, primera = i == 0)
                        }
                    }
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

/** renglon con nombre, detalle, porcentaje y una barra delgada; la sala usa su color, el niño teal */
@Composable
private fun FilaPorcentaje(titulo: String, detalle: String, porcentaje: Int, estrella: ColorEstrella?, primera: Boolean) {
    val esquema = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
        if (!primera) HorizontalDivider(Modifier.padding(bottom = Espacio.space2), color = esquema.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            Column(Modifier.weight(1f)) {
                if (estrella != null) EstrellaSala(titulo, estrella, tamano = 24.dp) else Text(titulo, style = MaterialTheme.typography.bodyLarge)
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = esquema.onSurfaceVariant)
            }
            Text("$porcentaje %", style = MaterialTheme.typography.titleMedium)
        }
        ProgresoDelgado(
            porcentaje / 100f,
            color = estrella?.let { SonrisasTheme.colors.estrella(it).glifo } ?: esquema.primary,
            pista = esquema.surfaceContainer,
        )
    }
}

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
    val esquema = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space1, bottom = Espacio.space6),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        item { ChipsSala(datos.salas, datos.sala.id, { it?.let(onSala) }, conTodas = false) }
        // sala elegida en tarjeta teal, como en el mockup de Horarios
        item {
            TarjetaTeal {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space4)) {
                    GlifoEstrella(datos.sala.estrella, tamano = 48.dp)
                    Column {
                        Text("Sala ${datos.sala.nombre}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "El horario se configura una vez y las familias lo ven en \"Ahora mismo\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
        item { EtiquetaSeccion("Actividades especiales") }
        item {
            TarjetaSeccion {
                if (datos.especiales.isEmpty()) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(esquema.surfaceContainer), contentAlignment = Alignment.Center) {
                            Icono(R.drawable.ic_event_busy, null, Modifier.size(24.dp), tint = esquema.onSurfaceVariant)
                        }
                        Text("No hay actividades especiales próximas.", style = MaterialTheme.typography.bodyLarge, color = esquema.onSurfaceVariant)
                    }
                }
                datos.especiales.forEachIndexed { i, e ->
                    if (i > 0) HorizontalDivider(color = esquema.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.titulo, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${e.fecha.relativoA(datos.hoy)}, ${e.inicio.formatoHora()} a ${e.fin.formatoHora()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = esquema.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { aBorrar = e.id to true }) { Icono(R.drawable.ic_delete, "Borrar ${e.titulo}", tint = esquema.error) }
                    }
                }
                FilledTonalButton(onClick = onNuevaEspecial, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icono(R.drawable.ic_event, null)
                    Text("Publicar actividad especial", modifier = Modifier.padding(start = Espacio.space2))
                }
            }
        }
        item { EtiquetaSeccion("Horario regular") }
        if (datos.bloques.isEmpty()) {
            item { EstadoVacio(R.drawable.ic_calendar_month, "Esta sala no tiene horario", mensaje = "Agrega el primer bloque de actividades.") }
        } else {
            // todos los bloques en una tarjeta, separados por lineas; lapiz gris y bote en rojo apagado
            item {
                TarjetaBase(Modifier.fillMaxWidth()) {
                    Column {
                        datos.bloques.forEachIndexed { i, b ->
                            if (i > 0) HorizontalDivider(color = esquema.outlineVariant)
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = Espacio.space4, end = Espacio.space1, top = Espacio.space2, bottom = Espacio.space2),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
                            ) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(SonrisasTheme.colors.estrella(datos.sala.estrella).glifo))
                                Column(Modifier.weight(1f)) {
                                    Text("${b.inicio.formatoHora()} a ${b.fin.formatoHora()}", style = MaterialTheme.typography.labelLarge, color = esquema.primary)
                                    Text(b.actividad, style = MaterialTheme.typography.bodyLarge)
                                }
                                IconButton(onClick = { onEditarBloque(b) }) { Icono(R.drawable.ic_edit, "Editar ${b.actividad}", tint = esquema.onSurfaceVariant) }
                                IconButton(onClick = { aBorrar = b.id to false }) { Icono(R.drawable.ic_delete, "Borrar ${b.actividad}", tint = esquema.error) }
                            }
                        }
                    }
                }
            }
        }
        item {
            FilledTonalButton(onClick = onNuevoBloque, modifier = Modifier.heightIn(min = 48.dp)) {
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
            CampoTexto(hoja.actividad, { onCambiarHoja(hoja.copy(actividad = it)) }, "Actividad", null)
            hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        is HojaHorario.Especial -> HojaModal(
            "Actividad especial", onCerrar, subtitulo = "Reemplaza al horario regular durante esas horas",
            boton = { BotonPrincipal("Publicar actividad", onGuardar, guardando = hoja.guardando) },
        ) {
            CampoTexto(hoja.titulo, { onCambiarHoja(hoja.copy(titulo = it)) }, "Nombre de la actividad", null)
            CampoFecha("Fecha", hoja.fecha, { it?.let { f -> onCambiarHoja(hoja.copy(fecha = f)) } })
            CampoHora("Inicio", hoja.inicio, { onCambiarHoja(hoja.copy(inicio = it)) })
            CampoHora("Fin", hoja.fin, { onCambiarHoja(hoja.copy(fin = it)) })
            hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        null -> Unit
    }

    aBorrar?.let { (id, especial) ->
        DialogoConfirmar(
            titulo = if (especial) "¿Borrar la actividad especial?" else "¿Borrar este bloque?",
            mensaje = "Las familias dejarán de verlo en el horario.",
            confirmar = "Borrar",
            onConfirmar = {
                if (especial) onBorrarEspecial(id) else onBorrarBloque(id)
                aBorrar = null
            },
            onCancelar = { aBorrar = null },
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
