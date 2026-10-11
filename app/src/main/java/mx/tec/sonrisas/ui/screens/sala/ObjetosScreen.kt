package mx.tec.sonrisas.ui.screens.sala

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.EstadoObjeto
import mx.tec.sonrisas.domain.ObjetoPerdido
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.CampoFecha
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.EstadoChip
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.HojaModal
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.ImagenArchivo
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.TipoEstado
import mx.tec.sonrisas.ui.components.archivoDesde
import mx.tec.sonrisas.ui.components.formatoDiaMes
import mx.tec.sonrisas.ui.components.relativoA
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun ObjetosScreen(
    datos: ObjetosDatos,
    filtro: FiltroObjetos,
    onFiltro: (FiltroObjetos) -> Unit,
    onPublicar: () -> Unit,
    onReclamar: (ObjetoPerdido) -> Unit,
    onEntregar: (ObjetoPerdido) -> Unit,
    onRechazar: (ObjetoPerdido) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibles = datos.objetos.filter { filtro.aplica(it) }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (datos.esPersonal) {
                ExtendedFloatingActionButton(onClick = onPublicar, icon = { Icono(R.drawable.ic_add_a_photo, null) }, text = { Text("Publicar objeto") })
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space2, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                    datos.sala?.let { EstrellaSala("Sala ${it.nombre}", it.estrella) }
                    Text(
                        if (datos.esPersonal) "Publica lo que encuentres en la sala para que su familia lo reclame."
                        else "Cosas que se quedaron en la sala. Si algo es de tu hijo, avísale a la educadora desde aquí.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                    items(FiltroObjetos.entries) { f ->
                        FilterChip(
                            selected = f == filtro,
                            onClick = { onFiltro(f) },
                            label = { Text("${f.etiqueta} (${datos.cuantos(f)})") },
                            leadingIcon = if (f == filtro) ({ Icono(R.drawable.ic_check, null) }) else null,
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
            if (visibles.isEmpty()) {
                item {
                    EstadoVacio(
                        R.drawable.ic_checkroom,
                        when (filtro) {
                            FiltroObjetos.DISPONIBLES -> "No hay objetos perdidos"
                            FiltroObjetos.RECLAMADOS -> "No hay objetos reclamados"
                            FiltroObjetos.HISTORIAL -> "Todavía no se entrega ningún objeto"
                        },
                        mensaje = if (filtro == FiltroObjetos.DISPONIBLES && datos.esPersonal) "Cuando encuentres algo, toca \"Publicar objeto\"." else null,
                    )
                }
            }
            items(visibles, key = { it.id }) { o ->
                TarjetaObjeto(o, datos, onReclamar, onEntregar, onRechazar)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaObjeto(
    o: ObjetoPerdido,
    datos: ObjetosDatos,
    onReclamar: (ObjetoPerdido) -> Unit,
    onEntregar: (ObjetoPerdido) -> Unit,
    onRechazar: (ObjetoPerdido) -> Unit,
) {
    TarjetaSeccion {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            if (o.foto != null) {
                ImagenArchivo(o.foto, "Foto de ${o.descripcion.lowercase()}", Modifier.size(88.dp))
            } else {
                Box(
                    Modifier.size(88.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icono(R.drawable.ic_checkroom, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                Text(o.descripcion, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Se encontró en: ${o.dondeSeEncontro}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(o.fecha.relativoA(datos.hoy), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                    ChipObjeto(o.estado)
                    if (o.id in datos.nuevos) EstadoChip("Nuevo", TipoEstado.INFO, R.drawable.ic_notifications)
                }
            }
        }
        val mio = o.reclamadoPorId != null && o.reclamadoPorId == datos.miId
        when (o.estado) {
            EstadoObjeto.DISPONIBLE -> if (!datos.esPersonal) {
                FilledTonalButton(onClick = { onReclamar(o) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icono(R.drawable.ic_front_hand, null)
                    Text("Es de mi hijo", modifier = Modifier.padding(start = Espacio.space2))
                }
            }
            EstadoObjeto.RECLAMADO -> when {
                datos.esPersonal -> {
                    Text("Lo reclamó ${o.reclamadoPor}", style = MaterialTheme.typography.bodyLarge)
                    o.notaReclamo?.let { Text("Nota: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                        FilledTonalButton(onClick = { onEntregar(o) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icono(R.drawable.ic_check, null)
                            Text("Marcar como entregado", modifier = Modifier.padding(start = Espacio.space2))
                        }
                        TextButton(onClick = { onRechazar(o) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Rechazar reclamo") }
                    }
                }
                mio -> Text("Lo reclamaste. Recógelo con la educadora a la hora de salida.", style = MaterialTheme.typography.bodyMedium)
                else -> Text("Otra familia ya lo reclamó. Si es de tu hijo, habla con la educadora.", style = MaterialTheme.typography.bodyMedium)
            }
            EstadoObjeto.ENTREGADO -> {
                val cuando = o.entregadoEl?.toLocalDate()?.formatoDiaMes()
                val texto = when {
                    datos.esPersonal -> "Se entregó${cuando?.let { " el $it" }.orEmpty()} a ${o.reclamadoPor}"
                    mio -> "Te lo entregamos${cuando?.let { " el $it" }.orEmpty()}."
                    else -> "Ya se entregó a su familia."
                }
                Text(texto, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ChipObjeto(estado: EstadoObjeto) = when (estado) {
    EstadoObjeto.DISPONIBLE -> EstadoChip("Disponible", TipoEstado.NEUTRO, R.drawable.ic_checkroom)
    EstadoObjeto.RECLAMADO -> EstadoChip("Reclamado", TipoEstado.AVISO, R.drawable.ic_front_hand)
    EstadoObjeto.ENTREGADO -> EstadoChip("Entregado", TipoEstado.EXITO, R.drawable.ic_check_circle_fill)
}

@Composable
fun HojaObjetoForm(
    hoja: HojaObjeto,
    hoy: LocalDate,
    onCambiar: (HojaObjeto) -> Unit,
    onPublicar: () -> Unit,
    onReclamar: () -> Unit,
    onCerrar: () -> Unit,
) {
    when (hoja) {
        is HojaObjeto.Publicar -> {
            val context = LocalContext.current
            val selector = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) onCambiar(hoja.copy(foto = context.archivoDesde(uri), vistaPrevia = uri.toString()))
            }
            val e = if (hoja.intento) hoja.errores(hoy) else emptyMap()
            HojaModal(
                titulo = "Publicar objeto perdido",
                onCerrar = onCerrar,
                boton = { BotonPrincipal("Publicar objeto", onPublicar, guardando = hoja.guardando) },
            ) {
                CampoTexto(hoja.descripcion, { onCambiar(hoja.copy(descripcion = it)) }, "Qué es", e["descripcion"], ayuda = "Por ejemplo: suéter azul talla 3")
                CampoTexto(hoja.donde, { onCambiar(hoja.copy(donde = it)) }, "Dónde se encontró", e["donde"])
                CampoFecha("Fecha en que se encontró", hoja.fecha, { it?.let { f -> onCambiar(hoja.copy(fecha = f)) } }, error = e["fecha"])
                if (hoja.vistaPrevia != null) {
                    ImagenArchivo(hoja.vistaPrevia, "Foto elegida", Modifier.fillMaxWidth().heightIn(max = 220.dp).size(220.dp))
                    TextButton(onClick = { onCambiar(hoja.copy(foto = null, vistaPrevia = null)) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icono(R.drawable.ic_close, null)
                        Text("Quitar foto", modifier = Modifier.padding(start = Espacio.space2))
                    }
                } else {
                    OutlinedButton(
                        onClick = { selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icono(R.drawable.ic_add_a_photo, null)
                        Text("Agregar foto del objeto", modifier = Modifier.padding(start = Espacio.space2))
                    }
                }
                Text(
                    "La foto es opcional y debe mostrar solo el objeto, nunca a niñas o niños. Se guarda dentro de la app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        is HojaObjeto.Reclamar -> HojaModal(
            titulo = "¿Es de tu hijo?",
            subtitulo = hoja.objeto.descripcion,
            onCerrar = onCerrar,
            boton = { BotonPrincipal("Enviar reclamo", onReclamar, guardando = hoja.guardando) },
        ) {
            Text(
                "Le avisaremos a la educadora para que lo aparte. Lo entregamos a la hora de salida.",
                style = MaterialTheme.typography.bodyMedium,
            )
            CampoTexto(hoja.nota, { onCambiar(hoja.copy(nota = it)) }, "Nota para la educadora (opcional)", null, ayuda = "Por ejemplo: tiene su nombre en la etiqueta", lineas = 2)
            hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ObjetosPreview() {
    val hoy = LocalDate.of(2026, 10, 12)
    val sala = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
    val objetos = listOf(
        ObjetoPerdido("1", "s", "Suéter azul marino talla 3", "Perchero del salón", hoy, null, "u", LocalDateTime.of(hoy, java.time.LocalTime.NOON)),
        ObjetoPerdido("2", "s", "Chamarra roja", "Patio", hoy.minusDays(2), null, "u", LocalDateTime.of(hoy, java.time.LocalTime.NOON), EstadoObjeto.RECLAMADO, "f", "Paola Elizondo Garza (mamá de Diego)"),
    )
    SonrisasTheme {
        ObjetosScreen(ObjetosDatos(sala, objetos, true, "u", emptySet(), hoy), FiltroObjetos.DISPONIBLES, {}, {}, {}, {}, {})
    }
}
