package mx.tec.sonrisas.ui.screens.sala

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.MaterialClase
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.HojaModal
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.relativoA
import mx.tec.sonrisas.ui.components.tamanoTexto
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private fun iconoTipo(tipo: String): Int = when {
    tipo == "application/pdf" -> R.drawable.ic_picture_as_pdf
    tipo.startsWith("image/") -> R.drawable.ic_image
    tipo.contains("word") -> R.drawable.ic_description
    tipo.contains("excel") || tipo.contains("spreadsheet") -> R.drawable.ic_table_chart
    tipo.contains("powerpoint") || tipo.contains("presentation") -> R.drawable.ic_slideshow
    else -> R.drawable.ic_draft
}

private fun nombreTipo(tipo: String): String = when {
    tipo == "application/pdf" -> "PDF"
    tipo.startsWith("image/") -> "Imagen"
    tipo.contains("word") -> "Word"
    tipo.contains("excel") || tipo.contains("spreadsheet") -> "Excel"
    tipo.contains("powerpoint") || tipo.contains("presentation") -> "PowerPoint"
    else -> "Archivo"
}

@Composable
fun MaterialScreen(
    datos: MaterialDatos,
    onSubir: () -> Unit,
    onAbrir: (MaterialClase) -> Unit,
    onCompartir: (MaterialClase) -> Unit,
    onEditar: (MaterialClase) -> Unit,
    onBorrar: (MaterialClase) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (datos.esPersonal) {
                ExtendedFloatingActionButton(onClick = onSubir, icon = { Icono(R.drawable.ic_upload_file, null) }, text = { Text("Subir material") })
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space2, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                    datos.sala?.let { EstrellaSala("Sala ${it.nombre}", it.estrella) }
                    Text(
                        if (datos.esPersonal) "Comparte canciones, actividades y avisos en archivo con las familias de la sala."
                        else "Archivos que comparte la educadora para trabajar en casa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (datos.material.isEmpty()) {
                item {
                    EstadoVacio(
                        R.drawable.ic_folder, "Todavía no hay material",
                        mensaje = if (datos.esPersonal) "Toca \"Subir material\" para compartir un PDF, una imagen o un documento." else "Cuando la educadora comparta algo, aparecerá aquí.",
                    )
                }
            }
            items(datos.material, key = { it.id }) { m ->
                TarjetaMaterial(m, datos, onAbrir, onCompartir, onEditar, onBorrar)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaMaterial(
    m: MaterialClase,
    datos: MaterialDatos,
    onAbrir: (MaterialClase) -> Unit,
    onCompartir: (MaterialClase) -> Unit,
    onEditar: (MaterialClase) -> Unit,
    onBorrar: (MaterialClase) -> Unit,
) {
    TarjetaSeccion {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.space3), verticalAlignment = Alignment.Top) {
            Icono(iconoTipo(m.tipo), nombreTipo(m.tipo), Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                Text(m.titulo, style = MaterialTheme.typography.titleMedium)
                if (m.descripcion.isNotBlank()) Text(m.descripcion, style = MaterialTheme.typography.bodyMedium)
                Text(
                    listOfNotNull(nombreTipo(m.tipo), tamanoTexto(m.tamano), m.subidoEl.toLocalDate().relativoA(datos.hoy), datos.autores[m.subidoPorId])
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Espacio.space1),
            verticalArrangement = Arrangement.spacedBy(Espacio.space1),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = { onAbrir(m) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_open_in_new, null)
                Text("Abrir", modifier = Modifier.padding(start = Espacio.space2))
            }
            TextButton(onClick = { onCompartir(m) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_share, null)
                Text("Compartir", modifier = Modifier.padding(start = Espacio.space2))
            }
            if (datos.puedeEditar(m)) {
                IconButton(onClick = { onEditar(m) }) { Icono(R.drawable.ic_edit, "Editar ${m.titulo}") }
                IconButton(onClick = { onBorrar(m) }) { Icono(R.drawable.ic_delete, "Borrar ${m.titulo}") }
            }
        }
    }
}

@Composable
fun HojaMaterialForm(hoja: HojaMaterial, onCambiar: (HojaMaterial) -> Unit, onGuardar: () -> Unit, onCerrar: () -> Unit) {
    when (hoja) {
        is HojaMaterial.Subir -> HojaModal(
            titulo = "Subir material",
            subtitulo = "${hoja.archivo.nombre}${hoja.archivo.tamano?.let { " · ${tamanoTexto(it)}" }.orEmpty()}",
            onCerrar = onCerrar,
            boton = { BotonPrincipal("Subir material", onGuardar, guardando = hoja.guardando) },
        ) {
            val error = if (hoja.intento && hoja.titulo.isBlank()) "Escribe el título." else null
            CampoTexto(hoja.titulo, { onCambiar(hoja.copy(titulo = it)) }, "Título", error)
            CampoTexto(hoja.descripcion, { onCambiar(hoja.copy(descripcion = it)) }, "Descripción (opcional)", null, lineas = 3)
            Text("Lo verán las familias de la sala. Se guarda una copia dentro de la app.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        is HojaMaterial.Editar -> HojaModal(
            titulo = "Editar material",
            subtitulo = hoja.material.nombreArchivo,
            onCerrar = onCerrar,
            boton = { BotonPrincipal("Guardar cambios", onGuardar, guardando = hoja.guardando) },
        ) {
            val error = if (hoja.intento && hoja.titulo.isBlank()) "Escribe el título." else null
            CampoTexto(hoja.titulo, { onCambiar(hoja.copy(titulo = it)) }, "Título", error)
            CampoTexto(hoja.descripcion, { onCambiar(hoja.copy(descripcion = it)) }, "Descripción (opcional)", null, lineas = 3)
            hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        is HojaMaterial.Borrar -> AlertDialog(
            onDismissRequest = onCerrar,
            icon = { Icono(R.drawable.ic_delete, null) },
            title = { Text("¿Borrar \"${hoja.material.titulo}\"?") },
            text = { Text("Se borra el archivo para todas las familias de la sala. No se puede deshacer.") },
            confirmButton = { TextButton(onClick = onGuardar) { Text("Borrar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MaterialPreview() {
    val hoy = LocalDate.of(2026, 10, 12)
    val sala = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
    val m = MaterialClase("1", "s", "Canción de los días de la semana", "Para practicar en casa.", "cancion.pdf", "application/pdf", 24_000, "", "u", LocalDateTime.of(hoy, LocalTime.NOON))
    SonrisasTheme {
        MaterialScreen(MaterialDatos(sala, listOf(m), mapOf("u" to "Mariana López Hernández"), true, "u", false, hoy), {}, {}, {}, {}, {})
    }
}
