package mx.tec.sonrisas.data

import mx.tec.sonrisas.domain.Conversacion
import mx.tec.sonrisas.domain.Mensaje
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoConversacion
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.domain.Usuario
import java.time.LocalDateTime

fun idChatSala(salaId: String) = "sala-$salaId"

/** foto del estado necesario para decidir quien pertenece a que conversacion */
data class ContextoChat(
    val usuario: Usuario?,
    val salas: List<Sala>,
    val ninos: List<Nino>,
    val usuarios: List<Usuario>,
    val tutores: List<Tutor>,
    val directas: List<Conversacion>,
) {
    private fun hijosEn(u: Usuario, salaId: String) =
        ninos.filter { it.activo && it.salaId == salaId && u.tutorId != null && u.tutorId in it.tutorIds }

    fun miembrosSala(sala: Sala): Set<String> = usuarios.filter { u ->
        u.activo && when (u.rol) {
            Rol.EDUCADORA -> u.salaId == sala.id
            Rol.DIRECCION -> u.estanciaId == sala.estanciaId
            Rol.FAMILIAR -> hijosEn(u, sala.id).isNotEmpty()
            Rol.FUNDACION -> false
        }
    }.map { it.id }.toSet()

    // la relacion se vuelve a revisar siempre: si el nino cambia de sala o alguien pierde acceso, el chat desaparece
    fun directaVigente(c: Conversacion): Boolean {
        val personas = c.participantes.mapNotNull { id -> usuarios.firstOrNull { it.id == id && it.activo } }
        if (personas.size != 2) return false
        val educadora = personas.firstOrNull { it.rol == Rol.EDUCADORA } ?: return false
        val tutor = personas.firstOrNull { it.rol == Rol.FAMILIAR } ?: return false
        return educadora.salaId == c.salaId && hijosEn(tutor, c.salaId).isNotEmpty()
    }

    fun contactos(): List<Contacto> {
        val u = usuario ?: return emptyList()
        return when (u.rol) {
            Rol.FAMILIAR -> salas.filter { hijosEn(u, it.id).isNotEmpty() }.flatMap { sala ->
                usuarios.filter { it.activo && it.rol == Rol.EDUCADORA && it.salaId == sala.id }
                    .map { Contacto(it.id, it.nombre, "Educadora de la sala ${sala.nombre}", sala) }
            }
            Rol.EDUCADORA -> {
                val sala = salas.firstOrNull { it.id == u.salaId } ?: return emptyList()
                usuarios.filter { it.activo && it.rol == Rol.FAMILIAR && hijosEn(it, sala.id).isNotEmpty() }
                    .map { Contacto(it.id, it.nombre, etiqueta(it.id, sala.id).substringAfter(" · ").replaceFirstChar { c -> c.uppercase() }, sala) }
            }
            else -> emptyList()
        }.sortedBy { it.nombre }
    }

    fun visibles(): List<Conversacion> {
        val u = usuario ?: return emptyList()
        val deSala = salas.filter { u.id in miembrosSala(it) }.map { Conversacion(idChatSala(it.id), TipoConversacion.SALA, it.id) }
        return deSala + directas.filter { u.id in it.participantes && directaVigente(it) }
    }

    fun etiqueta(autorId: String, salaId: String): String {
        val a = usuarios.firstOrNull { it.id == autorId } ?: return "Persona sin acceso"
        val nombre = a.nombre.substringBefore(" ")
        return when (a.rol) {
            Rol.EDUCADORA -> "$nombre · educadora"
            Rol.DIRECCION -> "$nombre · dirección"
            Rol.FUNDACION -> "$nombre · fundación"
            Rol.FAMILIAR -> {
                val parentesco = tutores.firstOrNull { it.id == a.tutorId }?.parentesco?.lowercase() ?: "familiar"
                val hijos = hijosEn(a, salaId).map { it.nombre }
                if (hijos.isEmpty()) "$nombre · familia" else "$nombre · $parentesco de ${hijos.joinToString(" y ")}"
            }
        }
    }

    fun aChat(conv: Conversacion, m: Mensaje) = MensajeChat(m, etiqueta(m.autorId, conv.salaId), m.autorId == usuario?.id)

    /** mensajes puede traer los de otras conversaciones; leido es hasta cuando leyo el usuario */
    fun resumir(conv: Conversacion, mensajes: List<Mensaje>, leido: LocalDateTime?): ResumenConversacion {
        val u = usuario!!
        val sala = salas.firstOrNull { it.id == conv.salaId }
        val propios = mensajes.filter { it.conversacionId == conv.id }
        val (titulo, subtitulo) = when (conv.tipo) {
            TipoConversacion.SALA -> "Sala ${sala?.nombre.orEmpty()}" to "Chat de la sala"
            TipoConversacion.DIRECTA -> {
                val otro = conv.participantes.firstOrNull { it != u.id }.orEmpty()
                val persona = usuarios.firstOrNull { it.id == otro }
                val detalle = if (persona?.rol == Rol.EDUCADORA) "Educadora de la sala ${sala?.nombre.orEmpty()}"
                else etiqueta(otro, conv.salaId).substringAfter(" · ").replaceFirstChar { it.uppercase() }
                persona?.nombre.orEmpty() to detalle
            }
        }
        return ResumenConversacion(
            conversacion = conv,
            titulo = titulo,
            subtitulo = subtitulo,
            sala = sala,
            ultimo = propios.maxByOrNull { it.enviado }?.let { aChat(conv, it) },
            noLeidos = propios.count { it.autorId != u.id && (leido == null || it.enviado.isAfter(leido)) },
        )
    }
}
