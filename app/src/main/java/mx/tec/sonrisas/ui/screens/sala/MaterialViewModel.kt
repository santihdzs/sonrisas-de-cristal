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
import mx.tec.sonrisas.data.MaterialRepository
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.data.tipoMaterialPermitido
import mx.tec.sonrisas.domain.MaterialClase
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.ui.navigation.Route
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.state.comoUiState
import java.time.LocalDate

data class MaterialDatos(
    val sala: Sala?,
    val material: List<MaterialClase>,
    val autores: Map<String, String>,
    val esPersonal: Boolean,
    val miId: String?,
    val esDireccion: Boolean,
    val hoy: LocalDate,
) {
    fun puedeEditar(m: MaterialClase): Boolean = esDireccion || (esPersonal && m.subidoPorId == miId)
}

sealed interface HojaMaterial {
    data class Subir(
        val archivo: ArchivoEntrada,
        val titulo: String,
        val descripcion: String = "",
        val intento: Boolean = false,
        val guardando: Boolean = false,
        val error: String? = null,
    ) : HojaMaterial

    data class Editar(
        val material: MaterialClase,
        val titulo: String,
        val descripcion: String,
        val intento: Boolean = false,
        val guardando: Boolean = false,
        val error: String? = null,
    ) : HojaMaterial

    data class Borrar(val material: MaterialClase) : HojaMaterial
}

class MaterialViewModel(
    savedStateHandle: SavedStateHandle,
    sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
    private val materialRepository: MaterialRepository,
    reloj: Reloj,
) : ViewModel() {
    private val salaId: String = checkNotNull(savedStateHandle[Route.ARG_SALA])
    private val reintentos = MutableStateFlow(0)
    private val usuario = (sesionRepository.estado.value as? SesionEstado.Activa)?.sesion?.usuario

    val uiState: StateFlow<UiState<MaterialDatos>> = comoUiState(reintentos) {
        combine(estanciasRepository.salas(), materialRepository.material(salaId), estanciasRepository.personal()) { salas, material, personal ->
            MaterialDatos(
                sala = salas.firstOrNull { it.id == salaId },
                material = material,
                autores = personal.associate { it.id to it.nombre },
                esPersonal = usuario?.rol == Rol.EDUCADORA || usuario?.rol == Rol.DIRECCION,
                miId = usuario?.id,
                esDireccion = usuario?.rol == Rol.DIRECCION,
                hoy = reloj.hoy(),
            )
        }
    }

    var hoja by mutableStateOf<HojaMaterial?>(null)
        private set

    fun elegido(archivo: ArchivoEntrada, onError: (String) -> Unit) {
        when {
            !tipoMaterialPermitido(archivo.tipo) -> onError("Ese tipo de archivo no se puede subir. Elige un PDF, una imagen o un documento de Word, Excel o PowerPoint.")
            (archivo.tamano ?: 0) > LIMITE_ARCHIVO_MB * 1024L * 1024L -> onError(errorTamano)
            else -> hoja = HojaMaterial.Subir(archivo, archivo.nombre.substringBeforeLast('.'))
        }
    }

    fun editar(m: MaterialClase) {
        hoja = HojaMaterial.Editar(m, m.titulo, m.descripcion)
    }

    fun pedirBorrar(m: MaterialClase) {
        hoja = HojaMaterial.Borrar(m)
    }

    fun cambiar(nueva: HojaMaterial) {
        hoja = when (nueva) {
            is HojaMaterial.Subir -> nueva.copy(error = null)
            is HojaMaterial.Editar -> nueva.copy(error = null)
            is HojaMaterial.Borrar -> nueva
        }
    }

    fun cerrar() {
        hoja = null
    }

    fun reintentar() {
        reintentos.value++
    }

    private val errorTamano = "El archivo pesa más de $LIMITE_ARCHIVO_MB MB. Elige uno más ligero o compártelo en partes."

    fun guardar(onListo: (String) -> Unit) {
        when (val h = hoja) {
            is HojaMaterial.Subir -> {
                val c = h.copy(intento = true)
                hoja = c
                if (c.titulo.isBlank() || c.guardando) return
                hoja = c.copy(guardando = true)
                viewModelScope.launch {
                    try {
                        materialRepository.subir(salaId, c.titulo, c.descripcion, c.archivo)
                        hoja = null
                        onListo("Subimos el material. Las familias de la sala ya lo pueden ver.")
                    } catch (e: ArchivoMuyGrande) {
                        hoja = c.copy(guardando = false, error = errorTamano)
                    } catch (e: Exception) {
                        hoja = c.copy(guardando = false, error = "No pudimos subir el archivo. Revisa tu conexión e inténtalo de nuevo.")
                    }
                }
            }
            is HojaMaterial.Editar -> {
                val c = h.copy(intento = true)
                hoja = c
                if (c.titulo.isBlank() || c.guardando) return
                hoja = c.copy(guardando = true)
                viewModelScope.launch {
                    try {
                        materialRepository.editar(c.material.id, c.titulo, c.descripcion)
                        hoja = null
                        onListo("Guardamos los cambios.")
                    } catch (e: Exception) {
                        hoja = c.copy(guardando = false, error = "No pudimos guardar los cambios. Revisa tu conexión e inténtalo de nuevo.")
                    }
                }
            }
            is HojaMaterial.Borrar -> {
                hoja = null
                viewModelScope.launch {
                    try {
                        materialRepository.borrar(h.material.id)
                        onListo("Borramos \"${h.material.titulo}\" para todas las familias.")
                    } catch (e: Exception) {
                        onListo("No pudimos borrar el material. Revisa tu conexión e inténtalo de nuevo.")
                    }
                }
            }
            null -> Unit
        }
    }
}
