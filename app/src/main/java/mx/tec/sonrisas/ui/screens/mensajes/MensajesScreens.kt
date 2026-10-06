package mx.tec.sonrisas.ui.screens.mensajes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.data.Contacto
import mx.tec.sonrisas.data.MensajeChat
import mx.tec.sonrisas.data.ResumenConversacion
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Conversacion
import mx.tec.sonrisas.domain.Mensaje
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoConversacion
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.GlifoEstrella
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.formatoDiaMes
import mx.tec.sonrisas.ui.components.formatoHora
import mx.tec.sonrisas.ui.components.relativoA
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate
import java.time.LocalDateTime

private fun cuando(m: Mensaje, hoy: LocalDate): String = when (m.enviado.toLocalDate()) {
    hoy -> m.enviado.formatoHora()
    hoy.minusDays(1) -> "Ayer"
    else -> m.enviado.toLocalDate().formatoDiaMes()
}

@Composable
fun ConversacionesScreen(
    datos: ConversacionesDatos,
    onAbrir: (ResumenConversacion) -> Unit,
    onNuevo: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (onNuevo != null) {
                ExtendedFloatingActionButton(onClick = onNuevo, icon = { Icono(R.drawable.ic_edit, null) }, text = { Text("Nuevo mensaje") })
            }
        },
    ) { padding ->
        if (datos.conversaciones.isEmpty()) {
            EstadoVacio(
                R.drawable.ic_chat, "Todavía no tienes conversaciones", Modifier.padding(padding),
                mensaje = "Aquí aparecerán el chat de la sala y tus mensajes directos.",
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = Espacio.space2, bottom = 96.dp),
        ) {
            items(datos.conversaciones, key = { it.conversacion.id }) { c ->
                FilaConversacion(c, datos.hoy, onClick = { onAbrir(c) })
                HorizontalDivider(Modifier.padding(start = 88.dp))
            }
        }
    }
}

@Composable
private fun FilaConversacion(c: ResumenConversacion, hoy: LocalDate, onClick: () -> Unit) {
    val ultimo = c.ultimo
    ListItem(
        headlineContent = { Text(c.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(c.subtitulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when {
                        ultimo == null -> "Sin mensajes todavía"
                        ultimo.propio -> "Tú: ${ultimo.mensaje.texto}"
                        c.conversacion.tipo == TipoConversacion.SALA -> "${ultimo.autor.substringBefore(" · ")}: ${ultimo.mensaje.texto}"
                        else -> ultimo.mensaje.texto
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        leadingContent = {
            val sala = c.sala
            if (c.conversacion.tipo == TipoConversacion.SALA && sala != null) GlifoEstrella(sala.estrella, tamano = 48.dp)
            else AvatarNino(c.titulo.substringBefore(" "), c.titulo.substringAfter(" ", ""), sala?.estrella ?: ColorEstrella.AZUL)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Espacio.space1)) {
                ultimo?.let { Text(cuando(it.mensaje, hoy), style = MaterialTheme.typography.labelMedium) }
                if (c.noLeidos > 0) {
                    Badge(Modifier.semantics { contentDescription = if (c.noLeidos == 1) "1 mensaje sin leer" else "${c.noLeidos} mensajes sin leer" }) {
                        Text("${c.noLeidos}")
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun NuevoMensajeScreen(contactos: List<Contacto>, abriendo: Boolean, onElegir: (Contacto) -> Unit, modifier: Modifier = Modifier) {
    if (contactos.isEmpty()) {
        EstadoVacio(
            R.drawable.ic_person, "No hay con quién iniciar una conversación", modifier,
            mensaje = "Puedes escribir a las educadoras de la sala de tus hijos o, si eres educadora, a las familias de tu sala que tienen acceso a la app.",
        )
        return
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = Espacio.space2)) {
        items(contactos, key = { it.usuarioId + it.sala.id }) { c ->
            ListItem(
                headlineContent = { Text(c.nombre, style = MaterialTheme.typography.titleMedium) },
                supportingContent = { Text(c.etiqueta) },
                leadingContent = { AvatarNino(c.nombre.substringBefore(" "), c.nombre.substringAfter(" ", ""), c.sala.estrella) },
                trailingContent = { Icono(R.drawable.ic_chevron_right, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.clickable(enabled = !abriendo) { onElegir(c) },
            )
        }
    }
}

private sealed interface ElementoChat {
    val clave: String

    data class Dia(val fecha: LocalDate) : ElementoChat {
        override val clave = "dia-$fecha"
    }

    data class Burbuja(val m: MensajeChat, val conAutor: Boolean) : ElementoChat {
        override val clave = m.mensaje.id
    }
}

// la lista va invertida para quedarse pegada al mensaje mas nuevo, asi que se arma del mas nuevo al mas viejo
private fun elementos(mensajes: List<MensajeChat>): List<ElementoChat> {
    val salida = mutableListOf<ElementoChat>()
    mensajes.forEachIndexed { i, m ->
        val anterior = mensajes.getOrNull(i - 1)
        val nuevoDia = anterior == null || anterior.mensaje.enviado.toLocalDate() != m.mensaje.enviado.toLocalDate()
        if (nuevoDia) salida += ElementoChat.Dia(m.mensaje.enviado.toLocalDate())
        salida += ElementoChat.Burbuja(m, conAutor = !m.propio && (nuevoDia || anterior?.mensaje?.autorId != m.mensaje.autorId))
    }
    return salida.asReversed()
}

@Composable
fun ChatScreen(
    datos: ChatDatos,
    borrador: String,
    enviando: Boolean,
    onBorrador: (String) -> Unit,
    onEnviar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (datos.resumen == null) {
        EstadoVacio(
            R.drawable.ic_chat, "Ya no participas en esta conversación", modifier,
            mensaje = "Puede pasar si el niño cambió de sala o si la dirección actualizó tu acceso.",
        )
        return
    }
    Column(
        modifier
            .fillMaxSize()
            // el scaffold ya dejo el espacio de la barra de navegacion; el teclado solo agrega lo que falta
            .consumeWindowInsets(WindowInsets.navigationBars)
            .imePadding(),
    ) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val ancho = maxWidth * 0.8f
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = Espacio.space4, vertical = Espacio.space3),
                verticalArrangement = Arrangement.spacedBy(Espacio.space2),
            ) {
                items(elementos(datos.mensajes), key = { it.clave }) { e ->
                    when (e) {
                        is ElementoChat.Dia -> Text(
                            e.fecha.relativoA(datos.hoy),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.space2),
                        )
                        is ElementoChat.Burbuja -> Burbuja(e.m, e.conAutor, ancho)
                    }
                }
                if (datos.mensajes.isEmpty()) {
                    item {
                        EstadoVacio(R.drawable.ic_chat, "Todavía no hay mensajes", mensaje = "Escribe el primero.")
                    }
                }
                if (datos.resumen.conversacion.tipo == TipoConversacion.SALA) {
                    item {
                        Text(
                            "En este chat participan las educadoras de la sala, la dirección y las familias con acceso a la app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = Espacio.space2),
                        )
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Espacio.space3, vertical = Espacio.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.space2),
            ) {
                OutlinedTextField(
                    value = borrador,
                    onValueChange = onBorrador,
                    placeholder = { Text("Escribe un mensaje") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(onClick = onEnviar, enabled = borrador.isNotBlank() && !enviando, modifier = Modifier.size(48.dp)) {
                    Icono(R.drawable.ic_send, "Enviar mensaje")
                }
            }
        }
    }
}

@Composable
private fun Burbuja(m: MensajeChat, conAutor: Boolean, anchoMaximo: androidx.compose.ui.unit.Dp) {
    val esquema = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.propio) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = anchoMaximo).semantics(mergeDescendants = true) {},
            horizontalAlignment = if (m.propio) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(Espacio.space1),
        ) {
            if (conAutor) Text(m.autor, style = MaterialTheme.typography.labelMedium, color = esquema.onSurfaceVariant)
            Surface(
                color = if (m.propio) esquema.primaryContainer else SonrisasTheme.colors.superficieElevada,
                contentColor = if (m.propio) esquema.onPrimaryContainer else esquema.onSurface,
                shape = MaterialTheme.shapes.medium,
                border = if (m.propio) null else androidx.compose.foundation.BorderStroke(1.dp, esquema.outlineVariant),
            ) {
                Text(m.mensaje.texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = Espacio.space3, vertical = Espacio.space2))
            }
            Text(
                if (m.propio) "Tú, ${m.mensaje.enviado.formatoHora()}" else m.mensaje.enviado.formatoHora(),
                style = MaterialTheme.typography.labelSmall,
                color = esquema.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatPreview() {
    val hoy = LocalDate.of(2026, 10, 12)
    val sala = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
    val conv = Conversacion("c", TipoConversacion.SALA, "s")
    val mensajes = listOf(
        MensajeChat(Mensaje("1", "c", "p", "¿Los viernes también llevan uniforme?", LocalDateTime.of(hoy.minusDays(1), java.time.LocalTime.of(8, 40))), "Paola · mamá de Diego", false),
        MensajeChat(Mensaje("2", "c", "m", "Los viernes pueden venir con ropa cómoda.", LocalDateTime.of(hoy, java.time.LocalTime.of(9, 15))), "Mariana · educadora", true),
    )
    SonrisasTheme {
        ChatScreen(ChatDatos(ResumenConversacion(conv, "Sala Maternales", "Chat de la sala", sala, mensajes.last(), 0), mensajes, hoy), "", false, {}, {})
    }
}
