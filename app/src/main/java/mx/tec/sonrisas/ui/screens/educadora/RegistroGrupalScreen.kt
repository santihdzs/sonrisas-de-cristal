package mx.tec.sonrisas.ui.screens.educadora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Porcion
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TiempoComida
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.CampoHora
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EtiquetaCampo
import mx.tec.sonrisas.ui.components.SelectorOpciones
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun RegistroGrupalScreen(
    datos: GrupalDatos,
    form: RegistroGrupalForm,
    onCambiar: (RegistroGrupalForm) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cuantos = if (form.modo == ModoGrupal.COMIDA) datos.presentes.size
    else form.seleccion?.let { sel -> datos.presentes.count { it.id in sel } } ?: datos.presentes.size
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (datos.presentes.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(Espacio.space4), verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                        val que = if (form.modo == ModoGrupal.COMIDA) "comida" else "siesta"
                        BotonPrincipal(
                            "Guardar $que de $cuantos ${if (cuantos == 1) "niño" else "niños"}",
                            onGuardar,
                            habilitado = cuantos > 0,
                            guardando = form.guardando,
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Espacio.space4),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ModoGrupal.entries.forEachIndexed { i, modo ->
                        SegmentedButton(
                            selected = form.modo == modo,
                            onClick = { onCambiar(form.copy(modo = modo)) },
                            shape = SegmentedButtonDefaults.itemShape(i, ModoGrupal.entries.size),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(if (modo == ModoGrupal.COMIDA) "Comida" else "Siesta") }
                    }
                }
            }
            if (datos.presentes.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_groups, "No hay niños presentes", mensaje = "Registra las llegadas para usar el registro grupal.") }
            } else if (form.modo == ModoGrupal.COMIDA) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                        CampoHora("Hora", form.hora, { onCambiar(form.copy(hora = it)) })
                        EtiquetaCampo("Tiempo")
                        SelectorOpciones(TiempoComida.entries, form.tiempo, { onCambiar(form.copy(tiempo = it)) }, { it.etiqueta })
                        EtiquetaCampo("Aplicar a todos")
                        SelectorOpciones(Porcion.entries, form.porcionGeneral, { onCambiar(form.copy(porcionGeneral = it, excepciones = emptyMap())) }, { it.etiqueta })
                        HorizontalDivider(Modifier.padding(top = Espacio.space2))
                        Text("Ajusta a quien comió distinto", style = MaterialTheme.typography.titleMedium)
                    }
                }
                items(datos.presentes, key = { it.id }) { n ->
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                        FilaNino(n, datos.sala.estrella)
                        SelectorOpciones(
                            Porcion.entries,
                            form.porcionDe(n.id),
                            { onCambiar(form.copy(excepciones = form.excepciones + (n.id to it))) },
                            { it.etiqueta },
                        )
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                        CampoHora("Se durmieron", form.inicio, { onCambiar(form.copy(inicio = it)) })
                        CampoHora("Despertaron", form.fin, { onCambiar(form.copy(fin = it)) })
                        HorizontalDivider(Modifier.padding(top = Espacio.space2))
                        Text("¿Quiénes durmieron?", style = MaterialTheme.typography.titleMedium)
                    }
                }
                items(datos.presentes, key = { it.id }) { n ->
                    val marcado = form.seleccion?.contains(n.id) ?: true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .toggleable(marcado, role = Role.Checkbox) { nuevo ->
                                val base = form.seleccion ?: datos.presentes.map { it.id }.toSet()
                                onCambiar(form.copy(seleccion = if (nuevo) base + n.id else base - n.id))
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
                    ) {
                        Checkbox(checked = marcado, onCheckedChange = null)
                        FilaNino(n, datos.sala.estrella)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaNino(n: Nino, estrella: ColorEstrella) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
        AvatarNino(n.nombre, n.apellidos, estrella, tamano = 40.dp)
        Text(n.nombreCompleto, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun RegistroGrupalPreview() {
    val sala = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
    val hoy = LocalDate.of(2026, 9, 30)
    val ninos = listOf(
        Nino("1", "Sofía", "Ramírez Treviño", hoy.minusMonths(29), "e", "s", emptyList(), emptyList()),
        Nino("2", "Diego", "Garza Leal", hoy.minusMonths(26), "e", "s", emptyList(), emptyList()),
    )
    val h = LocalTime.of(12, 45)
    SonrisasTheme {
        RegistroGrupalScreen(
            GrupalDatos(sala, ninos),
            RegistroGrupalForm(hora = h, tiempo = TiempoComida.COMIDA, excepciones = mapOf("2" to Porcion.POCO), inicio = h, fin = h),
            {}, {},
        )
    }
}
