package mx.tec.sonrisas.ui.screens.direccion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.ui.components.EstadoChip
import mx.tec.sonrisas.ui.components.HojaModal
import mx.tec.sonrisas.ui.components.TipoEstado
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FormaCampo
import mx.tec.sonrisas.ui.components.coloresCampo
import mx.tec.sonrisas.ui.components.coincideCon
import mx.tec.sonrisas.ui.components.tecladoNombres
import mx.tec.sonrisas.ui.components.tecladoTelefono
import mx.tec.sonrisas.ui.components.CampoFecha
import mx.tec.sonrisas.ui.components.CampoHora
import mx.tec.sonrisas.ui.components.EtiquetaCampo
import mx.tec.sonrisas.ui.components.FilaSwitch
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.SelectorOpciones
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.components.icono
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InscripcionScreen(
    datos: InscripcionDatos,
    form: InscripcionForm,
    onCambiar: (InscripcionForm) -> Unit,
    onElegirRegistrado: (Tutor) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errores = if (form.intento) form.errores(datos.hoy) else emptyMap()
    var eligiendo by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        TarjetaSeccion(titulo = "Datos generales", icono = R.drawable.ic_badge) {
            CampoTexto(form.nombre, { onCambiar(form.copy(nombre = it)) }, "Nombre", errores["nombre"], teclado = tecladoNombres)
            CampoTexto(form.apellidos, { onCambiar(form.copy(apellidos = it)) }, "Apellidos", errores["apellidos"], teclado = tecladoNombres)
            CampoFecha("Fecha de nacimiento", form.fechaNacimiento, { onCambiar(form.copy(fechaNacimiento = it)) }, error = errores["fecha"])
            EtiquetaCampo("Sala")
            if (datos.salas.isEmpty()) {
                Text("Primero registra una sala en Más > Salas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SelectorOpciones(datos.salas.map { it.id }, form.salaId, { onCambiar(form.copy(salaId = it)) }, { id -> datos.salas.first { it.id == id }.nombre })
            errores["sala"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        TarjetaSeccion(titulo = "Información médica", icono = R.drawable.ic_medical_services) {
            Text("Opcional. La educadora la verá destacada en la ficha del niño.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            CampoTexto(form.alergias, { onCambiar(form.copy(alergias = it)) }, "Alergias", null)
            CampoTexto(form.padecimientos, { onCambiar(form.copy(padecimientos = it)) }, "Padecimientos", null)
            CampoTexto(form.medicamentos, { onCambiar(form.copy(medicamentos = it)) }, "Medicamentos y dosis", null)
        }
        TarjetaSeccion(titulo = "Tutores", icono = R.drawable.ic_family_restroom) {
            form.tutores.forEachIndexed { i, t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tutor ${i + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    if (form.tutores.size > 1) {
                        IconButton(onClick = { onCambiar(form.copy(tutores = form.tutores - t)) }) {
                            Icono(R.drawable.ic_delete, "Quitar tutor ${i + 1}", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                fun cambiar(nuevo: FilaTutor) = onCambiar(form.copy(tutores = form.tutores.map { if (it.id == t.id) nuevo else it }))
                val hermanos = datos.hijosDe[t.id].orEmpty()
                if (t.existente && hermanos.isNotEmpty()) {
                    Text(
                        "Tutor registrado. También es tutor de ${hermanos.joinToString(", ")}; los cambios aplican para todos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CampoTexto(t.nombre, { cambiar(t.copy(nombre = it)) }, "Nombre completo", errores["tutor.nombre.${t.id}"], teclado = tecladoNombres)
                CampoTexto(t.parentesco, { cambiar(t.copy(parentesco = it)) }, "Parentesco", errores["tutor.parentesco.${t.id}"], teclado = tecladoNombres, ayuda = "Por ejemplo: Mamá, Papá, Abuela")
                CampoTexto(t.telefono, { cambiar(t.copy(telefono = it)) }, "Teléfono", errores["tutor.telefono.${t.id}"], teclado = tecladoTelefono, ayuda = "10 dígitos")
                FilaSwitch("Puede recogerlo", t.puedeRecoger, { cambiar(t.copy(puedeRecoger = it)) })
                val cuenta = datos.cuentas[t.id]
                if (cuenta != null) {
                    EstadoChip("Con acceso a la app · ${cuenta.usuario}", TipoEstado.EXITO, R.drawable.ic_check_circle_fill)
                } else {
                    FilaSwitch(
                        "Dar acceso a la app",
                        t.darAcceso,
                        { cambiar(t.copy(darAcceso = it)) },
                        descripcion = "Creamos una cuenta de familia con usuario y contraseña temporal.",
                    )
                }
            }
            errores["tutores"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.space2), verticalArrangement = Arrangement.spacedBy(Espacio.space2)) {
                FilledTonalButton(onClick = { onCambiar(form.copy(tutores = form.tutores + FilaTutor())) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icono(R.drawable.ic_add, null)
                    Text("Agregar tutor", modifier = Modifier.padding(start = Espacio.space2))
                }
                val disponibles = datos.registrados.filter { r -> form.tutores.none { it.id == r.id } }
                if (disponibles.isNotEmpty()) {
                    FilledTonalButton(onClick = { eligiendo = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icono(R.drawable.ic_family_restroom, null)
                        Text("Elegir tutor registrado", modifier = Modifier.padding(start = Espacio.space2))
                    }
                }
            }
        }
        TarjetaSeccion(titulo = "Otras personas autorizadas", icono = R.drawable.ic_person) {
            Text(
                "Solo las personas de esta lista y los tutores marcados pueden recogerlo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            form.otras.forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Persona ${i + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onCambiar(form.copy(otras = form.otras - p)) }) {
                        Icono(R.drawable.ic_delete, "Quitar persona ${i + 1}", tint = MaterialTheme.colorScheme.error)
                    }
                }
                fun cambiar(nueva: FilaPersona) = onCambiar(form.copy(otras = form.otras.map { if (it.id == p.id) nueva else it }))
                CampoTexto(p.nombre, { cambiar(p.copy(nombre = it)) }, "Nombre completo", errores["otra.nombre.${p.id}"], teclado = tecladoNombres)
                CampoTexto(p.parentesco, { cambiar(p.copy(parentesco = it)) }, "Parentesco", errores["otra.parentesco.${p.id}"], teclado = tecladoNombres)
                CampoTexto(p.telefono, { cambiar(p.copy(telefono = it)) }, "Teléfono", errores["otra.telefono.${p.id}"], teclado = tecladoTelefono)
            }
            errores["recoger"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            FilledTonalButton(onClick = { onCambiar(form.copy(otras = form.otras + FilaPersona())) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icono(R.drawable.ic_add, null)
                Text("Agregar persona", modifier = Modifier.padding(start = Espacio.space2))
            }
        }
        TarjetaSeccion(titulo = "Autorizaciones de imagen", icono = R.drawable.ic_image) {
            FilaSwitch(
                "Actividades internas de la estancia",
                form.permisoInterna,
                { onCambiar(form.copy(permisoInterna = it)) },
                descripcion = "Puede aparecer en fotos que solo ven las familias de la sala.",
            )
            FilaSwitch(
                "Redes sociales de la fundación",
                form.permisoRedes,
                { onCambiar(form.copy(permisoRedes = it)) },
                descripcion = "Puede aparecer en fotos publicadas por la fundación.",
            )
            if (form.permisoRedes && !form.permisoInterna) {
                Text(
                    "Sin la autorización interna tampoco podrá aparecer en fotos para redes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (errores.isNotEmpty()) {
            Text("Revisa los campos marcados antes de guardar.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        BotonPrincipal("Guardar expediente", onGuardar, guardando = form.guardando)
    }
    if (eligiendo) {
        HojaTutoresRegistrados(
            tutores = datos.registrados.filter { r -> form.tutores.none { it.id == r.id } },
            hijosDe = datos.hijosDe,
            onElegir = {
                onElegirRegistrado(it)
                eligiendo = false
            },
            onCerrar = { eligiendo = false },
        )
    }
}

/** para inscribir a un hermano: el tutor se reusa y su cuenta de familia ve a ambos niños */
@Composable
private fun HojaTutoresRegistrados(
    tutores: List<Tutor>,
    hijosDe: Map<String, List<String>>,
    onElegir: (Tutor) -> Unit,
    onCerrar: () -> Unit,
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    HojaModal(titulo = "Elegir tutor registrado", subtitulo = "Tutores de otros niños de esta estancia.", onCerrar = onCerrar) {
        TextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text("Buscar por nombre") },
            leadingIcon = { Icono(R.drawable.ic_search, null) },
            singleLine = true,
            shape = FormaCampo,
            colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth(),
        )
        val filtrados = tutores.filter { it.nombre.coincideCon(busqueda) }
        if (filtrados.isEmpty()) Text("No encontramos tutores con ese nombre.", style = MaterialTheme.typography.bodyLarge)
        filtrados.forEach { t ->
            val hijos = hijosDe[t.id].orEmpty()
            // cada tutor es una tarjeta gris sin contorno, como la eleccion de personas autorizadas
            ListItem(
                headlineContent = { Text(t.nombre, style = MaterialTheme.typography.titleMedium) },
                supportingContent = { Text(listOfNotNull(t.parentesco, hijos.takeIf { it.isNotEmpty() }?.let { "de ${it.joinToString(", ")}" }).joinToString(" ")) },
                trailingContent = { Icono(R.drawable.ic_chevron_right, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable { onElegir(t) },
            )
        }
    }
}

@Composable
fun PublicarAvisoScreen(form: AvisoForm, onCambiar: (AvisoForm) -> Unit, onPublicar: () -> Unit, modifier: Modifier = Modifier) {
    val errores = if (form.intento) form.errores else emptyMap()
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        EtiquetaCampo("Tipo de aviso")
        SelectorOpciones(TipoAviso.entries, form.tipo, { onCambiar(form.copy(tipo = it)) }, { it.etiqueta }, icono = { it.icono })
        CampoTexto(form.titulo, { onCambiar(form.copy(titulo = it)) }, "Título", errores["titulo"])
        CampoTexto(form.cuerpo, { onCambiar(form.copy(cuerpo = it)) }, "Mensaje", errores["cuerpo"], lineas = 4)
        CampoFecha("Fecha del evento (opcional)", form.fecha, { onCambiar(form.copy(fecha = it)) }, opcional = true)
        if (form.fecha != null) CampoHora("Hora del evento", form.hora, { onCambiar(form.copy(hora = it)) })
        Text(
            "Lo recibirán las familias y educadoras de esta estancia.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        BotonPrincipal("Publicar aviso", onPublicar, guardando = form.guardando)
    }
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun InscripcionPreview() {
    val salas = listOf(
        Sala("a", "e", "Lactantes", TipoSala.LACTANTES, ColorEstrella.AZUL),
        Sala("b", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO),
    )
    SonrisasTheme {
        InscripcionScreen(InscripcionDatos(salas, false, LocalDate.of(2026, 9, 30)), InscripcionForm(intento = true), {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PublicarAvisoPreview() {
    SonrisasTheme { PublicarAvisoScreen(AvisoForm(tipo = TipoAviso.JUNTA, titulo = "Junta de padres"), {}, {}) }
}
