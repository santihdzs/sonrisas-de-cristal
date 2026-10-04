package mx.tec.sonrisas.ui.screens.familia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.FotosRepository
import mx.tec.sonrisas.data.HorariosRepository
import mx.tec.sonrisas.data.IncidentesRepository
import mx.tec.sonrisas.data.JornadaRepository
import mx.tec.sonrisas.data.NinosRepository
import mx.tec.sonrisas.data.ObjetosPerdidosRepository
import mx.tec.sonrisas.domain.ActividadActual
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.EstadoObjeto
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.ResumenDia
import mx.tec.sonrisas.domain.ResumenSemana
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.lunesDeLaSemana
import mx.tec.sonrisas.ui.navigation.Route
import mx.tec.sonrisas.ui.screens.fotos.GaleriaDatos
import mx.tec.sonrisas.ui.screens.fotos.agrupar
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.state.comoUiState
import java.time.LocalDate
import java.time.LocalDateTime

data class Hijos(val lista: List<Nino>, val seleccionado: Nino, val salas: Map<String, Sala>) {
    val sala: Sala? get() = salas[seleccionado.salaId]
}

data class HoyDatos(
    val hijos: Hijos,
    val resumen: ResumenDia,
    val actividad: ActividadActual,
    val bloques: List<BloqueHorario>,
    val especiales: List<ActividadEspecial>,
    val ahora: LocalDateTime,
    val objetosNuevos: Int = 0,
)

data class SemanaDatos(val hijos: Hijos, val semana: ResumenSemana, val hoy: LocalDate)

data class FotosFamiliaDatos(val hijos: Hijos, val galeria: GaleriaDatos)

/** compartido por hoy, semana y fotos para que el selector de hijo sea el mismo en las tres */
@OptIn(ExperimentalCoroutinesApi::class)
class FamiliaViewModel(
    estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
    private val jornadaRepository: JornadaRepository,
    private val incidentesRepository: IncidentesRepository,
    private val fotosRepository: FotosRepository,
    private val horariosRepository: HorariosRepository,
    private val objetosRepository: ObjetosPerdidosRepository,
    private val reloj: Reloj,
) : ViewModel() {
    private val reintentos = MutableStateFlow(0)
    private val seleccion = MutableStateFlow<String?>(null)

    private val ahora: Flow<LocalDateTime> = flow {
        while (true) {
            emit(reloj.ahora())
            delay(30_000)
        }
    }

    private val hijos: Flow<Hijos?> =
        combine(ninosRepository.ninos(), estanciasRepository.salas(), seleccion) { ninos, salas, sel ->
            if (ninos.isEmpty()) null
            else Hijos(ninos, ninos.firstOrNull { it.id == sel } ?: ninos.first(), salas.associateBy { it.id })
        }.distinctUntilChanged()

    val hoyState: StateFlow<UiState<HoyDatos?>> = comoUiState(reintentos) {
        hijos.flatMapLatest { h ->
            if (h == null) return@flatMapLatest flowOf(null)
            val n = h.seleccionado
            val base = combine(
                jornadaRepository.asistenciasDe(n.id, reloj.hoy(), reloj.hoy()),
                jornadaRepository.eventosDe(n.id, reloj.hoy(), reloj.hoy()),
                incidentesRepository.incidentesDe(n.id, reloj.hoy(), reloj.hoy()),
            ) { a, e, i -> ResumenDia.de(a.firstOrNull(), e, i) }
            val nuevos = combine(objetosRepository.objetos(n.salaId), objetosRepository.vistos()) { objetos, vistos ->
                objetos.count { it.estado == EstadoObjeto.DISPONIBLE && it.id !in vistos }
            }
            combine(base, horariosRepository.bloques(n.salaId), horariosRepository.especiales(n.salaId), ahora, nuevos) { resumen, bloques, especiales, t, objetos ->
                val deHoy = especiales.filter { it.fecha == t.toLocalDate() }
                HoyDatos(h, resumen, ActividadActual.de(bloques, especiales, t), bloques, deHoy, t, objetos)
            }
        }
    }

    val semanaState: StateFlow<UiState<SemanaDatos?>> = comoUiState(reintentos) {
        hijos.flatMapLatest { h ->
            if (h == null) return@flatMapLatest flowOf(null)
            val hoy = reloj.hoy()
            val lunes = hoy.lunesDeLaSemana()
            val viernes = lunes.plusDays(4)
            val n = h.seleccionado
            combine(
                jornadaRepository.asistenciasDe(n.id, lunes, viernes),
                jornadaRepository.eventosDe(n.id, lunes, viernes),
                incidentesRepository.incidentesDe(n.id, lunes, viernes),
            ) { a, e, i -> SemanaDatos(h, ResumenSemana.de(n.nombre, hoy, a, e, i), hoy) }
        }
    }

    val fotosState: StateFlow<UiState<FotosFamiliaDatos?>> = comoUiState(reintentos) {
        hijos.flatMapLatest { h ->
            if (h == null) return@flatMapLatest flowOf(null)
            fotosRepository.fotosDe(h.seleccionado.id).map { fotos ->
                FotosFamiliaDatos(h, GaleriaDatos(agrupar(fotos), h.salas, reloj.hoy()))
            }
        }
    }

    fun seleccionar(ninoId: String) {
        seleccion.value = ninoId
    }

    fun reintentar() {
        reintentos.value++
    }
}

data class IncidenteDetalleDatos(val incidente: Incidente, val nino: Nino, val sala: Sala?, val registradoPor: String?)

class IncidenteDetalleViewModel(
    savedStateHandle: SavedStateHandle,
    estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
    incidentesRepository: IncidentesRepository,
) : ViewModel() {
    private val incidenteId: String = checkNotNull(savedStateHandle[Route.ARG_INCIDENTE])
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<IncidenteDetalleDatos?>> = comoUiState(reintentos) {
        combine(
            incidentesRepository.incidente(incidenteId),
            ninosRepository.ninos(),
            estanciasRepository.salas(),
            estanciasRepository.personal(),
        ) { i, ninos, salas, personal ->
            i?.let { inc ->
                val n = ninos.firstOrNull { it.id == inc.ninoId } ?: return@combine null
                IncidenteDetalleDatos(inc, n, salas.firstOrNull { it.id == n.salaId }, personal.firstOrNull { it.id == inc.registradoPorId }?.nombre)
            }
        }
    }

    fun reintentar() {
        reintentos.value++
    }
}
