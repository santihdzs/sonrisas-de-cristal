package mx.tec.sonrisas.data.fake

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.AlmacenArchivos
import mx.tec.sonrisas.data.ArchivoEntrada
import mx.tec.sonrisas.data.MaterialRepository
import mx.tec.sonrisas.data.tipoMaterialPermitido
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.MaterialClase
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.Visibilidad
import java.io.File
import java.util.UUID

class FakeMaterialRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val archivos: AlmacenArchivos,
    private val reloj: Reloj,
) : MaterialRepository {

    override fun material(salaId: String): Flow<List<MaterialClase>> =
        combine(sesion.usuario, almacen.salas, almacen.ninos, almacen.material) { u, salas, ninos, material ->
            if (salaId !in Visibilidad.salas(u, salas, ninos)) emptyList()
            else material.filter { it.salaId == salaId }.sortedByDescending { it.subidoEl }
        }

    private fun personalDe(salaId: String): Pair<Usuario, Sala> {
        val u = sesion.usuarioActual()
        val s = almacen.salas.value.firstOrNull { it.id == salaId }
        if (!Visibilidad.esPersonalDe(u, s)) throw AccesoDenegado()
        return u!! to s!!
    }

    // la educadora solo cambia lo que ella subio; la direccion puede corregir cualquier material de su estancia
    private fun editable(id: String): Triple<Usuario, Sala, MaterialClase> {
        val m = almacen.material.value.firstOrNull { it.id == id } ?: throw AccesoDenegado()
        val (u, s) = personalDe(m.salaId)
        if (u.rol != Rol.DIRECCION && m.subidoPorId != u.id) throw AccesoDenegado()
        return Triple(u, s, m)
    }

    override suspend fun subir(salaId: String, titulo: String, descripcion: String, archivo: ArchivoEntrada): MaterialClase {
        latencia()
        val (u, s) = personalDe(salaId)
        if (titulo.isBlank()) throw IllegalArgumentException("Escribe el título.")
        if (!tipoMaterialPermitido(archivo.tipo)) throw IllegalArgumentException("Tipo de archivo no permitido.")
        val id = UUID.randomUUID().toString()
        val ruta = withContext(Dispatchers.IO) { archivos.guardar("material", id, archivo) }
        val nuevo = MaterialClase(
            id, s.id, titulo.trim(), descripcion.trim(), archivo.nombre, archivo.tipo, File(ruta).length(), ruta, u.id, reloj.ahora(),
        )
        almacen.material.update { listOf(nuevo) + it }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.MATERIAL, "Subió el material \"${nuevo.titulo}\" a la sala ${s.nombre}", estanciaId = s.estanciaId)
        return nuevo
    }

    override suspend fun editar(id: String, titulo: String, descripcion: String) {
        latencia()
        val (u, s, m) = editable(id)
        if (titulo.isBlank()) throw IllegalArgumentException("Escribe el título.")
        almacen.material.update { lista -> lista.map { if (it.id == id) it.copy(titulo = titulo.trim(), descripcion = descripcion.trim()) else it } }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.MATERIAL, "Editó el material \"${m.titulo}\" de la sala ${s.nombre}", estanciaId = s.estanciaId)
    }

    override suspend fun borrar(id: String) {
        latencia()
        val (u, s, m) = editable(id)
        almacen.material.update { lista -> lista.filterNot { it.id == id } }
        // el archivo se borra del almacenamiento, no solo de la lista
        withContext(Dispatchers.IO) { archivos.borrar(m.ruta) }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.MATERIAL, "Borró el material \"${m.titulo}\" de la sala ${s.nombre}", estanciaId = s.estanciaId)
    }
}
