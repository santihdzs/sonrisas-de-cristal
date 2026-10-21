package mx.tec.sonrisas.ui.screens.fotos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.ArchivoEntrada
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.FotosRepository
import mx.tec.sonrisas.data.JornadaRepository
import mx.tec.sonrisas.data.NinosRepository
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.UsoFoto
import mx.tec.sonrisas.domain.puedeAparecerEn
import mx.tec.sonrisas.ui.navigation.Route
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.state.comoUiState
import java.io.File
import java.time.LocalDate
import java.util.UUID

data class GrupoFotos(val fecha: LocalDate, val fotos: List<Foto>)

data class GaleriaDatos(val grupos: List<GrupoFotos>, val salas: Map<String, Sala>, val hoy: LocalDate)

fun agrupar(fotos: List<Foto>): List<GrupoFotos> =
    fotos.sortedByDescending { it.cuando }
        .groupBy { it.cuando.toLocalDate() }
        .map { (fecha, lista) -> GrupoFotos(fecha, lista) }

class FotosSalaViewModel(
    sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
    private val fotosRepository: FotosRepository,
    reloj: Reloj,
) : ViewModel() {
    private val reintentos = MutableStateFlow(0)
    private val salaId: String? = (sesionRepository.estado.value as? SesionEstado.Activa)?.sesion?.usuario?.salaId

    val uiState: StateFlow<UiState<GaleriaDatos>> = comoUiState(reintentos) {
        combine(fotosRepository.fotosDeSala(salaId.orEmpty()), estanciasRepository.salas()) { fotos, salas ->
            GaleriaDatos(agrupar(fotos), salas.associateBy { it.id }, reloj.hoy())
        }
    }

    fun reintentar() {
        reintentos.value++
    }
}

data class AgregarFotoDatos(val sala: Sala, val ninos: List<Nino>)

data class AgregarFotoForm(
    // foto recien tomada con la camara, en la cache privada de la app
    val imagen: String? = null,
    val descripcion: String = "",
    val uso: UsoFoto = UsoFoto.INTERNA,
    val etiquetados: Set<String> = emptySet(),
    val guardando: Boolean = false,
    val error: String? = null,
) {
    val puedeGuardar: Boolean get() = imagen != null && descripcion.isNotBlank() && etiquetados.isNotEmpty() && !guardando
    val faltante: String?
        get() = when {
            imagen == null -> "Toma la foto para continuar."
            descripcion.isBlank() -> "Escribe una descripción."
            etiquetados.isEmpty() -> "Etiqueta al menos a un niño."
            else -> null
        }
}

class AgregarFotoViewModel(
    sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
    jornadaRepository: JornadaRepository,
    private val fotosRepository: FotosRepository,
    private val reloj: Reloj,
) : ViewModel() {
    private val reintentos = MutableStateFlow(0)
    private val hoy = reloj.hoy()
    private val salaId: String? = (sesionRepository.estado.value as? SesionEstado.Activa)?.sesion?.usuario?.salaId

    /** solo se puede etiquetar a quien vino hoy a la sala */
    val uiState: StateFlow<UiState<AgregarFotoDatos>> = comoUiState(reintentos) {
        combine(estanciasRepository.salas(), ninosRepository.ninos(), jornadaRepository.asistenciasDel(hoy)) { salas, ninos, asistencias ->
            val vinieron = asistencias.map { it.ninoId }.toSet()
            AgregarFotoDatos(salas.first { it.id == salaId }, ninos.filter { it.id in vinieron })
        }
    }

    var form by mutableStateOf(AgregarFotoForm())
        private set

    fun cambiar(nuevo: AgregarFotoForm) {
        form = nuevo.copy(error = null)
    }

    fun cambiarUso(uso: UsoFoto, ninos: List<Nino>) {
        // al cambiar a redes se quita a quien no tenga ese permiso
        val permitidos = ninos.filter { it.puedeAparecerEn(uso) }.map { it.id }.toSet()
        form = form.copy(uso = uso, etiquetados = form.etiquetados intersect permitidos, error = null)
    }

    fun alternar(nino: Nino) {
        if (!nino.puedeAparecerEn(form.uso)) return
        val actual = form.etiquetados
        form = form.copy(etiquetados = if (nino.id in actual) actual - nino.id else actual + nino.id, error = null)
    }

    fun reintentar() {
        reintentos.value++
    }

    fun guardar(onListo: (String) -> Unit) {
        val f = form
        if (!f.puedeGuardar || salaId == null) return
        form = f.copy(guardando = true)
        viewModelScope.launch {
            try {
                val imagen = File(f.imagen!!)
                fotosRepository.agregar(
                    Foto(UUID.randomUUID().toString(), salaId, reloj.ahora(), f.descripcion.trim(), f.etiquetados.toList(), f.uso, tema = null),
                    ArchivoEntrada(imagen.name, "image/jpeg", imagen.length()) { imagen.inputStream() },
                )
                imagen.delete()
                val n = f.etiquetados.size
                onListo("Guardamos la foto. La verán solo las familias de ${if (n == 1) "1 niño" else "$n niños"}.")
            } catch (e: Exception) {
                form = form.copy(guardando = false, error = "No pudimos guardar la foto. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }
}

data class FotoDetalleDatos(val foto: Foto, val sala: Sala?, val etiquetados: List<Nino>, val rol: Rol, val hoy: LocalDate)

class FotoDetalleViewModel(
    savedStateHandle: SavedStateHandle,
    private val sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
    private val fotosRepository: FotosRepository,
    reloj: Reloj,
) : ViewModel() {
    private val fotoId: String = checkNotNull(savedStateHandle[Route.ARG_FOTO])
    private val reintentos = MutableStateFlow(0)

    var borrando by mutableStateOf(false)
        private set

    var borrada by mutableStateOf(false)
        private set

    val uiState: StateFlow<UiState<FotoDetalleDatos?>> = comoUiState(reintentos) {
        combine(
            fotosRepository.foto(fotoId),
            estanciasRepository.salas(),
            ninosRepository.ninos(),
            sesionRepository.estado.map { (it as? SesionEstado.Activa)?.sesion?.usuario?.rol },
        ) { foto, salas, ninos, rol ->
            // la lista de ninos ya viene filtrada: una familia solo ve el nombre de su hijo
            foto?.let { f ->
                FotoDetalleDatos(f, salas.firstOrNull { it.id == f.salaId }, ninos.filter { it.id in f.ninoIds }, rol ?: Rol.FAMILIAR, reloj.hoy())
            }
        }
    }

    init {
        // RF-14: queda registro de quien consulto cada foto
        viewModelScope.launch { fotosRepository.registrarConsulta(fotoId) }
    }

    fun reintentar() {
        reintentos.value++
    }

    fun borrar(onListo: (String) -> Unit) {
        if (borrando) return
        borrando = true
        viewModelScope.launch {
            try {
                fotosRepository.borrar(fotoId)
                borrada = true
                onListo("Borramos la foto para todas las familias.")
            } catch (e: Exception) {
                borrando = false
                onListo("No pudimos borrar la foto. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }
}
