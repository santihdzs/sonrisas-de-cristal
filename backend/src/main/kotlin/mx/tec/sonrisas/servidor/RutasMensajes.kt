package mx.tec.sonrisas.servidor

import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mx.tec.sonrisas.data.AbrirDirecta
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.ContextoChat
import mx.tec.sonrisas.data.Id
import mx.tec.sonrisas.data.LIMITE_MENSAJE
import mx.tec.sonrisas.data.Numero
import mx.tec.sonrisas.data.ResumenConversacion
import mx.tec.sonrisas.data.Texto
import mx.tec.sonrisas.domain.Conversacion
import mx.tec.sonrisas.domain.Mensaje
import mx.tec.sonrisas.domain.TipoConversacion
import java.time.LocalDateTime

// MensajesRepository; quien pertenece a cada conversacion se recalcula en cada peticion (ContextoChat)

private fun Ctx.contexto(): ContextoChat {
    val salas = salasEstancia()
    val ninos = ninosEstancia()
    val usuarios = u.estanciaId?.let { Tablas.usuarios.donde("estancia_id = ?", it) }.orEmpty()
    val tutores = Tablas.tutores.en("id", ninos.flatMap { it.tutorIds }.toSet())
    val directas = Tablas.conversaciones.en("sala_id", salas.map { it.id })
    return ContextoChat(u, salas, ninos, usuarios, tutores, directas)
}

private fun Ctx.leidoHasta(conversacionId: String): LocalDateTime? =
    consultar("SELECT hasta FROM lecturas WHERE usuario_id = ? AND conversacion_id = ?", u.id, conversacionId) {
        LocalDateTime.parse(it.getString(1))
    }.firstOrNull()

// solo se cargan el ultimo mensaje y el conteo de no leidos, no toda la conversacion
private fun Ctx.resumen(c: ContextoChat, conv: Conversacion): ResumenConversacion {
    val ultimo = Tablas.mensajes.donde("conversacion_id = ? ORDER BY enviado DESC LIMIT 1", conv.id)
    val leido = leidoHasta(conv.id)
    val noLeidos = if (leido == null) {
        consultar("SELECT COUNT(*) FROM mensajes WHERE conversacion_id = ? AND autor_id <> ?", conv.id, u.id) { it.getInt(1) }
    } else {
        consultar(
            "SELECT COUNT(*) FROM mensajes WHERE conversacion_id = ? AND autor_id <> ? AND enviado > ?", conv.id, u.id, orden(leido),
        ) { it.getInt(1) }
    }.first()
    return c.resumir(conv, ultimo, leido).copy(noLeidos = noLeidos)
}

private fun Ctx.conversaciones(): List<ResumenConversacion> {
    val c = contexto()
    return c.visibles().map { resumen(c, it) }
        // las conversaciones directas sin mensajes no se listan hasta que alguien escribe
        .filter { it.conversacion.tipo == TipoConversacion.SALA || it.ultimo != null }
        .sortedWith(compareByDescending<ResumenConversacion> { it.ultimo?.mensaje?.enviado }.thenBy { it.titulo })
}

private fun Ctx.marcarLeida(conversacionId: String, cuando: LocalDateTime) {
    ejecutar("DELETE FROM lecturas WHERE usuario_id = ? AND conversacion_id = ?", u.id, conversacionId)
    ejecutar("INSERT INTO lecturas (usuario_id, conversacion_id, hasta) VALUES (?, ?, ?)", u.id, conversacionId, orden(cuando))
}

fun Route.rutasMensajes(db: Db) {
    get("/conversaciones") { lee(db) { conversaciones() } }

    get("/conversaciones/{id}") {
        lee(db) {
            val c = contexto()
            c.visibles().firstOrNull { it.id == call.parameters["id"] }?.let { resumen(c, it) }
        }
    }

    get("/conversaciones/{id}/mensajes") {
        lee(db) {
            val c = contexto()
            val conv = c.visibles().firstOrNull { it.id == call.parameters["id"] } ?: return@lee emptyList()
            Tablas.mensajes.donde("conversacion_id = ? ORDER BY enviado", conv.id).map { c.aChat(conv, it) }
        }
    }

    get("/mensajes/no-leidos") { lee(db) { Numero(conversaciones().sumOf { it.noLeidos }) } }

    post("/conversaciones/{id}/mensajes") {
        val texto = call.receive<Texto>().texto.orEmpty().trim()
        escribe(db) {
            val id = call.parameters["id"]!!
            if (contexto().visibles().none { it.id == id }) throw AccesoDenegado()
            if (texto.isBlank() || texto.length > LIMITE_MENSAJE) throw IllegalArgumentException("Revisa el mensaje.")
            Tablas.mensajes.guardar(Mensaje(nuevoId(), id, u.id, texto, ahora))
            marcarLeida(id, ahora)
        }
    }

    post("/conversaciones/{id}/leida") {
        escribe(db) {
            val id = call.parameters["id"]!!
            if (contexto().visibles().any { it.id == id }) marcarLeida(id, ahora)
        }
    }

    get("/contactos") { lee(db) { contexto().contactos() } }

    post("/directas") {
        val p = call.receive<AbrirDirecta>()
        escribe(db) {
            val c = contexto()
            if (c.contactos().none { it.usuarioId == p.usuarioId && it.sala.id == p.salaId }) throw AccesoDenegado()
            val participantes = setOf(u.id, p.usuarioId)
            val existente = c.directas.firstOrNull { it.salaId == p.salaId && it.participantes.toSet() == participantes }
            Id(existente?.id ?: Conversacion(nuevoId(), TipoConversacion.DIRECTA, p.salaId, participantes.toList()).also { Tablas.conversaciones.guardar(it) }.id)
        }
    }
}
