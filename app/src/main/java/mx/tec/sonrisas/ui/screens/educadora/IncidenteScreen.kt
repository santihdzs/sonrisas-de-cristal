package mx.tec.sonrisas.ui.screens.educadora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import mx.tec.sonrisas.ui.components.EtiquetaCampo
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.SelectorOpciones
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalTime

private val oraciones = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            AvatarNino(nino.nombre, nino.apellidos, estrella)
            Text(nino.nombreCompleto, style = MaterialTheme.typography.titleLarge)
        }
        if (nino.tieneInfoMedica) BannerAlergias(nino.alergias, nino.padecimientos, nino.medicamentos)
        EtiquetaCampo("¿Qué pasó?")
        SelectorOpciones(TipoIncidente.entries, form.tipo, { onCambiar(form.copy(tipo = it)) }, { it.etiqueta })
        errores["tipo"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        CampoHora("Hora del incidente", form.hora, { onCambiar(form.copy(hora = it)) })
        OutlinedTextField(
            value = form.parteDelCuerpo,
            onValueChange = { onCambiar(form.copy(parteDelCuerpo = it)) },
            label = { Text("Parte del cuerpo (opcional)") },
            singleLine = true,
            keyboardOptions = oraciones,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.descripcion,
            onValueChange = { onCambiar(form.copy(descripcion = it)) },
            label = { Text("Descripción") },
            isError = errores["descripcion"] != null,
            supportingText = { Text(errores["descripcion"] ?: "Cuenta qué pasó y cómo está ahora.") },
            keyboardOptions = oraciones,
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.accionTomada,
            onValueChange = { onCambiar(form.copy(accionTomada = it)) },
            label = { Text("Acción tomada") },
            isError = errores["accion"] != null,
            supportingText = { Text(errores["accion"] ?: "Por ejemplo: se aplicó compresa fría.") },
            keyboardOptions = oraciones,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
            Icono(R.drawable.ic_notifications, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "La familia lo verá de inmediato en su pantalla de hoy, sin esperar a la hora de salida.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        BotonPrincipal("Registrar y avisar a la familia", onGuardar, guardando = form.guardando, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun IncidentePreview() {
    val nino = Nino("1", "Sofía", "Ramírez Treviño", LocalDate.of(2024, 4, 18), "e", "s", emptyList(), emptyList(), alergias = "Cacahuate")
    SonrisasTheme {
        IncidenteScreen(nino, ColorEstrella.ROJO, IncidenteForm(tipo = TipoIncidente.GOLPE, hora = LocalTime.of(10, 40), intentoGuardar = true), {}, {})
    }
}
