package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.data.SesionRepository

const val MINUTOS_INACTIVIDAD = 5L

class SesionViewModel(private val sesionRepository: SesionRepository) : ViewModel() {

    val estado: StateFlow<SesionEstado> = sesionRepository.estado

    var bloqueada by mutableStateOf(false)
        private set

    private var ultimaInteraccion = System.currentTimeMillis()

    private val huellaActiva: Boolean
        get() = (estado.value as? SesionEstado.Activa)?.sesion?.desbloqueoHuella == true

    init {
        // revisa cada 15 segundos si ya pasaron 5 minutos sin tocar la pantalla (RF-13)
        viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                if (huellaActiva && System.currentTimeMillis() - ultimaInteraccion > MINUTOS_INACTIVIDAD * 60_000) {
                    bloqueada = true
                }
            }
        }
    }

    fun registrarInteraccion() {
        ultimaInteraccion = System.currentTimeMillis()
    }

    fun alIrASegundoPlano() {
        if (huellaActiva) bloqueada = true
    }

    fun desbloquear() {
        bloqueada = false
        registrarInteraccion()
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            bloqueada = false
            sesionRepository.cerrar()
        }
    }
}
