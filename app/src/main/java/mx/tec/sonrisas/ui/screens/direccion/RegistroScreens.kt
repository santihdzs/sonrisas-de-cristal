package mx.tec.sonrisas.ui.screens.direccion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.EtiquetaCampo
import mx.tec.sonrisas.ui.components.GlifoEstrella
import mx.tec.sonrisas.ui.components.HojaModal
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.SelectorOpciones
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@Composable
fun EstanciaFormScreen(
    datos: EstanciaDatos,
    form: EstanciaForm,
    onCambiar: (EstanciaForm) -> Unit,
    onGuardar: () -> Unit,
    onNuevaCuenta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errores = if (form.intento) form.errores(datos.nueva) else emptyMap()
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        TarjetaSeccion(titulo = "Datos de la estancia", icono = R.drawable.ic_apartment) {
            Campo(form.nombre, { onCambiar(form.copy(nombre = it)) }, "Nombre", errores["nombre"], teclado = tecladoNombres, ayuda = "Por ejemplo: Estancia San Bernabé")
            Campo(form.colonia, { onCambiar(form.copy(colonia = it)) }, "Colonia o dirección", errores["colonia"])
            Campo(form.telefono, { onCambiar(form.copy(telefono = it)) }, "Teléfono", errores["telefono"], teclado = tecladoTelefono, ayuda = "10 dígitos")
        }
        if (datos.nueva) {
            TarjetaSeccion(titulo = "Cuenta de dirección", icono = R.drawable.ic_key) {
                Text(
                    "Creamos la cuenta de quien dirige la estancia. Al guardar verás su usuario y una contraseña temporal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Campo(form.direccion, { onCambiar(form.copy(direccion = it)) }, "Nombre completo", errores["direccion"], teclado = tecladoNombres)
            }
        } else {
            TarjetaSeccion(titulo = "Cuentas de dirección", icono = R.drawable.ic_key) {
                if (datos.cuentas.isEmpty()) {
                    Text("Esta estancia todavía no tiene una cuenta de dirección.", style = MaterialTheme.typography.bodyLarge)
                }
                datos.cuentas.forEachIndexed { i, c ->
                    if (i > 0) HorizontalDivider()
                    Column {
                        Text(c.nombre, style = MaterialTheme.typography.titleMedium)
                        Text("Usuario: ${c.usuario}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedButton(onClick = onNuevaCuenta, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icono(R.drawable.ic_person_add, null)
                    Text("Crear cuenta de dirección", modifier = Modifier.padding(start = Espacio.space2))
                }
            }
        }
        if (errores.isNotEmpty()) {
            Text("Revisa los campos marcados antes de guardar.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        BotonPrincipal(if (datos.nueva) "Registrar estancia" else "Guardar cambios", onGuardar, guardando = form.guardando)
    }
}

@Composable
fun HojaNuevaCuenta(
    titulo: String,
    form: NuevaCuentaForm,
    onCambiar: (NuevaCuentaForm) -> Unit,
    onCrear: () -> Unit,
    onCerrar: () -> Unit,
) {
    HojaModal(
        titulo = titulo,
        subtitulo = "Al crearla verás el usuario y una contraseña temporal.",
        onCerrar = onCerrar,
        boton = { BotonPrincipal("Crear cuenta", onCrear, guardando = form.guardando) },
    ) {
        val error = if (form.intento && form.nombre.isBlank()) "Escribe el nombre." else null
        Campo(form.nombre, { onCambiar(form.copy(nombre = it)) }, "Nombre completo", error, teclado = tecladoNombres)
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun SalasScreen(
    datos: SalasDatos,
    onNueva: () -> Unit,
    onEditar: (Sala) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sinColores = datos.salas.size >= ColorEstrella.entries.size
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (!sinColores) {
                ExtendedFloatingActionButton(onClick = onNueva, icon = { Icono(R.drawable.ic_add, null) }, text = { Text("Registrar sala") })
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space2, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            if (sinColores) {
                item {
                    Text(
                        "Ya se usaron los ${ColorEstrella.entries.size} colores de estrella. Para identificar cada sala sin confusiones, una estancia tiene como máximo ${ColorEstrella.entries.size} salas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (datos.salas.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_meeting_room, "Todavía no hay salas", mensaje = "Registra la primera sala para poder inscribir niños y asignar educadoras.") }
            }
            items(datos.salas, key = { it.sala.id }) { f ->
                TarjetaSeccion(onClick = { onEditar(f.sala) }) {
                    EstrellaSala("Sala ${f.sala.nombre}", f.sala.estrella)
                    Text(
                        "${f.sala.tipo.etiqueta} · ${f.inscritos} ${if (f.inscritos == 1) "niño" else "niños"} · Estrella ${f.sala.estrella.etiqueta.lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (f.educadoras.isEmpty()) "Sin educadora asignada" else "Educadora: ${f.educadoras.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun HojaSalaForm(
    hoja: HojaSala,
    errores: Map<String, String>,
    ocupadas: Set<ColorEstrella>,
    onCambiar: (HojaSala) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
) {
    val e = if (hoja.intento) errores else emptyMap()
    HojaModal(
        titulo = if (hoja.id == null) "Registrar sala" else "Editar sala",
        onCerrar = onCerrar,
        boton = { BotonPrincipal(if (hoja.id == null) "Registrar sala" else "Guardar cambios", onGuardar, guardando = hoja.guardando) },
    ) {
        Campo(hoja.nombre, { onCambiar(hoja.copy(nombre = it)) }, "Nombre de la sala", e["nombre"], teclado = tecladoNombres)
        EtiquetaCampo("Tipo de sala")
        SelectorOpciones(TipoSala.entries, hoja.tipo, { onCambiar(hoja.copy(tipo = it)) }, { it.etiqueta })
        EtiquetaCampo("Color de la estrella")
        SelectorEstrella(hoja.estrella, ocupadas, { onCambiar(hoja.copy(estrella = it)) })
        e["estrella"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}

/** los colores que ya usa otra sala aparecen deshabilitados; el nombre del color acompana al glifo */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorEstrella(seleccionada: ColorEstrella?, ocupadas: Set<ColorEstrella>, onSeleccion: (ColorEstrella) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
        verticalArrangement = Arrangement.spacedBy(Espacio.space1),
    ) {
        ColorEstrella.entries.forEach { c ->
            val ocupada = c in ocupadas
            FilterChip(
                selected = c == seleccionada,
                onClick = { onSeleccion(c) },
                enabled = !ocupada,
                label = { Text(if (ocupada) "${c.etiqueta} (en uso)" else c.etiqueta, style = MaterialTheme.typography.labelLarge) },
                leadingIcon = { GlifoEstrella(c, tamano = 24.dp) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EstanciaFormPreview() {
    SonrisasTheme {
        EstanciaFormScreen(
            EstanciaDatos(Estancia("e", "Estancia San Bernabé", "San Bernabé, Monterrey", "81 8331 2040"), listOf(Usuario("u", "direccion", "", "Claudia Martínez Leal", Rol.DIRECCION, "e"))),
            EstanciaForm("Estancia San Bernabé", "San Bernabé, Monterrey", "81 8331 2040"),
            {}, {}, {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SalasPreview() {
    val sala = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
    SonrisasTheme { SalasScreen(SalasDatos(listOf(SalaFila(sala, listOf("Mariana López Hernández"), 14))), {}, {}) }
}
