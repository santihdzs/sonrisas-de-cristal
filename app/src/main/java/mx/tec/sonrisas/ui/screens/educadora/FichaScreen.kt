package mx.tec.sonrisas.ui.screens.educadora

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.ControlEsfinteres
import mx.tec.sonrisas.domain.EstadoAnimo
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.PersonaAutorizada
import mx.tec.sonrisas.domain.Porcion
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TiempoComida
import mx.tec.sonrisas.domain.TipoEvacuacion
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.BannerAlergias
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.CampoHora
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.EtiquetaCampo
import mx.tec.sonrisas.ui.components.FilaCheck
import mx.tec.sonrisas.ui.components.HojaModal
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.SelectorOpciones
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.detalle
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.components.formatoHora
import mx.tec.sonrisas.ui.components.icono
import mx.tec.sonrisas.ui.components.minutosTexto
import mx.tec.sonrisas.ui.components.titulo
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun FichaScreen(
    datos: FichaDatos,
    form: FichaFormState,
    ahora: LocalTime,
    onAbrir: (HojaRapida) -> Unit,
    onCambiar: (FichaFormState) -> Unit,
    onCerrarHoja: () -> Unit,
    onGuardar: () -> Unit,
    onEmpezarSiesta: () -> Unit,
    onTerminarSiesta: (String) -> Unit,
    onBorrar: (String) -> Unit,
    onIncidente: () -> Unit,
    onLlegada: () -> Unit,
    onSalida: (Asistencia) -> Unit,
    onImprimir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var aBorrar by remember { mutableStateOf<String?>(null) }
    val presente = datos.asistencia != null && !datos.asistencia.salio
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { AccionesRapidas(habilitadas = presente, onAbrir = onAbrir, onIncidente = onIncidente) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Espacio.space4),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            item { Column { EncabezadoFicha(datos, onLlegada, onSalida, onImprimir) } }
            if (datos.nino.tieneInfoMedica) {
                item { BannerAlergias(datos.nino.alergias, datos.nino.padecimientos, datos.nino.medicamentos) }
            }
            item { TituloSeccion("Hoy") }
            if (datos.linea.isEmpty()) {
                item {
                    EstadoVacio(
                        R.drawable.ic_edit_note, "Sin registros todavía",
                        mensaje = if (datos.asistencia == null) "Registra la llegada para empezar su bitácora del día." else "Usa los botones de abajo para anotar su día.",
                    )
                }
            }
            items(datos.linea, key = { it.id }) { item ->
                FilaLinea(item, datos, ahora, onBorrar = { aBorrar = it })
            }
        }
    }

    HojasRapidas(datos, form, ahora, onCambiar, onCerrarHoja, onGuardar, onEmpezarSiesta, onTerminarSiesta)

    aBorrar?.let { id ->
        AlertDialog(
            onDismissRequest = { aBorrar = null },
            title = { Text("¿Borrar este registro?") },
            text = { Text("Se quitará de la bitácora de hoy y la familia ya no lo verá.") },
            confirmButton = {
                TextButton(onClick = {
                    onBorrar(id)
                    aBorrar = null
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { aBorrar = null }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EncabezadoFicha(datos: FichaDatos, onLlegada: () -> Unit, onSalida: (Asistencia) -> Unit, onImprimir: () -> Unit) {
    val n = datos.nino
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space3), verticalAlignment = Alignment.CenterVertically) {
        AvatarNino(n.nombre, n.apellidos, datos.sala.estrella, tamano = 64.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
            Text(n.nombreCompleto, style = MaterialTheme.typography.headlineSmall)
            EstrellaSala("Sala ${datos.sala.nombre}", datos.sala.estrella, tamano = 24.dp)
        }
    }
    FlowRow(
        modifier = Modifier.padding(top = Espacio.space3),
        horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
        verticalArrangement = Arrangement.spacedBy(Espacio.space2),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        ChipAsistencia(datos.asistencia)
        val a = datos.asistencia
        if (a == null) {
            FilledTonalButton(onClick = onLlegada, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_login, null)
                Text("Registrar llegada", modifier = Modifier.padding(start = Espacio.space2))
            }
        } else if (!a.salio) {
            FilledTonalButton(onClick = { onSalida(a) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_logout, null)
                Text("Registrar salida", modifier = Modifier.padding(start = Espacio.space2))
            }
        }
        TextButton(onClick = onImprimir, modifier = Modifier.heightIn(min = 48.dp)) {
            Icono(R.drawable.ic_print, null)
            Text("Imprimir bitácora", modifier = Modifier.padding(start = Espacio.space2))
        }
    }
}

@Composable
private fun FilaLinea(item: ItemLinea, datos: FichaDatos, ahora: LocalTime, onBorrar: (String) -> Unit) {
    val esquema = MaterialTheme.colorScheme
    val (icono, titulo, detalle) = when (item) {
        is ItemLinea.Evento -> {
            val e = item.evento
            val t = if (e is EventoBitacora.Siesta && e.fin == null) {
                "Durmiendo desde las ${e.inicio.formatoHora()} (${minutosTexto(Duration.between(e.inicio, ahora).toMinutes().toInt().coerceAtLeast(0))})"
            } else e.titulo()
            Triple(e.icono, t, e.detalle())
        }
        is ItemLinea.Llegada -> Triple(
            R.drawable.ic_login, "Llegó",
            datos.nino.persona(item.asistencia.entregadoPorId)?.let { "Entregó ${it.nombre} (${it.parentesco.lowercase()})" },
        )
        is ItemLinea.Salida -> Triple(
            R.drawable.ic_logout, "Salió",
            datos.nino.persona(item.asistencia.recogidoPorId)?.let { "Recogió ${it.nombre} (${it.parentesco.lowercase()})" },
        )
        is ItemLinea.ConIncidente -> Triple(
            R.drawable.ic_healing, "Incidente ${item.incidente.folio}: ${item.incidente.tipo.etiqueta.lowercase()}",
            item.incidente.descripcion,
        )
    }
    val quien = when (item) {
        is ItemLinea.Evento -> datos.nombres[item.evento.registradoPorId]
        is ItemLinea.Llegada -> datos.nombres[item.asistencia.recibidoPorId]
        is ItemLinea.Salida -> datos.nombres[item.asistencia.recibidoPorId]
        is ItemLinea.ConIncidente -> datos.nombres[item.incidente.registradoPorId]
    }
    val esIncidente = item is ItemLinea.ConIncidente
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (esIncidente) esquema.errorContainer else esquema.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icono(icono, null, tint = if (esIncidente) esquema.onErrorContainer else esquema.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(top = Espacio.space1)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodyMedium, color = esquema.onSurfaceVariant)
            Text(
                listOfNotNull(item.cuando.formatoHora(), quien).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = esquema.onSurfaceVariant,
            )
        }
        if (item is ItemLinea.Evento) {
            IconButton(onClick = { onBorrar(item.evento.id) }) {
                Icono(R.drawable.ic_delete, "Borrar registro", tint = esquema.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AccionesRapidas(habilitadas: Boolean, onAbrir: (HojaRapida) -> Unit, onIncidente: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier.fillMaxWidth().padding(Espacio.space3),
            verticalArrangement = Arrangement.spacedBy(Espacio.space2),
        ) {
            if (!habilitadas) {
                Text(
                    "Registra la llegada para anotar comidas, siesta y más.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                BotonRapido("Comida", R.drawable.ic_restaurant, habilitadas, Modifier.weight(1f)) { onAbrir(HojaRapida.COMIDA) }
                BotonRapido("Siesta", R.drawable.ic_bedtime, habilitadas, Modifier.weight(1f)) { onAbrir(HojaRapida.SIESTA) }
                BotonRapido("Baño", R.drawable.ic_wc, habilitadas, Modifier.weight(1f)) { onAbrir(HojaRapida.BANO) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                BotonRapido("Ánimo", R.drawable.ic_mood, habilitadas, Modifier.weight(1f)) { onAbrir(HojaRapida.ANIMO) }
                BotonRapido("Nota", R.drawable.ic_edit_note, habilitadas, Modifier.weight(1f)) { onAbrir(HojaRapida.NOTA) }
                BotonRapido("Incidente", R.drawable.ic_healing, true, Modifier.weight(1f), acento = true, onClick = onIncidente)
            }
        }
    }
}

@Composable
private fun BotonRapido(
    texto: String,
    @DrawableRes icono: Int,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    acento: Boolean = false,
    onClick: () -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val frente = when {
        !habilitado -> esquema.onSurface.copy(alpha = 0.38f)
        acento -> esquema.onTertiaryContainer
        else -> esquema.onSurface
    }
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = MaterialTheme.shapes.medium,
        color = if (acento) esquema.tertiaryContainer else SonrisasTheme.colors.superficieElevada,
        contentColor = frente,
        modifier = modifier.heightIn(min = 64.dp),
    ) {
        Column(
            Modifier.padding(vertical = Espacio.space2, horizontal = Espacio.space1),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacio.space1, Alignment.CenterVertically),
        ) {
            Icono(icono, null)
            Text(texto, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

private val oraciones = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

@Composable
private fun HojasRapidas(
    datos: FichaDatos,
    form: FichaFormState,
    ahora: LocalTime,
    onCambiar: (FichaFormState) -> Unit,
    onCerrar: () -> Unit,
    onGuardar: () -> Unit,
    onEmpezarSiesta: () -> Unit,
    onTerminarSiesta: (String) -> Unit,
) {
    val nombre = datos.nino.nombreCompleto
    val error: @Composable () -> Unit = {
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
    when (form.hoja) {
        HojaRapida.COMIDA -> HojaModal(
            "Comida", onCerrar, subtitulo = nombre,
            boton = { BotonPrincipal("Guardar comida", onGuardar, habilitado = form.puedeGuardar, guardando = form.guardando) },
        ) {
            val c = form.comida
            CampoHora("Hora", c.hora, { onCambiar(form.copy(comida = c.copy(hora = it))) })
            EtiquetaCampo("Tiempo")
            SelectorOpciones(TiempoComida.entries, c.tiempo, { onCambiar(form.copy(comida = c.copy(tiempo = it))) }, { it.etiqueta })
            EtiquetaCampo("¿Cuánto comió?")
            SelectorOpciones(Porcion.entries, c.porcion, { onCambiar(form.copy(comida = c.copy(porcion = it))) }, { it.etiqueta })
            error()
        }
        HojaRapida.SIESTA -> HojaModal("Siesta", onCerrar, subtitulo = nombre) {
            val s = form.siesta
            val abierta = datos.siestaAbierta
            if (!s.manual) {
                if (abierta != null) {
                    val min = Duration.between(abierta.inicio, ahora).toMinutes().toInt().coerceAtLeast(0)
                    Text("Duerme desde las ${abierta.inicio.formatoHora()}", style = MaterialTheme.typography.titleLarge)
                    Text("Lleva ${minutosTexto(min)} dormido.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BotonPrincipal("Despertó", { onTerminarSiesta(abierta.id) }, guardando = form.guardando)
                } else {
                    Text("Toca el botón cuando se duerma. Después registras cuándo despertó.", style = MaterialTheme.typography.bodyLarge)
                    BotonPrincipal("Empezó a dormir", onEmpezarSiesta, guardando = form.guardando)
                }
                TextButton(onClick = { onCambiar(form.copy(siesta = s.copy(manual = true))) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Registrar horas a mano")
                }
            } else {
                CampoHora("Se durmió", s.inicio, { onCambiar(form.copy(siesta = s.copy(inicio = it))) })
                CampoHora("Despertó", s.fin, { onCambiar(form.copy(siesta = s.copy(fin = it))) })
                if (!s.fin.isAfter(s.inicio)) {
                    Text("La hora en que despertó debe ser después de la hora en que se durmió.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                BotonPrincipal("Guardar siesta", onGuardar, habilitado = form.puedeGuardar, guardando = form.guardando)
                TextButton(onClick = { onCambiar(form.copy(siesta = s.copy(manual = false))) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Usar la hora actual")
                }
            }
            error()
        }
        HojaRapida.BANO -> HojaModal(
            "Baño", onCerrar, subtitulo = nombre,
            boton = { BotonPrincipal("Guardar baño", onGuardar, habilitado = form.puedeGuardar, guardando = form.guardando) },
        ) {
            val b = form.bano
            CampoHora("Hora", b.hora, { onCambiar(form.copy(bano = b.copy(hora = it))) })
            EtiquetaCampo("¿Qué hizo?")
            SelectorOpciones(TipoEvacuacion.entries, b.tipo, { onCambiar(form.copy(bano = b.copy(tipo = it))) }, { it.etiqueta })
            EtiquetaCampo("¿Avisó?")
            SelectorOpciones(ControlEsfinteres.entries, b.control, { onCambiar(form.copy(bano = b.copy(control = it))) }, { it.etiqueta })
            error()
        }
        HojaRapida.ANIMO -> HojaModal(
            "Ánimo", onCerrar, subtitulo = nombre,
            boton = { BotonPrincipal("Guardar ánimo", onGuardar, habilitado = form.puedeGuardar, guardando = form.guardando) },
        ) {
            val a = form.animo
            CampoHora("Hora", a.hora, { onCambiar(form.copy(animo = a.copy(hora = it))) })
            SelectorOpciones(
                EstadoAnimo.entries, a.estado, { onCambiar(form.copy(animo = a.copy(estado = it))) }, { it.etiqueta },
                icono = { it.icono }, grande = true,
            )
            FilaCheck("Lloró", a.lloro, { onCambiar(form.copy(animo = a.copy(lloro = it))) })
            FilaCheck("Peleó", a.peleo, { onCambiar(form.copy(animo = a.copy(peleo = it))) })
            FilaCheck("Participó en actividades", a.participo, { onCambiar(form.copy(animo = a.copy(participo = it))) })
            error()
        }
        HojaRapida.NOTA -> HojaModal(
            "Nota", onCerrar, subtitulo = nombre,
            boton = { BotonPrincipal("Guardar nota", onGuardar, habilitado = form.puedeGuardar, guardando = form.guardando) },
        ) {
            val n = form.nota
            OutlinedTextField(
                value = n.texto,
                onValueChange = { onCambiar(form.copy(nota = n.copy(texto = it))) },
                label = { Text("Observaciones para la familia") },
                supportingText = { Text("La familia la verá en su resumen del día.") },
                keyboardOptions = oraciones,
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            error()
        }
        null -> Unit
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun FichaPreview() {
    val hoy = LocalDate.of(2026, 9, 30)
    val sala = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
    val mama = PersonaAutorizada("p1", "Laura Treviño Garza", "Mamá", "81 2345 6789")
    val nino = Nino("1", "Sofía", "Ramírez Treviño", hoy.minusMonths(29), "e", "s", listOf("p1"), listOf(mama), alergias = "Cacahuate")
    val asistencia = Asistencia("a", "1", hoy, LocalTime.of(8, 5), "p1", "u")
    val eventos = listOf(
        EventoBitacora.Alimento("e1", "1", LocalDateTime.of(hoy, LocalTime.of(8, 45)), "u", TiempoComida.DESAYUNO, Porcion.TODO),
        EventoBitacora.Animo("e2", "1", LocalDateTime.of(hoy, LocalTime.of(11, 40)), "u", EstadoAnimo.FELIZ, false, false, true),
    )
    val datos = FichaDatos(
        nino, sala, asistencia,
        (listOf(ItemLinea.Llegada(asistencia)) + eventos.map { ItemLinea.Evento(it) }).sortedByDescending { it.cuando },
        null, mapOf("u" to "Mariana López Hernández"), hoy,
    )
    val h = LocalTime.of(12, 0)
    val form = FichaFormState(
        comida = ComidaForm(h, TiempoComida.COMIDA), siesta = SiestaForm(h, h), bano = BanoForm(h),
        animo = AnimoForm(h), nota = NotaForm(h),
    )
    SonrisasTheme {
        FichaScreen(datos, form, h, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
