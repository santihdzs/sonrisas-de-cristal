package mx.tec.sonrisas.ui.screens.familia

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import mx.tec.sonrisas.ui.components.EstadoPildora
import mx.tec.sonrisas.ui.components.LocaleMx
import mx.tec.sonrisas.ui.components.Pildora
import mx.tec.sonrisas.ui.components.PildoraEstado
import mx.tec.sonrisas.ui.components.TarjetaBase
import mx.tec.sonrisas.ui.components.TipoTono
import mx.tec.sonrisas.ui.components.sombraSuave
import mx.tec.sonrisas.ui.components.tono
import mx.tec.sonrisas.ui.theme.Elevacion
import mx.tec.sonrisas.ui.theme.EstiloEtiquetaSeccion
import mx.tec.sonrisas.ui.theme.Radio
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ActividadActual
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.DiaSemana
import mx.tec.sonrisas.domain.EstadoAnimo
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.PersonaAutorizada
import mx.tec.sonrisas.domain.Porcion
import mx.tec.sonrisas.domain.ResumenDia
import mx.tec.sonrisas.domain.ResumenSemana
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TiempoComida
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.BotonAcceso
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.EstadoChip
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.TipoEstado
import mx.tec.sonrisas.ui.components.detallesAnimo
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.components.formatoDiaMes
import mx.tec.sonrisas.ui.components.formatoHora
import mx.tec.sonrisas.ui.components.formatoLargo
import mx.tec.sonrisas.ui.components.icono
import mx.tec.sonrisas.ui.components.minutosTexto
import mx.tec.sonrisas.ui.components.nombreDia
import mx.tec.sonrisas.ui.components.primerNombre
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** selector de hijo en pildora blanca flotante; el elegido va en teal. Solo aparece con dos hijos o mas */
@Composable
fun SelectorHijos(hijos: Hijos, onSeleccion: (String) -> Unit, modifier: Modifier = Modifier) {
    if (hijos.lista.size < 2) return
    Surface(
        shape = CircleShape,
        color = SonrisasTheme.colors.superficieElevada,
        modifier = modifier.sombraSuave(Elevacion.tarjeta, CircleShape),
    ) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(Espacio.space1).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Espacio.space1),
        ) {
            hijos.lista.forEach { n ->
                val activo = n.id == hijos.seleccionado.id
                Row(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(CircleShape)
                        .background(if (activo) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(selected = activo, role = Role.Tab, onClick = { onSeleccion(n.id) })
                        .padding(start = 6.dp, end = Espacio.space4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
                ) {
                    AvatarNino(n.nombre, n.apellidos, hijos.salas[n.salaId]?.estrella ?: ColorEstrella.AZUL, tamano = 32.dp)
                    Text(
                        primerNombre(n.nombre),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (activo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
fun SinHijos(modifier: Modifier = Modifier) {
    EstadoVacio(
        R.drawable.ic_child_care, "No hay niños vinculados a tu cuenta", modifier,
        mensaje = "Si tu hijo o hija está inscrito, pide a la dirección de la estancia que revise tu registro como tutor.",
    )
}

/**
 * patron D (DISENO.md): fondo continuo arriba con el selector de hijo y tarjetas blancas flotantes
 * (el dia y "Ahora mismo", la sala); abajo una hoja blanca con asistencia, comidas, siesta y demas.
 */
@Composable
fun HoyScreen(
    datos: HoyDatos,
    onSeleccion: (String) -> Unit,
    onIncidente: (Incidente) -> Unit,
    onObjetos: (Sala) -> Unit,
    onMaterial: (Sala) -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = datos.hijos.seleccionado
    val r = datos.resumen
    val margen = Modifier.padding(horizontal = Espacio.space4)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Espacio.space1),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        if (datos.hijos.lista.size >= 2) item { SelectorHijos(datos.hijos, onSeleccion, margen) }
        items(r.incidentes.sortedByDescending { it.cuando }, key = { it.id }) { BannerIncidente(it, onIncidente, margen) }
        item { TarjetaDia(datos, margen) }
        datos.hijos.sala?.let { sala ->
            item { TarjetaSalaFamilia(n, sala, datos.objetosNuevos, onObjetos = { onObjetos(sala) }, onMaterial = { onMaterial(sala) }, modifier = margen) }
        }
        item { HojaDelDia(datos, Modifier.padding(top = Espacio.space2)) }
    }
}

/** tarjeta blanca con el avatar, "El dia de ..." y el bloque "Ahora mismo" con el horario desplegable */
@Composable
private fun TarjetaDia(datos: HoyDatos, modifier: Modifier = Modifier) {
    val n = datos.hijos.seleccionado
    var abierto by rememberSaveable { mutableStateOf(false) }
    TarjetaBase(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Espacio.space4), verticalArrangement = Arrangement.spacedBy(Espacio.space4)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                AvatarNino(n.nombre, n.apellidos, datos.hijos.sala?.estrella ?: ColorEstrella.AZUL, tamano = 56.dp)
                Column {
                    Text("El día de ${n.nombre}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(datos.ahora.toLocalDate().formatoLargo(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(Radio.r20), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Espacio.space4), verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                        Icono(R.drawable.ic_schedule, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Ahora mismo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    when (val a = datos.actividad) {
                        is ActividadActual.Especial -> {
                            Text(a.actividad.titulo, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            FlowRowSimple {
                                EstadoChip("Actividad especial", TipoEstado.INFO, R.drawable.ic_event)
                                Text("${a.actividad.inicio.formatoHora()} a ${a.actividad.fin.formatoHora()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        is ActividadActual.Regular -> {
                            Text(a.bloque.actividad, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text("De ${a.bloque.inicio.formatoHora()} a ${a.bloque.fin.formatoHora()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ActividadActual.FueraDeHorario -> {
                            Text("Fuera del horario", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text("La estancia no tiene actividades en este momento.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = { abierto = !abierto }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (abierto) "Ocultar horario de hoy" else "Ver horario de hoy")
                        Icono(if (abierto) R.drawable.ic_expand_less else R.drawable.ic_expand_more, null, Modifier.padding(start = Espacio.space1))
                    }
                    if (abierto) HorarioDelDia(datos.bloques, datos.especiales, datos.actividad)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaSalaFamilia(nino: Nino, sala: Sala, objetosNuevos: Int, onObjetos: () -> Unit, onMaterial: () -> Unit, modifier: Modifier = Modifier) {
    TarjetaSeccion(modifier) {
        EstrellaSala("Sala ${sala.nombre}", sala.estrella, tamano = 24.dp)
        Text("Lo que comparte la sala de ${nino.nombre}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
            BotonAcceso("Material de clase", R.drawable.ic_folder, onMaterial)
            BotonAcceso("Objetos perdidos", R.drawable.ic_checkroom, onObjetos, insignia = objetosNuevos)
        }
    }
}

/** un incidente es una alerta real: va arriba, en rojo apagado suave */
@Composable
private fun BannerIncidente(incidente: Incidente, onVer: (Incidente) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(Radio.r20),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Espacio.space4), verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                Icono(R.drawable.ic_error_fill, null, tint = MaterialTheme.colorScheme.error)
                Text("Incidente a las ${incidente.cuando.formatoHora()}", style = MaterialTheme.typography.titleMedium)
            }
            Text("${incidente.tipo.etiqueta}: ${incidente.descripcion}", style = MaterialTheme.typography.bodyLarge)
            Text("Qué hicimos: ${incidente.accionTomada}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onVer(incidente) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Ver detalle", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowSimple(contenido: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
        verticalArrangement = Arrangement.spacedBy(Espacio.space1),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) { contenido() }
}

@Composable
private fun HorarioDelDia(bloques: List<BloqueHorario>, especiales: List<ActividadEspecial>, actual: ActividadActual) {
    data class Fila(val inicio: LocalTime, val fin: LocalTime, val texto: String, val especial: Boolean, val activa: Boolean)
    val filas = (bloques.map { b ->
        Fila(b.inicio, b.fin, b.actividad, false, (actual as? ActividadActual.Regular)?.bloque?.id == b.id)
    } + especiales.map { e ->
        Fila(e.inicio, e.fin, e.titulo, true, (actual as? ActividadActual.Especial)?.actividad?.id == e.id)
    }).sortedBy { it.inicio }
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
        filas.forEach { f ->
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                Text(
                    f.inicio.formatoHora(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (f.activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        f.texto + if (f.especial) " (actividad especial)" else "",
                        style = if (f.activa) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    )
                    if (f.activa) Text("Ahora", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** hoja blanca de esquinas superiores redondeadas con el resumen del dia */
@Composable
private fun HojaDelDia(datos: HoyDatos, modifier: Modifier = Modifier) {
    val n = datos.hijos.seleccionado
    val r = datos.resumen
    Surface(
        color = SonrisasTheme.colors.superficieElevada,
        shape = RoundedCornerShape(topStart = Radio.encabezado, topEnd = Radio.encabezado),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(start = Espacio.space5, end = Espacio.space5, top = Espacio.space6, bottom = Espacio.space8),
            verticalArrangement = Arrangement.spacedBy(Espacio.space6),
        ) {
            val asistencia = r.asistencia
            if (asistencia == null) {
                EstadoVacio(
                    R.drawable.ic_schedule,
                    "Todavía no registramos la llegada de ${n.nombre} hoy.",
                    mensaje = "Cuando la educadora la registre, aquí verás cómo va su día.",
                )
            } else {
                SeccionAsistencia(n, asistencia)
                SeccionComidas(r)
                SeccionSiesta(r)
                SeccionBano(r)
                SeccionAnimo(r)
                SeccionNotas(r)
            }
        }
    }
}

/** encabezado de cada seccion de la hoja: icono en circulo del color del tipo y titulo en mayusculas */
@Composable
private fun SeccionHoja(titulo: String, @DrawableRes icono: Int, tipo: TipoTono, contenido: @Composable ColumnScope.() -> Unit) {
    val par = tono(tipo)
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.space3)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(par.suave), contentAlignment = Alignment.Center) {
                Icono(icono, null, Modifier.size(18.dp), tint = par.texto)
            }
            Text(
                titulo.uppercase(LocaleMx),
                style = EstiloEtiquetaSeccion,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading(); contentDescription = titulo },
            )
        }
        contenido()
    }
}

@Composable
private fun SeccionAsistencia(n: Nino, a: Asistencia) {
    SeccionHoja("Asistencia", R.drawable.ic_login, TipoTono.LLEGADA_SALIDA) {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            val entrega = n.persona(a.entregadoPorId)
            DatoHora(
                "Llegada", a.horaEntrada.formatoHora(),
                entrega?.let { "con ${it.parentesco} (${primerNombre(it.nombre)})" },
                R.drawable.ic_check, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary,
                Modifier.weight(1f),
            )
            val salida = a.horaSalida
            if (salida != null) {
                val recoge = n.persona(a.recogidoPorId)
                DatoHora(
                    "Salida", salida.formatoHora(),
                    recoge?.let { "con ${it.parentesco} (${primerNombre(it.nombre)})" },
                    R.drawable.ic_logout, SonrisasTheme.colors.salio.suave, SonrisasTheme.colors.salio.texto,
                    Modifier.weight(1f),
                )
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    PildoraEstado("En la estancia", EstadoPildora.PRESENTE)
                }
            }
        }
        a.observacionesSalida?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** "Llegada 8:05 / con Mama (Laura)" con su icono en circulo */
@Composable
private fun DatoHora(etiqueta: String, hora: String, quien: String?, @DrawableRes icono: Int, fondo: Color, frente: Color, modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(fondo), contentAlignment = Alignment.Center) {
            Icono(icono, null, Modifier.size(20.dp), tint = frente)
        }
        Column {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                Text(etiqueta, style = MaterialTheme.typography.bodyLarge)
                Text(hora, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
            quien?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String, apagado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            valor,
            style = if (apagado) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
            color = if (apagado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** tres mosaicos (desayuno, colacion, comida) en naranja suave con la porcion en pildora */
@Composable
private fun SeccionComidas(r: ResumenDia) {
    val par = tono(TipoTono.ALIMENTO)
    SeccionHoja("Comidas", R.drawable.ic_restaurant, TipoTono.ALIMENTO) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
            TiempoComida.entries.forEach { t ->
                val p = r.comidas[t]
                Surface(color = par.suave, shape = RoundedCornerShape(Radio.r16), modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(
                        Modifier.padding(vertical = Espacio.space3, horizontal = Espacio.space1).semantics(mergeDescendants = true) {},
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Espacio.space2),
                    ) {
                        Icono(R.drawable.ic_restaurant, null, Modifier.size(22.dp), tint = par.texto)
                        Text(t.etiqueta, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                        if (p != null) {
                            Pildora(p.etiqueta, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Pildora("Sin registro", SonrisasTheme.colors.superficieElevada, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionSiesta(r: ResumenDia) {
    SeccionHoja("Siesta", R.drawable.ic_bedtime, TipoTono.SIESTA) {
        if (r.siestas.isEmpty()) {
            Text("Sin registro", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            if (r.minutosSiesta > 0) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                    Text(minutosTexto(r.minutosSiesta), style = MaterialTheme.typography.headlineSmall)
                    Text("Durmió ${minutosTexto(r.minutosSiesta)} en total", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            r.siestas.forEach { s ->
                val texto = s.fin?.let { "De ${s.inicio.formatoHora()} a ${it.formatoHora()}" }
                    ?: "Duerme desde las ${s.inicio.formatoHora()}"
                Text(texto, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SeccionBano(r: ResumenDia) {
    SeccionHoja("Baño", R.drawable.ic_wc, TipoTono.PANAL) {
        if (r.pipi == 0 && r.popo == 0) {
            Text("Sin registro", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FilaDato("Pipí", veces(r.pipi))
            FilaDato("Popó", veces(r.popo))
        }
    }
}

private fun veces(n: Int) = if (n == 1) "1 vez" else "$n veces"

@Composable
private fun SeccionAnimo(r: ResumenDia) {
    SeccionHoja("Ánimo", R.drawable.ic_mood, TipoTono.ANIMO) {
        val a = r.ultimoAnimo
        if (a == null) {
            Text("Sin registro", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                Icono(a.estado.icono, null, Modifier.size(32.dp), tint = tono(TipoTono.ANIMO).texto)
                Text(a.estado.etiqueta, style = MaterialTheme.typography.titleLarge)
            }
            Text(a.detallesAnimo(), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SeccionNotas(r: ResumenDia) {
    SeccionHoja("Notas de la educadora", R.drawable.ic_edit_note, TipoTono.NOTA) {
        if (r.notas.isEmpty()) {
            Text("Sin notas por ahora", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        r.notas.forEachIndexed { i, nota ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(nota.texto, style = MaterialTheme.typography.bodyLarge)
            Text(nota.cuando.formatoHora(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SemanaScreen(
    datos: SemanaDatos,
    onSeleccion: (String) -> Unit,
    onCompartir: () -> Unit,
    onImprimir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                BotonPrincipal("Compartir resumen", onCompartir, Modifier.padding(Espacio.space4))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Espacio.space4),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            item { SelectorHijos(datos.hijos, onSeleccion) }
            item {
                Column {
                    Text("La semana de ${datos.hijos.seleccionado.nombre}", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Del ${datos.semana.lunes.formatoDiaMes()} al ${datos.semana.lunes.plusDays(4).formatoDiaMes()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onImprimir, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icono(R.drawable.ic_print, null)
                        Text("Imprimir bitácora", modifier = Modifier.padding(start = Espacio.space2))
                    }
                }
            }
            items(datos.semana.dias, key = { it.fecha.toString() }) { dia -> FilaDia(dia, datos.hoy) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaDia(dia: DiaSemana, hoy: LocalDate) {
    val titulo = dia.fecha.nombreDia() + (if (dia.fecha == hoy) " (hoy)" else "")
    TarjetaSeccion {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(dia.fecha.formatoDiaMes(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when (dia) {
                is DiaSemana.Pendiente -> EstadoChip("Pendiente", TipoEstado.NEUTRO, R.drawable.ic_schedule)
                is DiaSemana.NoAsistio -> EstadoChip("No asistió", TipoEstado.NEUTRO, R.drawable.ic_event_busy)
                is DiaSemana.Asistio -> dia.resumen.ultimoAnimo?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                        Icono(it.estado.icono, null)
                        Text(it.estado.etiqueta, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        if (dia is DiaSemana.Asistio) {
            val r = dia.resumen
            val a = r.asistencia!!
            Text(
                "Asistió de ${a.horaEntrada.formatoHora()}" + (a.horaSalida?.let { " a ${it.formatoHora()}" } ?: " (sigue en la estancia)"),
                style = MaterialTheme.typography.bodyLarge,
            )
            val comidas = TiempoComida.entries.filter { it in r.comidas }.joinToString(", ") {
                "${it.etiqueta.lowercase()} ${r.comidas.getValue(it).etiqueta.lowercase()}"
            }
            if (comidas.isNotEmpty()) FilaIcono(R.drawable.ic_restaurant, comidas.replaceFirstChar { it.uppercase() })
            if (r.minutosSiesta > 0) FilaIcono(R.drawable.ic_bedtime, "Siesta de ${minutosTexto(r.minutosSiesta)}")
            if (r.incidentes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                    r.incidentes.forEach { EstadoChip("Incidente: ${it.tipo.etiqueta.lowercase()}", TipoEstado.ERROR, R.drawable.ic_error_fill) }
                }
            }
        }
    }
}

@Composable
private fun FilaIcono(@DrawableRes icono: Int, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalAlignment = Alignment.CenterVertically) {
        Icono(icono, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun IncidenteDetalleScreen(datos: IncidenteDetalleDatos?, modifier: Modifier = Modifier) {
    if (datos == null) {
        EstadoVacio(R.drawable.ic_healing, "Este incidente no está disponible", modifier)
        return
    }
    val i = datos.incidente
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        EstadoChip("Incidente ${i.folio}", TipoEstado.ERROR, R.drawable.ic_error_fill)
        Text(i.tipo.etiqueta, style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            AvatarNino(datos.nino.nombre, datos.nino.apellidos, datos.sala?.estrella ?: ColorEstrella.AZUL, tamano = 40.dp)
            Column {
                Text(datos.nino.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                Text("${i.cuando.toLocalDate().formatoLargo()}, ${i.cuando.formatoHora()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TarjetaSeccion(titulo = "Qué pasó") {
            Text(i.descripcion, style = MaterialTheme.typography.bodyLarge)
            i.parteDelCuerpo?.let { Text("Parte del cuerpo: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        TarjetaSeccion(titulo = "Qué hicimos") {
            Text(i.accionTomada, style = MaterialTheme.typography.bodyLarge)
        }
        datos.registradoPor?.let {
            Text("Registró: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val salaEjemplo = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
private val mama = PersonaAutorizada("p1", "Laura Treviño Garza", "Mamá", "81 2345 6789")
private val sofia = Nino("1", "Sofía", "Ramírez Treviño", LocalDate.of(2024, 4, 18), "e", "s", listOf("p1"), listOf(mama), alergias = "Cacahuate")
private val mateo = Nino("2", "Mateo", "Ramírez Treviño", LocalDate.of(2025, 10, 27), "e", "s", listOf("p1"), listOf(mama))
private val hoyEjemplo = LocalDate.of(2026, 9, 30)
private val incidenteEjemplo = Incidente(
    "i", "INC-0003", "1", LocalDateTime.of(hoyEjemplo, LocalTime.of(10, 40)), TipoIncidente.GOLPE, "Rodilla derecha",
    "Golpe leve en la rodilla al jugar en el patio", "Se limpió la zona y se aplicó compresa fría.", "u",
)

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun HoyPreview() {
    val asistencia = Asistencia("a", "1", hoyEjemplo, LocalTime.of(8, 5), "p1", "u")
    val eventos = listOf(
        EventoBitacora.Alimento("e1", "1", LocalDateTime.of(hoyEjemplo, LocalTime.of(8, 45)), "u", TiempoComida.DESAYUNO, Porcion.TODO),
        EventoBitacora.Animo("e2", "1", LocalDateTime.of(hoyEjemplo, LocalTime.of(11, 40)), "u", EstadoAnimo.FELIZ, false, false, true),
    )
    val bloque = BloqueHorario("b", "s", LocalTime.of(10, 30), LocalTime.of(11, 15), "Juego en el patio")
    val datos = HoyDatos(
        Hijos(listOf(sofia, mateo), sofia, mapOf("s" to salaEjemplo)),
        ResumenDia.de(asistencia, eventos, listOf(incidenteEjemplo)),
        ActividadActual.Regular(bloque), listOf(bloque), emptyList(), LocalDateTime.of(hoyEjemplo, LocalTime.of(10, 50)),
    )
    SonrisasTheme { HoyScreen(datos, {}, {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SemanaPreview() {
    val asistencia = Asistencia("a", "1", hoyEjemplo.minusDays(2), LocalTime.of(8, 5), "p1", "u", horaSalida = LocalTime.of(15, 10))
    val semana = ResumenSemana.de("Sofía", hoyEjemplo, listOf(asistencia), emptyList(), emptyList())
    SonrisasTheme { SemanaScreen(SemanaDatos(Hijos(listOf(sofia), sofia, mapOf("s" to salaEjemplo)), semana, hoyEjemplo), {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun IncidenteDetallePreview() {
    SonrisasTheme { IncidenteDetalleScreen(IncidenteDetalleDatos(incidenteEjemplo, sofia, salaEjemplo, "Mariana López Hernández")) }
}
