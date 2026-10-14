package mx.tec.sonrisas.ui.screens.comun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.AvisosRepository
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.state.comoUiState
import mx.tec.sonrisas.ui.state.reintentando

data class AvisosDatos(val avisos: List<Aviso>, val leidos: Set<String>) {
    val noLeidos: Int get() = avisos.count { it.id !in leidos }
}

class AvisosViewModel(private val avisosRepository: AvisosRepository) : ViewModel() {
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<AvisosDatos>> = comoUiState(reintentos) {
        combine(avisosRepository.avisos(), avisosRepository.leidos()) { a, l -> AvisosDatos(a, l) }
    }

    val noLeidos: StateFlow<Int> =
        combine(avisosRepository.avisos(), avisosRepository.leidos()) { a, l -> a.count { it.id !in l } }
            .reintentando()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun marcarLeidos(ids: Collection<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { avisosRepository.marcarLeidos(ids) }
    }

    fun reintentar() {
        reintentos.value++
    }
}
