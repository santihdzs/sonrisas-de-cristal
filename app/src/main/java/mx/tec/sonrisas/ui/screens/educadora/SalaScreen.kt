package mx.tec.sonrisas.ui.screens.educadora

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.BannerAviso
import mx.tec.sonrisas.ui.components.BotonAcceso
import mx.tec.sonrisas.ui.components.BotonFlotante
import mx.tec.sonrisas.ui.components.BotonPerfil
import mx.tec.sonrisas.ui.components.ChipFiltro
import mx.tec.sonrisas.ui.components.ChipTranslucido
import mx.tec.sonrisas.ui.components.DonaConLeyenda
import mx.tec.sonrisas.ui.components.EncabezadoTeal
import mx.tec.sonrisas.ui.components.EstadoPildora
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EtiquetaSeccion
import mx.tec.sonrisas.ui.components.FormaAvatar
import mx.tec.sonrisas.ui.components.GlifoEstrella
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.PildoraEstado
import mx.tec.sonrisas.ui.components.SegmentoDona
import mx.tec.sonrisas.ui.components.TarjetaBase
import mx.tec.sonrisas.ui.components.edadTexto
import mx.tec.sonrisas.ui.components.formatoHora
import mx.tec.sonrisas.ui.components.formatoLargo
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalTime

/** estado de asistencia en pildora; tambien lo usa la ficha del niño */
@Composable
fun ChipAsistencia(asistencia: Asistencia?, modifier: Modifier = Modifier) {
    when {
        asistencia == null -> PildoraEstado("Sin llegar", EstadoPildora.SIN_LLEGAR, modifier)
        asistencia.horaSalida != null -> PildoraEstado("Salió ${asistencia.horaSalida?.formatoHora()}", EstadoPildora.SALIO, modifier)
        else -> PildoraEstado("Presente desde ${asistencia.horaEntrada.formatoHora()}", EstadoPildora.PRESENTE, modifier)
    }
}

/** "Alergia" si tiene alergias, "Salud" si solo tiene padecimientos o medicamentos */
private fun textoSalud(nino: Nino): String? = when {
    nino.alergias.isNotBlank() -> "Alergia"
    nino.tieneInfoMedica -> "Salud"
    else -> null
}

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
    onPerfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibles = datos.ninos.filter { filtro.aplica(it) }
    val conSalud = datos.ninos.count { it.nino.tieneInfoMedica }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = { BotonFlotante("Registro grupal", R.drawable.ic_group_add, onRegistroGrupal) },
    ) { padding ->
        val lista = rememberLazyListState()
        // cuando el encabezado teal sale de la pantalla, la barra de estado recupera su fondo claro
        val encabezadoFuera by remember { derivedStateOf { lista.firstVisibleItemIndex > 0 } }
        Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            state = lista,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Espacio.finalConBoton),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            item { EncabezadoSala(datos, onPerfil) }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Espacio.space4),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
                ) {
                    BotonAcceso("Material de clase", R.drawable.ic_folder, onMaterial)
                    BotonAcceso("Objetos perdidos", R.drawable.ic_checkroom, onObjetos)
                    BotonAcceso("Imprimir bitácora", R.drawable.ic_print, onImprimir)
                }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Espacio.space4),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
                ) {
                    items(FiltroSala.entries) { f ->
                        ChipFiltro(
                            texto = f.etiqueta,
                            seleccionado = f == filtro,
                            onClick = { onFiltro(f) },
                            contador = datos.ninos.count { f.aplica(it) },
                        )
                    }
                }
            }
            if (conSalud > 0) {
                item {
                    BannerAviso(
                        if (conSalud == 1) "1 niño con alerta de salud" else "$conSalud niños con alerta de salud",
                        Modifier.padding(horizontal = Espacio.space4),
                    )
                }
            }
            item { EtiquetaSeccion("Niños de la sala", Modifier.padding(horizontal = Espacio.space4)) }
            if (visibles.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_child_care, "No hay niños en este filtro", mensaje = "Cambia el filtro para ver al resto de la sala.") }
            }
            // dos columnas; cada fila mide lo que la tarjeta mas alta para que las dos queden parejas
            items(visibles.chunked(2), key = { fila -> fila.first().nino.id }) { fila ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = Espacio.space4),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
                ) {
                    fila.forEach { item ->
                        TarjetaNino(
                            item,
                            datos.fecha,
                            datos.sala.estrella,
                            onClick = { onNino(item.nino) },
                            onLlegada = { onLlegada(item.nino) },
                            onSalida = { a -> onSalida(item.nino, a) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (fila.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (encabezadoFuera) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background),
            )
        }
        }
    }
}

@Composable
private fun EncabezadoSala(datos: SalaDatos, onPerfil: () -> Unit) {
    val colores = SonrisasTheme.colors
    val sinLlegar = datos.ninos.count { it.estado == EstadoAsistencia.SIN_LLEGAR }
    val salieron = datos.ninos.count { it.estado == EstadoAsistencia.SALIO }
    EncabezadoTeal("Mi sala", accion = { BotonPerfil(onPerfil, sobreTeal = true) }) {
        ChipTranslucido("Sala ${datos.sala.nombre}", inicio = { GlifoEstrella(datos.sala.estrella, tamano = 20.dp) })
        Text(datos.fecha.formatoLargo(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        Box(
            Modifier
                .padding(vertical = Espacio.space2)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.15f)),
        )
        DonaConLeyenda(
            segmentos = listOf(
                SegmentoDona("Presentes", datos.presentes, Color.White),
                SegmentoDona("Sin llegar", sinLlegar, Color.White.copy(alpha = 0.4f)),
                SegmentoDona("Ya salieron", salieron, colores.acentoSobreTeal),
            ),
            textoCentro = "presentes",
        )
    }
}

@Composable
private fun TarjetaNino(
    item: NinoEnSala,
    hoy: LocalDate,
    estrella: ColorEstrella,
    onClick: () -> Unit,
    onLlegada: () -> Unit,
    onSalida: (Asistencia) -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = item.nino
    val salud = textoSalud(n)
    TarjetaBase(modifier = modifier, onClick = onClick) {
        Column(Modifier.fillMaxHeight().padding(14.dp), verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
            Row(verticalAlignment = Alignment.Top) {
                AvatarNino(n.nombre, n.apellidos, estrella, tamano = 52.dp, forma = FormaAvatar.CUADRADO)
                Spacer(Modifier.weight(1f))
                if (salud != null) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(SonrisasTheme.colors.avisoContenedor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icono(R.drawable.ic_warning_fill, salud, Modifier.size(16.dp), tint = SonrisasTheme.colors.aviso)
                    }
                }
            }
            Spacer(Modifier.height(Espacio.space2))
            Text(n.nombreCompleto, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(edadTexto(n.fechaNacimiento, hoy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f).heightIn(min = Espacio.space2))
            ChipAsistencia(item.asistencia, Modifier.fillMaxWidth())
            when (item.estado) {
                EstadoAsistencia.SIN_LLEGAR -> BotonAsistencia("Llegada", R.drawable.ic_login, onLlegada)
                EstadoAsistencia.PRESENTE -> BotonAsistencia("Salida", R.drawable.ic_logout) { onSalida(item.asistencia!!) }
                EstadoAsistencia.SALIO -> Unit
            }
        }
    }
}

@Composable
private fun BotonAsistencia(texto: String, icono: Int, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = Espacio.space1)) {
        Icono(icono, null, Modifier.size(18.dp))
        Text(texto, modifier = Modifier.padding(start = Espacio.space2))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun SalaPreview() {
    val sala = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
    val hoy = LocalDate.of(2026, 9, 30)
    val sofia = Nino("1", "Sofía", "Ramírez Treviño", hoy.minusMonths(29), "e", "s", emptyList(), emptyList(), alergias = "Cacahuate")
    val diego = Nino("2", "Diego", "Garza Leal", hoy.minusMonths(26), "e", "s", emptyList(), emptyList())
    val ana = Nino("3", "Ana", "Cantú Elizondo", hoy.minusMonths(34), "e", "s", emptyList(), emptyList())
    val datos = SalaDatos(
        sala, hoy,
        listOf(
            NinoEnSala(sofia, Asistencia("a", "1", hoy, LocalTime.of(8, 5), "p", "e")),
            NinoEnSala(diego, null),
            NinoEnSala(ana, null),
        ),
    )
    SonrisasTheme {
        SalaScreen(datos, FiltroSala.TODOS, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {})
    }
}
