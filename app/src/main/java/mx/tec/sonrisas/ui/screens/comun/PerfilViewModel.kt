package mx.tec.sonrisas.ui.screens.comun

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.NinosRepository
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.state.comoUiState

data class PerfilDatos(
    val nombre: String,
    val usuario: String,
    val rol: Rol,
    val estancia: String?,
    val sala: Sala?,
    val hijos: List<Pair<Nino, Sala?>>,
    val huella: Boolean,
) {
    val puedeUsarHuella: Boolean get() = rol == Rol.EDUCADORA || rol == Rol.DIRECCION
}

class PerfilViewModel(
    private val sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
) : ViewModel() {

    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<PerfilDatos>> = comoUiState(reintentos) {
        combine(
            sesionRepository.estado,
            estanciasRepository.estancias(),
            estanciasRepository.salas(),
            ninosRepository.ninos(),
        ) { estado, estancias, salas, ninos ->
            val sesion = (estado as SesionEstado.Activa).sesion
            val u = sesion.usuario
            PerfilDatos(
                nombre = u.nombre,
                usuario = u.usuario,
                rol = u.rol,
                estancia = estancias.firstOrNull { it.id == u.estanciaId }?.nombre,
                sala = salas.firstOrNull { it.id == u.salaId },
                hijos = if (u.rol == Rol.FAMILIAR) ninos.map { n -> n to salas.firstOrNull { it.id == n.salaId } } else emptyList(),
                huella = sesion.desbloqueoHuella,
            )
        }
    }

    fun reintentar() {
        reintentos.value++
    }

    fun cambiarHuella(activo: Boolean) {
        sesionRepository.cambiarDesbloqueoHuella(activo)
    }
}
