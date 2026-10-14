package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.ColorEstrella

const val MINUTOS_INACTIVIDAD = 5L

data class ContextoBloqueo(val titulo: String, val estrella: ColorEstrella?, val nombre: String)

class SesionViewModel(
    private val sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
) : ViewModel() {

    val estado: StateFlow<SesionEstado> = sesionRepository.estado

    var bloqueada by mutableStateOf(false)
        private set

    var mensajeBloqueo by mutableStateOf<String?>(null)
        private set

    /** en la pantalla de bloqueo se muestra la sala (o la estancia) de quien tiene la sesion */
    val contextoBloqueo: StateFlow<ContextoBloqueo?> =
        combine(estado, estanciasRepository.salas(), estanciasRepository.estancias()) { e, salas, estancias ->
            val u = (e as? SesionEstado.Activa)?.sesion?.usuario ?: return@combine null
            val sala = salas.firstOrNull { it.id == u.salaId }
            ContextoBloqueo(
                titulo = sala?.let { "Sala ${it.nombre}" } ?: estancias.firstOrNull { it.id == u.estanciaId }?.nombre ?: "Sonrisas de Cristal",
                estrella = sala?.estrella,
                nombre = u.nombre,
            )
        }.reintentando().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // los viewmodels de cada sesion viven aqui y no en un remember, para que sobrevivan a la rotacion
    private var tiendaDe: String? = null
    private var tienda = ViewModelStore()

    fun tiendaPara(usuarioId: String): ViewModelStore {
        if (tiendaDe != usuarioId) {
            tienda.clear()
            tienda = ViewModelStore()
            tiendaDe = usuarioId
        }
        return tienda
    }

    override fun onCleared() {
        tienda.clear()
    }

    private var ultimaInteraccion = System.currentTimeMillis()

    private val huellaActiva: Boolean
        get() = (estado.value as? SesionEstado.Activa)?.sesion?.desbloqueoHuella == true

    init {
        // al cerrar sesion, o si la direccion retira el acceso, no queda nada del usuario anterior
        viewModelScope.launch {
            estado.collect {
                if (it !is SesionEstado.Activa) {
                    tienda.clear()
                    tiendaDe = null
                }
            }
        }
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
        mensajeBloqueo = null
        registrarInteraccion()
    }

    fun avisarBloqueo(mensaje: String?) {
        mensajeBloqueo = mensaje
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            bloqueada = false
            mensajeBloqueo = null
            sesionRepository.cerrar()
        }
    }
}
