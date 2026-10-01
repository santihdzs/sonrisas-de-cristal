package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// el datepicker de material trabaja en milisegundos utc a medianoche
private fun LocalDate.aMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.aFecha(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    etiqueta: String,
    fecha: LocalDate?,
    onCambio: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    opcional: Boolean = false,
) {
    var abierto by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    fecha?.formatoLargo() ?: "Sin elegir",
                    style = if (fecha != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    color = if (fecha != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (opcional && fecha != null) {
                TextButton(onClick = { onCambio(null) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Quitar") }
            }
            TextButton(onClick = { abierto = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (fecha == null) "Elegir" else "Cambiar")
            }
        }
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    if (abierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = fecha?.aMillis())
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onCambio(it.aFecha()) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}
