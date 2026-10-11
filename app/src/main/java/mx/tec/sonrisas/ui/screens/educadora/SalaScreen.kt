package mx.tec.sonrisas.ui.screens.educadora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.BotonAcceso
import mx.tec.sonrisas.ui.components.EstadoChip
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.TipoEstado
import mx.tec.sonrisas.ui.components.edadTexto
import mx.tec.sonrisas.ui.components.formatoHora
import mx.tec.sonrisas.ui.components.formatoLargo
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ChipAsistencia(asistencia: Asistencia?, modifier: Modifier = Modifier) {
    when {
        asistencia == null -> EstadoChip("Sin llegar", TipoEstado.NEUTRO, R.drawable.ic_schedule, modifier)
        asistencia.horaSalida != null -> EstadoChip("Salió ${asistencia.horaSalida.formatoHora()}", TipoEstado.INFO, R.drawable.ic_logout, modifier)
        else -> EstadoChip("Presente desde ${asistencia.horaEntrada.formatoHora()}", TipoEstado.EXITO, R.drawable.ic_check_circle_fill, modifier)
    }
}

@Composable
fun ChipSalud(nino: Nino, modifier: Modifier = Modifier) {
    when {
        nino.alergias.isNotBlank() -> EstadoChip("Alergia", TipoEstado.AVISO, R.drawable.ic_warning_fill, modifier)
        nino.padecimientos.isNotBlank() || nino.medicamentos.isNotBlank() ->
            EstadoChip("Salud", TipoEstado.AVISO, R.drawable.ic_warning_fill, modifier)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SalaScreen(
    datos: SalaDatos,
    filtro: FiltroSala,
    onFiltro: (FiltroSala) -> Unit,
    onNino: (Nino) -> Unit,
    onLlegada: (Nino) -> Unit,
    onSalida: (Nino, Asistencia) -> Unit,
    onRegistroGrupal: () -> Unit,
    onObjetos: () -> Unit,
    onMaterial: () -> Unit,
    onImprimir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibles = datos.ninos.filter { filtro.aplica(it) }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRegistroGrupal,
                icon = { Icono(R.drawable.ic_group_add, null) },
                text = { Text("Registro grupal") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                    EstrellaSala("Sala ${datos.sala.nombre}", datos.sala.estrella)
                    Text(datos.fecha.formatoLargo(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${datos.presentes} de ${datos.ninos.size} presentes", style = MaterialTheme.typography.headlineSmall)
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                    BotonAcceso("Material de clase", R.drawable.ic_folder, onMaterial)
                    BotonAcceso("Objetos perdidos", R.drawable.ic_checkroom, onObjetos)
                    BotonAcceso("Imprimir bitácora", R.drawable.ic_print, onImprimir)
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                    items(FiltroSala.entries) { f ->
                        FilterChip(
                            selected = f == filtro,
                            onClick = { onFiltro(f) },
                            label = { Text(f.etiqueta) },
                            leadingIcon = if (f == filtro) ({ Icono(R.drawable.ic_check, null) }) else null,
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
            if (visibles.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_child_care, "No hay niños en este filtro", mensaje = "Cambia el filtro para ver al resto de la sala.") }
            }
            items(visibles, key = { it.nino.id }) { item ->
                TarjetaNino(item, datos.fecha, datos.sala.estrella, onClick = { onNino(item.nino) }, onLlegada = { onLlegada(item.nino) }, onSalida = { a -> onSalida(item.nino, a) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaNino(
    item: NinoEnSala,
    hoy: LocalDate,
    estrella: ColorEstrella,
    onClick: () -> Unit,
    onLlegada: () -> Unit,
    onSalida: (Asistencia) -> Unit,
) {
    val n = item.nino
    TarjetaSeccion(onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space3), verticalAlignment = Alignment.Top) {
            AvatarNino(n.nombre, n.apellidos, estrella)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                Text(n.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                Text(edadTexto(n.fechaNacimiento, hoy), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
                    verticalArrangement = Arrangement.spacedBy(Espacio.space2),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    ChipAsistencia(item.asistencia)
                    ChipSalud(n)
                    when (item.estado) {
                        EstadoAsistencia.SIN_LLEGAR -> FilledTonalButton(onClick = onLlegada, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icono(R.drawable.ic_login, null)
                            Text("Llegada", modifier = Modifier.padding(start = Espacio.space2))
                        }
                        EstadoAsistencia.PRESENTE -> FilledTonalButton(onClick = { onSalida(item.asistencia!!) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icono(R.drawable.ic_logout, null)
                            Text("Salida", modifier = Modifier.padding(start = Espacio.space2))
                        }
                        EstadoAsistencia.SALIO -> Unit
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SalaPreview() {
    val sala = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
    val hoy = LocalDate.of(2026, 9, 30)
    val sofia = Nino("1", "Sofía", "Ramírez Treviño", hoy.minusMonths(29), "e", "s", emptyList(), emptyList(), alergias = "Cacahuate")
    val diego = Nino("2", "Diego", "Garza Leal", hoy.minusMonths(26), "e", "s", emptyList(), emptyList())
    val datos = SalaDatos(
        sala, hoy,
        listOf(
            NinoEnSala(sofia, Asistencia("a", "1", hoy, LocalTime.of(8, 5), "p", "e")),
            NinoEnSala(diego, null),
        ),
    )
    SonrisasTheme {
        SalaScreen(datos, FiltroSala.TODOS, {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
    }
}
