package mx.tec.sonrisas.ui.screens.comun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.ResultadoInicio
import mx.tec.sonrisas.data.SesionRepository

data class LoginUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val mostrarContrasena: Boolean = false,
    val entrando: Boolean = false,
    val error: String? = null,
) {
    val puedeEntrar: Boolean get() = usuario.isNotBlank() && contrasena.isNotBlank() && !entrando
}

class LoginViewModel(private val sesionRepository: SesionRepository) : ViewModel() {

    var estado by mutableStateOf(LoginUiState())
        private set

    fun cambiarUsuario(texto: String) {
        estado = estado.copy(usuario = texto, error = null)
    }

    fun cambiarContrasena(texto: String) {
        estado = estado.copy(contrasena = texto, error = null)
    }

    fun alternarMostrar() {
        estado = estado.copy(mostrarContrasena = !estado.mostrarContrasena)
    }

    fun entrar() {
        if (!estado.puedeEntrar) return
        iniciar(estado.usuario, estado.contrasena)
    }

    private fun iniciar(usuario: String, contrasena: String) {
        viewModelScope.launch {
            estado = estado.copy(entrando = true, error = null)
            estado = when (sesionRepository.iniciar(usuario, contrasena)) {
                ResultadoInicio.EXITO -> LoginUiState()
                ResultadoInicio.INCORRECTO -> estado.copy(
                    entrando = false,
                    error = "Usuario o contraseña incorrectos. Revísalos e inténtalo de nuevo.",
                )
                ResultadoInicio.SIN_ACCESO -> estado.copy(
                    entrando = false,
                    error = "Tu acceso a la app fue retirado. Si crees que es un error, habla con la dirección de tu estancia.",
                )
            }
        }
    }
}
