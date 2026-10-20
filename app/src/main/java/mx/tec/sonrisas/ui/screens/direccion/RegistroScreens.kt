package mx.tec.sonrisas.ui.screens.direccion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.ui.components.BannerAviso
import mx.tec.sonrisas.ui.components.BotonFlotante
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.tecladoNombres
import mx.tec.sonrisas.ui.components.tecladoTelefono
import mx.tec.sonrisas.ui.components.EstadoChip
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.EtiquetaCampo
import mx.tec.sonrisas.ui.components.GlifoEstrella
import mx.tec.sonrisas.ui.components.HojaModal
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.SelectorOpciones
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.TipoEstado
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
            CampoTexto(form.nombre, { onCambiar(form.copy(nombre = it)) }, "Nombre", errores["nombre"], teclado = tecladoNombres, ayuda = "Por ejemplo: Estancia San Bernabé")
            CampoTexto(form.colonia, { onCambiar(form.copy(colonia = it)) }, "Colonia o dirección", errores["colonia"])
            CampoTexto(form.telefono, { onCambiar(form.copy(telefono = it)) }, "Teléfono", errores["telefono"], teclado = tecladoTelefono, ayuda = "10 dígitos")
        }
        if (datos.nueva) {
            TarjetaSeccion(titulo = "Cuenta de dirección", icono = R.drawable.ic_key) {
                Text(
                    "Creamos la cuenta de quien dirige la estancia. Al guardar verás su usuario y una contraseña temporal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CampoTexto(form.direccion, { onCambiar(form.copy(direccion = it)) }, "Nombre completo", errores["direccion"], teclado = tecladoNombres)
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
        CampoTexto(form.nombre, { onCambiar(form.copy(nombre = it)) }, "Nombre completo", error, teclado = tecladoNombres)
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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            // sin colores de estrella libres ya no se pueden registrar mas salas
            if (!sinColores) BotonFlotante("Registrar sala", R.drawable.ic_add, onNueva)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space2, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            if (sinColores) {
                item {
                    BannerAviso(
                        icono = R.drawable.ic_info,
                    ) {
                        Text(
                            "Ya se usaron los ${ColorEstrella.entries.size} colores de estrella. Para identificar cada sala sin confusiones, una estancia tiene como máximo ${ColorEstrella.entries.size} salas.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            if (datos.salas.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_meeting_room, "Todavía no hay salas", mensaje = "Registra la primera sala para poder inscribir niños y asignar educadoras.") }
            }
            items(datos.salas, key = { it.sala.id }) { f ->
                TarjetaSeccion(
                    onClick = { onEditar(f.sala) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EstrellaSala("Sala ${f.sala.nombre}", f.sala.estrella, Modifier.weight(1f))
                        Icono(R.drawable.ic_edit, "Editar sala ${f.sala.nombre}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
        CampoTexto(hoja.nombre, { onCambiar(hoja.copy(nombre = it)) }, "Nombre de la sala", e["nombre"], teclado = tecladoNombres)
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
            EstanciaDatos(Estancia("e", "Estancia San Bernabé", "San Bernabé, Monterrey", "81 8331 2040"), listOf(Usuario("u", "direccion", "Claudia Martínez Leal", Rol.DIRECCION, "e"))),
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

@Composable
fun PersonalScreen(
    datos: PersonalDatos,
    onNueva: () -> Unit,
    onEducadora: (Usuario) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (datos.salas.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = onNueva, icon = { Icono(R.drawable.ic_person_add, null) }, text = { Text("Registrar educadora") })
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Espacio.space4, end = Espacio.space4, top = Espacio.space2, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.space3),
        ) {
            if (datos.educadoras.isEmpty()) {
                item {
                    EstadoVacio(
                        R.drawable.ic_badge, "Todavía no hay educadoras registradas",
                        mensaje = if (datos.salas.isEmpty()) "Primero registra una sala en Más > Salas." else "Registra a una educadora y asígnale su sala.",
                    )
                }
            }
            items(datos.educadoras, key = { it.id }) { u ->
                val sala = datos.sala(u.salaId)
                TarjetaSeccion(onClick = { onEducadora(u) }) {
                    Text(u.nombre, style = MaterialTheme.typography.titleMedium)
                    Text("Usuario: ${u.usuario}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (u.activo) {
                        if (sala != null) EstrellaSala("Sala ${sala.nombre}", sala.estrella, tamano = 24.dp)
                        else Text("Sin sala asignada", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        EstadoChip("Sin acceso a la app", TipoEstado.NEUTRO, R.drawable.ic_person_off)
                    }
                }
            }
        }
    }
}

@Composable
fun HojaPersonalForm(
    hoja: HojaPersonal,
    salas: List<Sala>,
    onCambiar: (HojaPersonal) -> Unit,
    onRegistrar: () -> Unit,
    onAsignar: () -> Unit,
    onRetirar: () -> Unit,
    onCerrar: () -> Unit,
) {
    when (hoja) {
        is HojaPersonal.Nueva -> {
            val e = if (hoja.intento) hoja.errores else emptyMap()
            HojaModal(
                titulo = "Registrar educadora",
                subtitulo = "Al registrarla verás su usuario y una contraseña temporal.",
                onCerrar = onCerrar,
                boton = { BotonPrincipal("Registrar educadora", onRegistrar, guardando = hoja.guardando) },
            ) {
                CampoTexto(hoja.nombre, { onCambiar(hoja.copy(nombre = it)) }, "Nombre completo", e["nombre"], teclado = tecladoNombres)
                EtiquetaCampo("Sala")
                SelectorOpciones(salas.map { it.id }, hoja.salaId, { onCambiar(hoja.copy(salaId = it)) }, { id -> salas.first { it.id == id }.nombre })
                e["sala"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        is HojaPersonal.Detalle -> {
            val u = hoja.educadora
            HojaModal(
                titulo = u.nombre,
                subtitulo = "Usuario: ${u.usuario}",
                onCerrar = onCerrar,
                boton = if (u.activo) ({ BotonPrincipal("Guardar sala", onAsignar, guardando = hoja.guardando) }) else null,
            ) {
                if (u.activo) {
                    EtiquetaCampo("Sala asignada")
                    SelectorOpciones(salas.map { it.id }, hoja.salaId, { onCambiar(hoja.copy(salaId = it)) }, { id -> salas.first { it.id == id }.nombre })
                    OutlinedButton(
                        onClick = { onCambiar(hoja.copy(confirmarRetiro = true)) },
                        enabled = !hoja.guardando,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icono(R.drawable.ic_person_off, null, tint = MaterialTheme.colorScheme.error)
                        Text("Retirar acceso", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = Espacio.space2))
                    }
                } else {
                    EstadoChip("Sin acceso a la app", TipoEstado.NEUTRO, R.drawable.ic_person_off)
                    Text(
                        "Ya no puede iniciar sesión. Lo que registró sigue en la bitácora y en el registro de actividad con su nombre.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                hoja.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
            if (hoja.confirmarRetiro) {
                AlertDialog(
                    onDismissRequest = { onCambiar(hoja.copy(confirmarRetiro = false)) },
                    icon = { Icono(R.drawable.ic_person_off, null) },
                    title = { Text("¿Retirar el acceso de ${u.nombre}?") },
                    text = { Text("Ya no podrá iniciar sesión ni verá a los niños de la sala. Su historial se conserva en el registro de actividad.") },
                    confirmButton = { TextButton(onClick = onRetirar) { Text("Retirar acceso", color = MaterialTheme.colorScheme.error) } },
                    dismissButton = { TextButton(onClick = { onCambiar(hoja.copy(confirmarRetiro = false)) }) { Text("Cancelar") } },
                )
            }
        }
    }
}
