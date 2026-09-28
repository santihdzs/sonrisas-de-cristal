package mx.tec.sonrisas.ui.screens.comun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.Rol

data class CuentaDemo(val usuario: String, val contrasena: String, val nombre: String, val rol: Rol)

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

    val cuentas: List<CuentaDemo> = sesionRepository.cuentasDemo().map { CuentaDemo(it.usuario, it.contrasena, it.nombre, it.rol) }

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

    fun entrarComo(cuenta: CuentaDemo) {
        estado = estado.copy(usuario = cuenta.usuario, contrasena = cuenta.contrasena)
        iniciar(cuenta.usuario, cuenta.contrasena)
    }

    private fun iniciar(usuario: String, contrasena: String) {
        viewModelScope.launch {
            estado = estado.copy(entrando = true, error = null)
            val ok = sesionRepository.iniciar(usuario, contrasena)
            estado = if (ok) LoginUiState() else estado.copy(
                entrando = false,
                error = "Usuario o contraseña incorrectos. Revísalos e inténtalo de nuevo.",
            )
        }
    }
}
