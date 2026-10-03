package mx.tec.sonrisas.ui.screens.direccion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.CuentasRepository
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.NinosRepository
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Credenciales
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.Validaciones
import mx.tec.sonrisas.ui.navigation.Route
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.state.comoUiState
import java.util.UUID

const val MENSAJE_ERROR_REGISTRO = "No pudimos guardar el registro. Revisa tu conexión e inténtalo de nuevo."

data class EstanciaForm(
    val nombre: String = "",
    val colonia: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val intento: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
) {
    fun errores(nueva: Boolean): Map<String, String> = Validaciones.estancia(nombre, colonia, telefono) + buildMap {
        if (nueva) Validaciones.requerido(direccion, "el nombre de quien dirige la estancia")?.let { put("direccion", it) }
    }
}

data class NuevaCuentaForm(val nombre: String = "", val intento: Boolean = false, val guardando: Boolean = false, val error: String? = null)

data class EstanciaDatos(val estancia: Estancia?, val cuentas: List<Usuario>) {
    val nueva: Boolean get() = estancia == null
}

class EstanciaViewModel(
    savedStateHandle: SavedStateHandle,
    private val estanciasRepository: EstanciasRepository,
    private val cuentasRepository: CuentasRepository,
) : ViewModel() {
    private val estanciaId: String? = savedStateHandle[Route.ARG_ESTANCIA]
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<EstanciaDatos>> = comoUiState(reintentos) {
        combine(estanciasRepository.estancias(), cuentasRepository.cuentas()) { estancias, cuentas ->
            EstanciaDatos(
                estancias.firstOrNull { it.id == estanciaId },
                cuentas.filter { it.rol == Rol.DIRECCION && it.estanciaId != null && it.estanciaId == estanciaId },
            )
        }
    }

    var form by mutableStateOf(EstanciaForm())
        private set

    var nuevaCuenta by mutableStateOf<NuevaCuentaForm?>(null)
        private set

    init {
        if (estanciaId != null) {
            viewModelScope.launch {
                val e = estanciasRepository.estancias().first().firstOrNull { it.id == estanciaId } ?: return@launch
                form = EstanciaForm(e.nombre, e.colonia, e.telefono)
            }
        }
    }

    fun cambiar(nuevo: EstanciaForm) {
        form = nuevo.copy(error = null)
    }

    fun reintentar() {
        reintentos.value++
    }

    fun guardar(onListo: (String, List<Credenciales>) -> Unit) {
        val f = form.copy(intento = true)
        form = f
        val nueva = estanciaId == null
        if (f.errores(nueva).isNotEmpty() || f.guardando) return
        form = f.copy(guardando = true)
        viewModelScope.launch {
            try {
                val guardada = estanciasRepository.guardarEstancia(Estancia(estanciaId ?: UUID.randomUUID().toString(), f.nombre, f.colonia, f.telefono))
                if (nueva) {
                    val credenciales = cuentasRepository.crearDireccion(guardada.id, f.direccion)
                    onListo("Registramos ${guardada.nombre}.", listOf(credenciales))
                } else {
                    onListo("Guardamos los datos de ${guardada.nombre}.", emptyList())
                }
            } catch (e: Exception) {
                form = form.copy(guardando = false, error = MENSAJE_ERROR_REGISTRO)
            }
        }
    }

    fun abrirNuevaCuenta() {
        nuevaCuenta = NuevaCuentaForm()
    }

    fun cambiarNuevaCuenta(nueva: NuevaCuentaForm) {
        nuevaCuenta = nueva.copy(error = null)
    }

    fun cerrarNuevaCuenta() {
        nuevaCuenta = null
    }

    fun crearCuenta(onListo: (List<Credenciales>) -> Unit) {
        val id = estanciaId ?: return
        val c = nuevaCuenta?.copy(intento = true) ?: return
        nuevaCuenta = c
        if (c.nombre.isBlank() || c.guardando) return
        nuevaCuenta = c.copy(guardando = true)
        viewModelScope.launch {
            try {
                val credenciales = cuentasRepository.crearDireccion(id, c.nombre)
                nuevaCuenta = null
                onListo(listOf(credenciales))
            } catch (e: Exception) {
                nuevaCuenta = c.copy(guardando = false, error = "No pudimos crear la cuenta. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }
}

data class SalaFila(val sala: Sala, val educadoras: List<String>, val inscritos: Int)

data class SalasDatos(val salas: List<SalaFila>) {
    val todas: List<Sala> get() = salas.map { it.sala }
}

data class HojaSala(
    val id: String? = null,
    val nombre: String = "",
    val tipo: TipoSala = TipoSala.LACTANTES,
    val estrella: ColorEstrella? = null,
    val intento: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
)

class SalasViewModel(
    private val estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
) : ViewModel() {
    private val reintentos = MutableStateFlow(0)
    private var salasActuales: List<Sala> = emptyList()

    val uiState: StateFlow<UiState<SalasDatos>> = comoUiState(reintentos) {
        combine(estanciasRepository.salas(), estanciasRepository.educadoras(), ninosRepository.ninos()) { salas, educadoras, ninos ->
            salasActuales = salas
            SalasDatos(
                salas.map { s ->
                    SalaFila(
                        s,
                        educadoras.filter { it.salaId == s.id }.map { it.nombre },
                        ninos.count { it.salaId == s.id && it.activo },
                    )
                },
            )
        }
    }

    var hoja by mutableStateOf<HojaSala?>(null)
        private set

    fun nueva() {
        val libre = ColorEstrella.entries.firstOrNull { c -> salasActuales.none { it.estrella == c } }
        hoja = HojaSala(estrella = libre)
    }

    fun editar(sala: Sala) {
        hoja = HojaSala(sala.id, sala.nombre, sala.tipo, sala.estrella)
    }

    fun cambiar(nueva: HojaSala) {
        hoja = nueva.copy(error = null)
    }

    fun cerrar() {
        hoja = null
    }

    fun reintentar() {
        reintentos.value++
    }

    fun errores(h: HojaSala): Map<String, String> =
        Validaciones.sala(h.nombre, h.estrella, salasActuales.filter { it.id != h.id })

    fun guardar(onListo: (String) -> Unit) {
        val h = hoja?.copy(intento = true) ?: return
        hoja = h
        if (errores(h).isNotEmpty() || h.guardando) return
        hoja = h.copy(guardando = true)
        viewModelScope.launch {
            try {
                // la estancia la pone la capa de datos a partir de la sesion
                val sala = estanciasRepository.guardarSala(Sala(h.id ?: UUID.randomUUID().toString(), "", h.nombre, h.tipo, h.estrella!!))
                hoja = null
                onListo(if (h.id == null) "Registramos la sala ${sala.nombre}." else "Guardamos los cambios de la sala ${sala.nombre}.")
            } catch (e: Exception) {
                hoja = h.copy(guardando = false, error = MENSAJE_ERROR_REGISTRO)
            }
        }
    }
}
