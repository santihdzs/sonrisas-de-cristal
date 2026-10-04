package mx.tec.sonrisas.ui.screens.sala

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.ArchivoEntrada
import mx.tec.sonrisas.data.ArchivoMuyGrande
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.LIMITE_ARCHIVO_MB
import mx.tec.sonrisas.data.ObjetosPerdidosRepository
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.EstadoObjeto
import mx.tec.sonrisas.domain.ObjetoPerdido
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.Validaciones
import mx.tec.sonrisas.ui.navigation.Route
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.state.comoUiState
import java.time.LocalDate

enum class FiltroObjetos(val etiqueta: String) {
    DISPONIBLES("Disponibles"), RECLAMADOS("Reclamados"), HISTORIAL("Entregados");

    fun aplica(o: ObjetoPerdido): Boolean = when (this) {
        DISPONIBLES -> o.estado == EstadoObjeto.DISPONIBLE
        RECLAMADOS -> o.estado == EstadoObjeto.RECLAMADO
        HISTORIAL -> o.estado == EstadoObjeto.ENTREGADO
    }
}

data class ObjetosDatos(
    val sala: Sala?,
    val objetos: List<ObjetoPerdido>,
    val esPersonal: Boolean,
    val miId: String?,
    val nuevos: Set<String>,
    val hoy: LocalDate,
) {
    fun cuantos(f: FiltroObjetos): Int = objetos.count { f.aplica(it) }
}

sealed interface HojaObjeto {
    data class Publicar(
        val descripcion: String = "",
        val donde: String = "",
        val fecha: LocalDate,
        val foto: ArchivoEntrada? = null,
        val vistaPrevia: String? = null,
        val intento: Boolean = false,
        val guardando: Boolean = false,
        val error: String? = null,
    ) : HojaObjeto {
        fun errores(hoy: LocalDate): Map<String, String> = buildMap {
            Validaciones.requerido(descripcion, "qué objeto es")?.let { put("descripcion", it) }
            Validaciones.requerido(donde, "dónde se encontró")?.let { put("donde", it) }
            if (fecha.isAfter(hoy)) put("fecha", "La fecha no puede ser futura.")
        }
    }

    data class Reclamar(val objeto: ObjetoPerdido, val nota: String = "", val guardando: Boolean = false, val error: String? = null) : HojaObjeto
}

class ObjetosViewModel(
    savedStateHandle: SavedStateHandle,
    sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
    private val objetosRepository: ObjetosPerdidosRepository,
    private val reloj: Reloj,
) : ViewModel() {
    private val salaId: String = checkNotNull(savedStateHandle[Route.ARG_SALA])
    private val reintentos = MutableStateFlow(0)
    private val usuario = (sesionRepository.estado.value as? SesionEstado.Activa)?.sesion?.usuario

    val uiState: StateFlow<UiState<ObjetosDatos>> = comoUiState(reintentos) {
        combine(estanciasRepository.salas(), objetosRepository.objetos(salaId), objetosRepository.vistos()) { salas, objetos, vistos ->
            val familia = usuario?.rol == Rol.FAMILIAR
            ObjetosDatos(
                sala = salas.firstOrNull { it.id == salaId },
                objetos = objetos,
                esPersonal = usuario?.rol == Rol.EDUCADORA || usuario?.rol == Rol.DIRECCION,
                miId = usuario?.id,
                nuevos = if (familia) objetos.filter { it.estado == EstadoObjeto.DISPONIBLE && it.id !in vistos }.map { it.id }.toSet() else emptySet(),
                hoy = reloj.hoy(),
            )
        }
    }

    var filtro by mutableStateOf(FiltroObjetos.DISPONIBLES)
        private set

    var hoja by mutableStateOf<HojaObjeto?>(null)
        private set

    fun cambiarFiltro(f: FiltroObjetos) {
        filtro = f
    }

    fun reintentar() {
        reintentos.value++
    }

    fun abrirPublicar() {
        hoja = HojaObjeto.Publicar(fecha = reloj.hoy())
    }

    fun abrirReclamo(o: ObjetoPerdido) {
        hoja = HojaObjeto.Reclamar(o)
    }

    fun cambiar(nueva: HojaObjeto) {
        hoja = when (nueva) {
            is HojaObjeto.Publicar -> nueva.copy(error = null)
            is HojaObjeto.Reclamar -> nueva.copy(error = null)
        }
    }

    fun cerrar() {
        hoja = null
    }

    fun marcarVistos(ids: Collection<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { objetosRepository.marcarVistos(ids) }
    }

    fun publicar(onListo: (String) -> Unit) {
        val h = (hoja as? HojaObjeto.Publicar)?.copy(intento = true) ?: return
        hoja = h
        if (h.errores(reloj.hoy()).isNotEmpty() || h.guardando) return
        hoja = h.copy(guardando = true)
        viewModelScope.launch {
            try {
                objetosRepository.publicar(salaId, h.descripcion, h.donde, h.fecha, h.foto)
                hoja = null
                filtro = FiltroObjetos.DISPONIBLES
                onListo("Publicamos el objeto. Las familias de la sala ya lo pueden ver.")
            } catch (e: ArchivoMuyGrande) {
                hoja = h.copy(guardando = false, error = "La foto pesa más de $LIMITE_ARCHIVO_MB MB. Elige una más ligera o publica sin foto.")
            } catch (e: Exception) {
                hoja = h.copy(guardando = false, error = "No pudimos publicar el objeto. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }

    fun reclamar(onListo: (String) -> Unit) {
        val h = hoja as? HojaObjeto.Reclamar ?: return
        if (h.guardando) return
        hoja = h.copy(guardando = true)
        viewModelScope.launch {
            try {
                objetosRepository.reclamar(h.objeto.id, h.nota)
                hoja = null
                onListo("Avisamos a la educadora. Recoge el objeto en la sala a la hora de salida.")
            } catch (e: IllegalStateException) {
                hoja = h.copy(guardando = false, error = "Otra familia ya reclamó este objeto. Si es tuyo, habla con la educadora.")
            } catch (e: Exception) {
                hoja = h.copy(guardando = false, error = "No pudimos enviar el reclamo. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }

    fun entregar(o: ObjetoPerdido, onListo: (String) -> Unit) = accion(onListo, "Marcamos \"${o.descripcion}\" como entregado.") {
        objetosRepository.entregar(o.id)
    }

    fun rechazar(o: ObjetoPerdido, onListo: (String) -> Unit) = accion(onListo, "El objeto vuelve a estar disponible para las familias.") {
        objetosRepository.rechazarReclamo(o.id)
    }

    private fun accion(onListo: (String) -> Unit, mensaje: String, bloque: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                bloque()
                onListo(mensaje)
            } catch (e: Exception) {
                onListo("No pudimos actualizar el objeto. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }
}
