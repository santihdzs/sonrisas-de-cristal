package mx.tec.sonrisas.data

import kotlinx.coroutines.flow.StateFlow
import mx.tec.sonrisas.domain.Usuario

data class Sesion(val usuario: Usuario, val desbloqueoHuella: Boolean = false)

sealed interface SesionEstado {
    data object Cargando : SesionEstado
    data object Anonima : SesionEstado
    data class Activa(val sesion: Sesion) : SesionEstado
}

enum class ResultadoInicio { EXITO, INCORRECTO, SIN_ACCESO }

interface SesionRepository {
    val estado: StateFlow<SesionEstado>

    suspend fun iniciar(usuario: String, contrasena: String): ResultadoInicio

    suspend fun cerrar()

    fun cambiarDesbloqueoHuella(activo: Boolean)
}
