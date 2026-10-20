package mx.tec.sonrisas.ui.screens.educadora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.PersonaAutorizada
import mx.tec.sonrisas.domain.SiNoNoAplica
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.CampoHora
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.EtiquetaCampo
import mx.tec.sonrisas.ui.components.FilaSwitch
import mx.tec.sonrisas.ui.components.HojaModal
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.SelectorOpciones
import mx.tec.sonrisas.ui.components.SelectorPersona
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.components.formatoHora
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun HojasAsistencia(
    hoja: HojaAsistencia?,
    onCambiarLlegada: (LlegadaForm) -> Unit,
    onCambiarSalida: (SalidaForm) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
) {
    when (hoja) {
        is HojaAsistencia.Llegada -> HojaLlegada(hoja.nino, hoja.form, onCambiarLlegada, onGuardar, onCerrar)
        is HojaAsistencia.Salida -> HojaSalida(hoja.nino, hoja.incidentes, hoja.form, onCambiarSalida, onGuardar, onCerrar)
        null -> Unit
    }
}

@Composable
private fun HojaLlegada(
    nino: Nino,
    form: LlegadaForm,
    onCambio: (LlegadaForm) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
) {
    HojaModal(
        titulo = "Registrar llegada",
        subtitulo = nino.nombreCompleto,
        onCerrar = onCerrar,
        boton = { BotonPrincipal("Registrar llegada", onGuardar, habilitado = form.puedeGuardar, guardando = form.guardando) },
    ) {
        CampoHora("Hora de llegada", form.hora, { onCambio(form.copy(hora = it)) })
        EtiquetaCampo("¿Quién lo entrega?")
        SelectorPersona(nino.personasAutorizadas, form.entregadoPorId, { onCambio(form.copy(entregadoPorId = it)) })
        if (form.entregadoPorId == null) {
            Text("Elige a una de las personas autorizadas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilaSwitch("Llegó limpio", form.llegoLimpio, { onCambio(form.copy(llegoLimpio = it)) })
        FilaSwitch("Trajo su mochila completa", form.mochilaCompleta, { onCambio(form.copy(mochilaCompleta = it)) })
        FilaSwitch("Llegó en buen estado físico", form.buenEstado, { onCambio(form.copy(buenEstado = it)) })
        if (!form.buenEstado) {
            CampoTexto(
                valor = form.lesion,
                onCambio = { onCambio(form.copy(lesion = it)) },
                etiqueta = "¿Qué lesión y dónde?",
                error = if (form.faltaLesion) "Describe la lesión y la parte del cuerpo." else null,
                ayuda = "Por ejemplo: rasguño en la mejilla izquierda.",
            )
        }
        FilaSwitch("¿Llega con síntomas?", form.conSintomas, { onCambio(form.copy(conSintomas = it)) })
        if (form.conSintomas) {
            CampoTexto(
                valor = form.sintomas,
                onCambio = { onCambio(form.copy(sintomas = it)) },
                etiqueta = "¿Cuáles?",
                error = if (form.faltanSintomas) "Escribe los síntomas." else null,
            )
            EtiquetaCampo("¿Dejaron medicamento con receta?")
            SelectorOpciones(SiNoNoAplica.entries, form.medicamento, { onCambio(form.copy(medicamento = it)) }, { it.etiqueta })
        }
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun HojaSalida(
    nino: Nino,
    incidentes: List<Incidente>,
    form: SalidaForm,
    onCambio: (SalidaForm) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
) {
    HojaModal(
        titulo = "Registrar salida",
        subtitulo = nino.nombreCompleto,
        onCerrar = onCerrar,
        boton = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                if (incidentes.isNotEmpty()) RecordatorioIncidentes(incidentes)
                BotonPrincipal("Registrar salida", onGuardar, habilitado = form.puedeGuardar, guardando = form.guardando)
            }
        },
    ) {
        CampoHora("Hora de salida", form.hora, { onCambio(form.copy(hora = it)) })
        EtiquetaCampo("¿Quién lo recoge?")
        SelectorPersona(nino.personasAutorizadas, form.recogidoPorId, { onCambio(form.copy(recogidoPorId = it)) })
        if (form.recogidoPorId == null) {
            Text(
                "Solo pueden recogerlo las personas autorizadas en su expediente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilaSwitch("Salió limpio", form.salioLimpio, { onCambio(form.copy(salioLimpio = it)) })
        FilaSwitch("Lleva sus pertenencias completas", form.pertenenciasCompletas, { onCambio(form.copy(pertenenciasCompletas = it)) })
        FilaSwitch("Sale en buen estado físico", form.buenEstado, { onCambio(form.copy(buenEstado = it)) })
        if (!form.buenEstado) {
            CampoTexto(
                valor = form.lesion,
                onCambio = { onCambio(form.copy(lesion = it)) },
                etiqueta = "¿Qué lesión y dónde?",
                error = if (form.faltaLesion) "Describe la lesión y la parte del cuerpo." else null,
            )
        }
        CampoTexto(
            valor = form.observaciones,
            onCambio = { onCambio(form.copy(observaciones = it)) },
            etiqueta = "Observaciones (opcional)",
            error = null,
            lineas = 2,
        )
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun RecordatorioIncidentes(incidentes: List<Incidente>) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Espacio.space4), horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            Icono(R.drawable.ic_error_fill, null, tint = MaterialTheme.colorScheme.error)
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                Text("Coméntalo con quien lo recoge", style = MaterialTheme.typography.titleSmall)
                incidentes.forEach {
                    Text("${it.cuando.formatoHora()} · ${it.tipo.etiqueta}: ${it.descripcion}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview
@Composable
private fun HojaSalidaPreview() {
    val mama = PersonaAutorizada("p1", "Laura Treviño Garza", "Mamá", "81 2345 6789")
    val papa = PersonaAutorizada("p2", "Jorge Ramírez Salinas", "Papá", "81 8765 4321")
    val nino = Nino("1", "Sofía", "Ramírez Treviño", LocalDate.of(2024, 4, 18), "e", "s", listOf("p1", "p2"), listOf(mama, papa))
    val incidente = Incidente("i", "INC-0003", "1", LocalDateTime.of(2026, 9, 30, 10, 40), TipoIncidente.GOLPE, "Rodilla", "Golpe leve en la rodilla", "Compresa fría", "e")
    SonrisasTheme {
        Column {
            RecordatorioIncidentes(listOf(incidente))
            SelectorPersona(nino.personasAutorizadas, "p2", {})
        }
    }
}
