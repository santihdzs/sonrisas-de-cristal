package mx.tec.sonrisas.data.fake

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.AlmacenArchivos
import mx.tec.sonrisas.data.ArchivoEntrada
import mx.tec.sonrisas.data.ObjetosPerdidosRepository
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.EstadoObjeto
import mx.tec.sonrisas.domain.ObjetoPerdido
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.Usuario
import java.time.LocalDate
import java.util.UUID

class FakeObjetosRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val archivos: AlmacenArchivos,
    private val reloj: Reloj,
) : ObjetosPerdidosRepository {

    override fun objetos(salaId: String): Flow<List<ObjetoPerdido>> =
        combine(sesion.usuario, almacen.salas, almacen.ninos, almacen.objetos) { u, salas, ninos, objetos ->
            if (salaId !in Visibilidad.salas(u, salas, ninos)) return@combine emptyList()
            objetos.filter { it.salaId == salaId }
                .map { if (u?.rol == Rol.FAMILIAR && it.reclamadoPorId != u.id) it.copy(reclamadoPor = null, notaReclamo = null) else it }
                .sortedByDescending { it.publicadoEl }
        }

    override fun vistos(): Flow<Set<String>> =
        combine(sesion.usuario, almacen.objetosVistos) { u, v -> if (u == null) emptySet() else v[u.id].orEmpty() }

    override suspend fun marcarVistos(ids: Collection<String>) {
        val u = sesion.usuarioActual() ?: return
        almacen.objetosVistos.update { mapa -> mapa + (u.id to (mapa[u.id].orEmpty() + ids)) }
    }

    private fun sala(id: String): Sala? = almacen.salas.value.firstOrNull { it.id == id }

    private fun personalDe(salaId: String): Pair<Usuario, Sala> {
        val u = sesion.usuarioActual()
        val s = sala(salaId)
        if (!Visibilidad.esPersonalDe(u, s)) throw AccesoDenegado()
        return u!! to s!!
    }

    private fun objeto(id: String): ObjetoPerdido = almacen.objetos.value.firstOrNull { it.id == id } ?: throw AccesoDenegado()

    private fun actualizar(id: String, cambio: (ObjetoPerdido) -> ObjetoPerdido) {
        almacen.objetos.update { lista -> lista.map { if (it.id == id) cambio(it) else it } }
    }

    override suspend fun publicar(
        salaId: String,
        descripcion: String,
        dondeSeEncontro: String,
        fecha: LocalDate,
        foto: ArchivoEntrada?,
    ): ObjetoPerdido {
        latencia()
        val (u, s) = personalDe(salaId)
        if (descripcion.isBlank() || dondeSeEncontro.isBlank()) throw IllegalArgumentException("Faltan datos del objeto.")
        if (fecha.isAfter(reloj.hoy())) throw IllegalArgumentException("La fecha no puede ser futura.")
        val id = UUID.randomUUID().toString()
        val ruta = foto?.let { withContext(Dispatchers.IO) { archivos.guardar("objetos", id, it) } }
        val nuevo = ObjetoPerdido(id, s.id, descripcion.trim(), dondeSeEncontro.trim(), fecha, ruta, u.id, reloj.ahora())
        almacen.objetos.update { listOf(nuevo) + it }
        // quien lo publica no lo ve como nuevo
        marcarVistos(listOf(id))
        almacen.registrar(u, reloj.ahora(), AccionRegistro.OBJETO, "Publicó el objeto perdido \"${nuevo.descripcion}\" en la sala ${s.nombre}", estanciaId = s.estanciaId)
        return nuevo
    }

    override suspend fun reclamar(id: String, nota: String?) {
        latencia()
        val u = sesion.usuarioActual()
        if (u?.rol != Rol.FAMILIAR) throw AccesoDenegado()
        val o = objeto(id)
        val hijos = Visibilidad.ninos(u, almacen.ninos.value).filter { it.salaId == o.salaId }
        if (hijos.isEmpty()) throw AccesoDenegado()
        if (o.estado != EstadoObjeto.DISPONIBLE) throw IllegalStateException("Alguien más ya reclamó este objeto.")
        val parentesco = almacen.tutores.value.firstOrNull { it.id == u.tutorId }?.parentesco?.lowercase() ?: "familiar"
        val quien = "${u.nombre} ($parentesco de ${hijos.joinToString(" y ") { it.nombre }})"
        val limpia = nota?.trim()?.takeIf { it.isNotBlank() }
        actualizar(id) { it.copy(estado = EstadoObjeto.RECLAMADO, reclamadoPorId = u.id, reclamadoPor = quien, notaReclamo = limpia, reclamadoEl = reloj.ahora()) }
        almacen.registrar(
            u, reloj.ahora(), AccionRegistro.OBJETO, "Reclamó el objeto perdido \"${o.descripcion}\"", hijos.first().id,
            estanciaId = sala(o.salaId)?.estanciaId,
        )
    }

    override suspend fun entregar(id: String) {
        latencia()
        val o = objeto(id)
        val (u, s) = personalDe(o.salaId)
        if (o.estado != EstadoObjeto.RECLAMADO) throw IllegalStateException("Solo se entrega un objeto reclamado.")
        actualizar(id) { it.copy(estado = EstadoObjeto.ENTREGADO, entregadoEl = reloj.ahora()) }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.OBJETO, "Entregó el objeto perdido \"${o.descripcion}\" a ${o.reclamadoPor}", estanciaId = s.estanciaId)
    }

    override suspend fun rechazarReclamo(id: String) {
        latencia()
        val o = objeto(id)
        val (u, s) = personalDe(o.salaId)
        if (o.estado != EstadoObjeto.RECLAMADO) return
        actualizar(id) { it.copy(estado = EstadoObjeto.DISPONIBLE, reclamadoPorId = null, reclamadoPor = null, notaReclamo = null, reclamadoEl = null) }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.OBJETO, "Rechazó el reclamo de ${o.reclamadoPor} sobre \"${o.descripcion}\"", estanciaId = s.estanciaId)
    }
}
