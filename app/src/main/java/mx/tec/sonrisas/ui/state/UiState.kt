package mx.tec.sonrisas.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

sealed interface UiState<out T> {
    data object Cargando : UiState<Nothing>
    data class Exito<T>(val datos: T) : UiState<T>
    data class Error(val mensaje: String) : UiState<Nothing>
}

const val MENSAJE_ERROR_CARGA = "No pudimos cargar la información. Revisa tu conexión e inténtalo de nuevo."

/** convierte un flow en UiState; cambiar el valor de reintentos vuelve a suscribirse */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> ViewModel.comoUiState(reintentos: MutableStateFlow<Int>, fuente: () -> Flow<T>): StateFlow<UiState<T>> =
    reintentos.flatMapLatest {
        fuente()
            .map<T, UiState<T>> { UiState.Exito(it) }
            .onStart { emit(UiState.Cargando) }
            .catch { emit(UiState.Error(MENSAJE_ERROR_CARGA)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Cargando)
