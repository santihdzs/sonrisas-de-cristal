package mx.tec.sonrisas.data.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.Contacto
import mx.tec.sonrisas.data.ContextoChat
import mx.tec.sonrisas.data.idChatSala
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

    override fun conversaciones(): Flow<List<ResumenConversacion>> =
        combine(contexto, almacen.mensajes, almacen.lecturas) { c, mensajes, lecturas ->
            c.visibles().map { c.resumir(it, mensajes, lecturas[c.usuario?.id]?.get(it.id)) }
                // las conversaciones directas sin mensajes no se listan hasta que alguien escribe
                .filter { it.conversacion.tipo == TipoConversacion.SALA || it.ultimo != null }
                .sortedWith(compareByDescending<ResumenConversacion> { it.ultimo?.mensaje?.enviado }.thenBy { it.titulo })
        }

    override fun conversacion(id: String): Flow<ResumenConversacion?> =
        combine(contexto, almacen.mensajes, almacen.lecturas) { c, mensajes, lecturas ->
            c.visibles().firstOrNull { it.id == id }?.let { c.resumir(it, mensajes, lecturas[c.usuario?.id]?.get(it.id)) }
        }

    override fun mensajes(conversacionId: String): Flow<List<MensajeChat>> =
        combine(contexto, almacen.mensajes) { c, mensajes ->
            val conv = c.visibles().firstOrNull { it.id == conversacionId } ?: return@combine emptyList()
            mensajes.filter { it.conversacionId == conversacionId }.sortedBy { it.enviado }.map { c.aChat(conv, it) }
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
