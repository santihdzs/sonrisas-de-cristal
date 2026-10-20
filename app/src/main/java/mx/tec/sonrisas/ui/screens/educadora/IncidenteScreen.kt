package mx.tec.sonrisas.ui.screens.educadora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.BannerAlergias
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.CampoHora
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.EtiquetaSeccion
import mx.tec.sonrisas.ui.components.FormaAvatar
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.SelectorOpciones
import mx.tec.sonrisas.ui.components.TarjetaBase
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun IncidenteScreen(
    nino: Nino,
    estrella: ColorEstrella,
    form: IncidenteForm,
    onCambiar: (IncidenteForm) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errores = if (form.intentoGuardar) form.errores else emptyMap()
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        TarjetaBase(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(Espacio.space4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
            ) {
                AvatarNino(nino.nombre, nino.apellidos, estrella, forma = FormaAvatar.CUADRADO)
                Text(nino.nombreCompleto, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
        if (nino.tieneInfoMedica) BannerAlergias(nino.alergias, nino.padecimientos, nino.medicamentos)
        EtiquetaSeccion("¿Qué pasó?")
        TarjetaSeccion {
            SelectorOpciones(TipoIncidente.entries, form.tipo, { onCambiar(form.copy(tipo = it)) }, { it.etiqueta })
            errores["tipo"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            CampoHora("Hora del incidente", form.hora, { onCambiar(form.copy(hora = it)) })
        }
        EtiquetaSeccion("Detalles")
        TarjetaSeccion {
            CampoTexto(
                valor = form.parteDelCuerpo,
                onCambio = { onCambiar(form.copy(parteDelCuerpo = it)) },
                etiqueta = "Parte del cuerpo (opcional)",
                error = null,
            )
            CampoTexto(
                valor = form.descripcion,
                onCambio = { onCambiar(form.copy(descripcion = it)) },
                etiqueta = "Descripción",
                error = errores["descripcion"],
                ayuda = "Cuenta qué pasó y cómo está ahora.",
                lineas = 3,
            )
            CampoTexto(
                valor = form.accionTomada,
                onCambio = { onCambiar(form.copy(accionTomada = it)) },
                etiqueta = "Acción tomada",
                error = errores["accion"],
                ayuda = "Por ejemplo: se aplicó compresa fría.",
                lineas = 2,
            )
        }
        // aviso informativo en teal suave (no es una alerta)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(Espacio.space4), horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                Icono(R.drawable.ic_notifications, null)
                Text(
                    "La familia lo verá de inmediato en su pantalla de hoy, sin esperar a la hora de salida.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        BotonPrincipal("Registrar y avisar a la familia", onGuardar, guardando = form.guardando, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun IncidentePreview() {
    val nino = Nino("1", "Sofía", "Ramírez Treviño", LocalDate.of(2024, 4, 18), "e", "s", emptyList(), emptyList(), alergias = "Cacahuate")
    SonrisasTheme {
        IncidenteScreen(nino, ColorEstrella.ROJO, IncidenteForm(tipo = TipoIncidente.GOLPE, hora = LocalTime.of(10, 40), intentoGuardar = true), {}, {})
    }
}
