package mx.tec.sonrisas.data.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.Contacto
import mx.tec.sonrisas.data.LIMITE_MENSAJE
import mx.tec.sonrisas.data.MensajeChat
import mx.tec.sonrisas.data.MensajesRepository
import mx.tec.sonrisas.data.ResumenConversacion
import mx.tec.sonrisas.domain.Conversacion
import mx.tec.sonrisas.domain.Mensaje
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoConversacion
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.domain.Usuario
import java.time.LocalDateTime
import java.util.UUID

fun idChatSala(salaId: String) = "sala-$salaId"

/** foto del estado necesario para decidir quien pertenece a que conversacion */
internal data class ContextoChat(
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
}

class FakeMensajesRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val reloj: Reloj,
) : MensajesRepository {

    private val contexto: Flow<ContextoChat> =
        combine(
            combine(sesion.usuario, almacen.salas, almacen.ninos, almacen.usuarios, almacen.tutores) { u, s, n, us, t ->
                ContextoChat(u, s, n, us, t, emptyList())
            },
            almacen.conversaciones,
        ) { c, directas -> c.copy(directas = directas) }

    private fun contextoActual() = ContextoChat(
        sesion.usuarioActual(), almacen.salas.value, almacen.ninos.value, almacen.usuarios.value, almacen.tutores.value, almacen.conversaciones.value,
    )

    private fun aChat(c: ContextoChat, conv: Conversacion, m: Mensaje) = MensajeChat(m, c.etiqueta(m.autorId, conv.salaId), m.autorId == c.usuario?.id)

    private fun resumir(
        c: ContextoChat,
        conv: Conversacion,
        mensajes: List<Mensaje>,
        lecturas: Map<String, Map<String, LocalDateTime>>,
    ): ResumenConversacion {
        val u = c.usuario!!
        val sala = c.salas.firstOrNull { it.id == conv.salaId }
        val propios = mensajes.filter { it.conversacionId == conv.id }
        val leido = lecturas[u.id]?.get(conv.id)
        val (titulo, subtitulo) = when (conv.tipo) {
            TipoConversacion.SALA -> "Sala ${sala?.nombre.orEmpty()}" to "Chat de la sala"
            TipoConversacion.DIRECTA -> {
                val otro = conv.participantes.firstOrNull { it != u.id }.orEmpty()
                val persona = c.usuarios.firstOrNull { it.id == otro }
                val detalle = if (persona?.rol == Rol.EDUCADORA) "Educadora de la sala ${sala?.nombre.orEmpty()}"
                else c.etiqueta(otro, conv.salaId).substringAfter(" · ").replaceFirstChar { it.uppercase() }
                persona?.nombre.orEmpty() to detalle
            }
        }
        return ResumenConversacion(
            conversacion = conv,
            titulo = titulo,
            subtitulo = subtitulo,
            sala = sala,
            ultimo = propios.maxByOrNull { it.enviado }?.let { aChat(c, conv, it) },
            noLeidos = propios.count { it.autorId != u.id && (leido == null || it.enviado.isAfter(leido)) },
        )
    }

    override fun conversaciones(): Flow<List<ResumenConversacion>> =
        combine(contexto, almacen.mensajes, almacen.lecturas) { c, mensajes, lecturas ->
            c.visibles().map { resumir(c, it, mensajes, lecturas) }
                // las conversaciones directas sin mensajes no se listan hasta que alguien escribe
                .filter { it.conversacion.tipo == TipoConversacion.SALA || it.ultimo != null }
                .sortedWith(compareByDescending<ResumenConversacion> { it.ultimo?.mensaje?.enviado }.thenBy { it.titulo })
        }

    override fun conversacion(id: String): Flow<ResumenConversacion?> =
        combine(contexto, almacen.mensajes, almacen.lecturas) { c, mensajes, lecturas ->
            c.visibles().firstOrNull { it.id == id }?.let { resumir(c, it, mensajes, lecturas) }
        }

    override fun mensajes(conversacionId: String): Flow<List<MensajeChat>> =
        combine(contexto, almacen.mensajes) { c, mensajes ->
            val conv = c.visibles().firstOrNull { it.id == conversacionId } ?: return@combine emptyList()
            mensajes.filter { it.conversacionId == conversacionId }.sortedBy { it.enviado }.map { aChat(c, conv, it) }
        }

    override fun noLeidos(): Flow<Int> = conversaciones().map { l -> l.sumOf { it.noLeidos } }.distinctUntilChanged()

    override suspend fun enviar(conversacionId: String, texto: String) {
        latencia()
        val c = contextoActual()
        val u = c.usuario ?: throw AccesoDenegado()
        if (c.visibles().none { it.id == conversacionId }) throw AccesoDenegado()
        val limpio = texto.trim()
        if (limpio.isBlank() || limpio.length > LIMITE_MENSAJE) throw IllegalArgumentException("Revisa el mensaje.")
        val ahora = reloj.ahora()
        almacen.mensajes.update { it + Mensaje(UUID.randomUUID().toString(), conversacionId, u.id, limpio, ahora) }
        marcar(u, conversacionId, ahora)
    }

    override suspend fun marcarLeida(conversacionId: String) {
        val c = contextoActual()
        val u = c.usuario ?: return
        if (c.visibles().none { it.id == conversacionId }) return
        marcar(u, conversacionId, reloj.ahora())
    }

    override fun contactos(): Flow<List<Contacto>> = contexto.map { it.contactos() }

    override suspend fun abrirDirecta(usuarioId: String, salaId: String): String {
        val c = contextoActual()
        val u = c.usuario ?: throw AccesoDenegado()
        if (c.contactos().none { it.usuarioId == usuarioId && it.sala.id == salaId }) throw AccesoDenegado()
        val participantes = setOf(u.id, usuarioId)
        c.directas.firstOrNull { it.salaId == salaId && it.participantes.toSet() == participantes }?.let { return it.id }
        val nueva = Conversacion(UUID.randomUUID().toString(), TipoConversacion.DIRECTA, salaId, participantes.toList())
        almacen.conversaciones.update { it + nueva }
        return nueva.id
    }

    private fun marcar(u: Usuario, conversacionId: String, cuando: LocalDateTime) {
        almacen.lecturas.update { mapa -> mapa + (u.id to (mapa[u.id].orEmpty() + (conversacionId to cuando))) }
    }
}
