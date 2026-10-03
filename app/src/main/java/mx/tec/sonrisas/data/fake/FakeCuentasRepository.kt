package mx.tec.sonrisas.data.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.CuentasRepository
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.Credenciales
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.contrasenaTemporal
import mx.tec.sonrisas.domain.usuarioSugerido
import java.util.UUID

class FakeCuentasRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val reloj: Reloj,
) : CuentasRepository {

    override fun cuentas(): Flow<List<Usuario>> =
        combine(sesion.usuario, almacen.usuarios) { u, us ->
            when (u?.rol) {
                Rol.FUNDACION -> us.filter { it.rol == Rol.DIRECCION }
                Rol.DIRECCION -> us.filter { it.estanciaId == u.estanciaId && it.rol != Rol.FUNDACION }
                else -> emptyList()
            }.sortedBy { it.nombre }
        }

    override suspend fun crearDireccion(estanciaId: String, nombre: String): Credenciales {
        latencia()
        val u = sesion.usuarioActual()
        if (u?.rol != Rol.FUNDACION) throw AccesoDenegado()
        val estancia = almacen.estancias.value.firstOrNull { it.id == estanciaId } ?: throw AccesoDenegado()
        return crear(u, nombre, Rol.DIRECCION, estancia.id, "Creó la cuenta de dirección de ${nombre.trim()} en ${estancia.nombre}")
    }

    private fun direccion(): Usuario {
        val u = sesion.usuarioActual()
        if (u?.rol != Rol.DIRECCION || u.estanciaId == null) throw AccesoDenegado()
        return u
    }

    private fun salaPropia(u: Usuario, salaId: String) =
        almacen.salas.value.firstOrNull { it.id == salaId && it.estanciaId == u.estanciaId } ?: throw AccesoDenegado()

    private fun educadoraPropia(u: Usuario, usuarioId: String) =
        almacen.usuarios.value.firstOrNull { it.id == usuarioId && it.rol == Rol.EDUCADORA && it.estanciaId == u.estanciaId }
            ?: throw AccesoDenegado()

    override suspend fun crearEducadora(nombre: String, salaId: String): Credenciales {
        latencia()
        val u = direccion()
        val sala = salaPropia(u, salaId)
        return crear(u, nombre, Rol.EDUCADORA, u.estanciaId!!, "Registró a la educadora ${nombre.trim()} en la sala ${sala.nombre}", salaId = sala.id)
    }

    override suspend fun asignarSala(usuarioId: String, salaId: String) {
        latencia()
        val u = direccion()
        val sala = salaPropia(u, salaId)
        val e = educadoraPropia(u, usuarioId)
        if (!e.activo) throw AccesoDenegado()
        almacen.usuarios.update { lista -> lista.map { if (it.id == e.id) it.copy(salaId = sala.id) else it } }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.CUENTA, "Asignó a ${e.nombre} a la sala ${sala.nombre}")
    }

    override suspend fun retirarAcceso(usuarioId: String) {
        latencia()
        val u = direccion()
        val e = educadoraPropia(u, usuarioId)
        almacen.usuarios.update { lista -> lista.map { if (it.id == e.id) it.copy(activo = false) else it } }
        almacen.desbloqueoHuella.update { it - e.id }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.CUENTA, "Retiró el acceso a la app de ${e.nombre}")
    }

    private fun crear(
        autor: Usuario,
        nombre: String,
        rol: Rol,
        estanciaId: String,
        detalle: String,
        salaId: String? = null,
        tutorId: String? = null,
    ): Credenciales {
        if (nombre.isBlank()) throw IllegalArgumentException("Escribe el nombre.")
        val usuario = usuarioSugerido(nombre, almacen.usuarios.value.map { it.usuario })
        val contrasena = contrasenaTemporal()
        val nuevo = Usuario(UUID.randomUUID().toString(), usuario, contrasena, nombre.trim(), rol, estanciaId, salaId, tutorId)
        almacen.usuarios.update { it + nuevo }
        almacen.registrar(autor, reloj.ahora(), AccionRegistro.CUENTA, detalle, estanciaId = estanciaId)
        return Credenciales(nuevo.nombre, usuario, contrasena, rol)
    }
}
