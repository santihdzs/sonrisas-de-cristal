package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.domain.PersonaAutorizada
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    guardando: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !guardando,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        if (guardando) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(Espacio.space2))
            Text("Guardando...")
        } else {
            Text(texto)
        }
    }
}

@Composable
fun FilaSwitch(
    texto: String,
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    descripcion: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = activo, role = Role.Switch, onValueChange = onCambio),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        Column(Modifier.weight(1f)) {
            Text(texto, style = MaterialTheme.typography.bodyLarge)
            if (descripcion != null) {
                Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = activo, onCheckedChange = null)
    }
}

@Composable
fun FilaCheck(
    texto: String,
    marcado: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = marcado, enabled = habilitado, role = Role.Checkbox, onValueChange = onCambio),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
    ) {
        Checkbox(checked = marcado, onCheckedChange = null, enabled = habilitado)
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}

/** solo permite elegir entre las personas autorizadas del expediente, sin texto libre (RF-15) */
@Composable
fun SelectorPersona(
    personas: List<PersonaAutorizada>,
    seleccionadaId: String?,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.selectableGroup()) {
        personas.forEach { p ->
            val activa = p.id == seleccionadaId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(selected = activa, role = Role.RadioButton, onClick = { onSeleccion(p.id) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
            ) {
                RadioButton(selected = activa, onClick = null)
                Column {
                    Text(p.nombre, style = MaterialTheme.typography.bodyLarge)
                    Text(p.parentesco, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun EtiquetaCampo(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.padding(top = Espacio.space2))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaModal(
    titulo: String,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    boton: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = estado,
        containerColor = SonrisasTheme.colors.superficieElevada,
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.space4)
                .padding(bottom = Espacio.space4)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            Column {
                Text(titulo, style = MaterialTheme.typography.headlineSmall)
                if (subtitulo != null) {
                    Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            contenido()
            if (boton != null) {
                Spacer(Modifier.size(Espacio.space1))
                boton()
            }
        }
    }
}

val tecladoNombres = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
val tecladoOraciones = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
val tecladoTelefono = KeyboardOptions(keyboardType = KeyboardType.Phone)

@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    modifier: Modifier = Modifier,
    teclado: KeyboardOptions = tecladoOraciones,
    ayuda: String? = null,
    lineas: Int = 1,
) {
    OutlinedTextField(
        value = valor,
        // en telefonos solo se aceptan digitos y espacios, aunque el teclado permita otros simbolos
        onValueChange = { if (teclado == tecladoTelefono) onCambio(it.filter { c -> c.isDigit() || c == ' ' }) else onCambio(it) },
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = lineas == 1,
        minLines = lineas,
        keyboardOptions = teclado,
        modifier = modifier.fillMaxWidth(),
    )
}
