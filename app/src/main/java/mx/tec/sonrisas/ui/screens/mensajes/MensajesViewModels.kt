package mx.tec.sonrisas.ui.screens.mensajes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.Contacto
import mx.tec.sonrisas.data.LIMITE_MENSAJE
import mx.tec.sonrisas.data.MensajeChat
import mx.tec.sonrisas.data.MensajesRepository
import mx.tec.sonrisas.data.ResumenConversacion
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.ui.navigation.Route
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.state.comoUiState
import mx.tec.sonrisas.ui.state.reintentando
import java.time.LocalDate

data class ConversacionesDatos(val conversaciones: List<ResumenConversacion>, val hoy: LocalDate)

class ConversacionesViewModel(mensajesRepository: MensajesRepository, reloj: Reloj) : ViewModel() {
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<ConversacionesDatos>> = comoUiState(reintentos) {
        mensajesRepository.conversaciones().map { ConversacionesDatos(it, reloj.hoy()) }
    }

    val noLeidos: StateFlow<Int> = mensajesRepository.noLeidos().reintentando().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun reintentar() {
        reintentos.value++
    }
}

data class ChatDatos(val resumen: ResumenConversacion?, val mensajes: List<MensajeChat>, val hoy: LocalDate)

class ChatViewModel(
    savedStateHandle: SavedStateHandle,
    private val mensajesRepository: MensajesRepository,
    reloj: Reloj,
) : ViewModel() {
    private val conversacionId: String = checkNotNull(savedStateHandle[Route.ARG_CONVERSACION])
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<ChatDatos>> = comoUiState(reintentos) {
        combine(mensajesRepository.conversacion(conversacionId), mensajesRepository.mensajes(conversacionId)) { r, m ->
            ChatDatos(r, m, reloj.hoy())
        }
    }

    var borrador by mutableStateOf("")
        private set

    var enviando by mutableStateOf(false)
        private set

    fun cambiarBorrador(texto: String) {
        borrador = texto.take(LIMITE_MENSAJE)
    }

    fun reintentar() {
        reintentos.value++
    }

    fun marcarLeida() {
        viewModelScope.launch { mensajesRepository.marcarLeida(conversacionId) }
    }

    fun enviar(onError: (String) -> Unit) {
        val texto = borrador.trim()
        if (texto.isBlank() || enviando) return
        enviando = true
        viewModelScope.launch {
            try {
                mensajesRepository.enviar(conversacionId, texto)
                borrador = ""
            } catch (e: Exception) {
                // el borrador se conserva para reintentar sin volver a escribir
                onError("No pudimos enviar el mensaje. Revisa tu conexión e inténtalo de nuevo.")
            } finally {
                enviando = false
            }
        }
    }
}

class NuevoMensajeViewModel(private val mensajesRepository: MensajesRepository) : ViewModel() {
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<List<Contacto>>> = comoUiState(reintentos) { mensajesRepository.contactos() }

    var abriendo by mutableStateOf(false)
        private set

    fun reintentar() {
        reintentos.value++
    }

    fun abrir(c: Contacto, onListo: (String) -> Unit, onError: (String) -> Unit) {
        if (abriendo) return
        abriendo = true
        viewModelScope.launch {
            try {
                onListo(mensajesRepository.abrirDirecta(c.usuarioId, c.sala.id))
            } catch (e: Exception) {
                onError("No pudimos abrir la conversación. Revisa tu conexión e inténtalo de nuevo.")
            } finally {
                abriendo = false
            }
        }
    }
}
