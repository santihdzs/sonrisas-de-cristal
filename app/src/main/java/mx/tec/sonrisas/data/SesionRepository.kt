package mx.tec.sonrisas.data

import kotlinx.coroutines.flow.StateFlow
import mx.tec.sonrisas.domain.Usuario

data class Sesion(val usuario: Usuario, val desbloqueoHuella: Boolean = false)

sealed interface SesionEstado {
    data object Cargando : SesionEstado
    data object Anonima : SesionEstado
    data class Activa(val sesion: Sesion) : SesionEstado
}

interface SesionRepository {
    val estado: StateFlow<SesionEstado>

    /** devuelve false si el usuario o la contrasena no coinciden */
    suspend fun iniciar(usuario: String, contrasena: String): Boolean

    suspend fun cerrar()

    fun cambiarDesbloqueoHuella(activo: Boolean)
}
