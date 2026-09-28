package mx.tec.sonrisas.ui.screens.educadora

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.IncidentesRepository
import mx.tec.sonrisas.data.JornadaRepository
import mx.tec.sonrisas.data.NinosRepository
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.ControlEsfinteres
import mx.tec.sonrisas.domain.EstadoAnimo
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Porcion
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.SiNoNoAplica
import mx.tec.sonrisas.domain.TiempoComida
import mx.tec.sonrisas.domain.TipoEvacuacion
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.Validaciones
import mx.tec.sonrisas.domain.tiempoComidaSugerido
import mx.tec.sonrisas.ui.navigation.Route
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.state.comoUiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

const val MENSAJE_ERROR_GUARDAR = "No pudimos guardar el registro. Revisa tu conexión e inténtalo de nuevo."

private fun SesionRepository.usuario(): Flow<Usuario?> =
    estado.map { (it as? SesionEstado.Activa)?.sesion?.usuario }

private fun LocalTime.sinSegundos(): LocalTime = withSecond(0).withNano(0)

enum class EstadoAsistencia { SIN_LLEGAR, PRESENTE, SALIO }

data class NinoEnSala(val nino: Nino, val asistencia: Asistencia?) {
    val estado: EstadoAsistencia
        get() = when {
            asistencia == null -> EstadoAsistencia.SIN_LLEGAR
            asistencia.salio -> EstadoAsistencia.SALIO
            else -> EstadoAsistencia.PRESENTE
        }
}

data class SalaDatos(val sala: Sala, val fecha: LocalDate, val ninos: List<NinoEnSala>) {
    val presentes: Int get() = ninos.count { it.estado == EstadoAsistencia.PRESENTE }
}

enum class FiltroSala(val etiqueta: String) {
    TODOS("Todos"), SIN_LLEGAR("Sin llegar"), PRESENTES("Presentes"), SALIERON("Ya salieron");

    fun aplica(n: NinoEnSala): Boolean = when (this) {
        TODOS -> true
        SIN_LLEGAR -> n.estado == EstadoAsistencia.SIN_LLEGAR
        PRESENTES -> n.estado == EstadoAsistencia.PRESENTE
        SALIERON -> n.estado == EstadoAsistencia.SALIO
    }
}

class SalaViewModel(
    sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
    jornadaRepository: JornadaRepository,
    reloj: Reloj,
) : ViewModel() {
    private val hoy = reloj.hoy()
    private val reintentos = MutableStateFlow(0)

    var filtro by mutableStateOf(FiltroSala.TODOS)
        private set

    val uiState: StateFlow<UiState<SalaDatos>> = comoUiState(reintentos) {
        combine(
            sesionRepository.usuario(),
            estanciasRepository.salas(),
            ninosRepository.ninos(),
            jornadaRepository.asistenciasDel(hoy),
        ) { u, salas, ninos, asistencias ->
            val sala = salas.first { it.id == u?.salaId }
            val porNino = asistencias.associateBy { it.ninoId }
            SalaDatos(sala, hoy, ninos.map { NinoEnSala(it, porNino[it.id]) })
        }
    }

    fun cambiarFiltro(f: FiltroSala) {
        filtro = f
    }

    fun reintentar() {
        reintentos.value++
    }
}

data class LlegadaForm(
    val hora: LocalTime,
    val entregadoPorId: String? = null,
    val llegoLimpio: Boolean = true,
    val mochilaCompleta: Boolean = true,
    val buenEstado: Boolean = true,
    val lesion: String = "",
    val conSintomas: Boolean = false,
    val sintomas: String = "",
    val medicamento: SiNoNoAplica = SiNoNoAplica.NO_APLICA,
    val guardando: Boolean = false,
    val error: String? = null,
) {
    val faltaLesion: Boolean get() = !buenEstado && lesion.isBlank()
    val faltanSintomas: Boolean get() = conSintomas && sintomas.isBlank()
    val puedeGuardar: Boolean get() = entregadoPorId != null && !faltaLesion && !faltanSintomas && !guardando
}

data class SalidaForm(
    val hora: LocalTime,
    val recogidoPorId: String? = null,
    val salioLimpio: Boolean = true,
    val pertenenciasCompletas: Boolean = true,
    val buenEstado: Boolean = true,
    val lesion: String = "",
    val observaciones: String = "",
    val guardando: Boolean = false,
    val error: String? = null,
) {
    val faltaLesion: Boolean get() = !buenEstado && lesion.isBlank()
    val puedeGuardar: Boolean get() = recogidoPorId != null && !faltaLesion && !guardando
}

sealed interface HojaAsistencia {
    val nino: Nino

    data class Llegada(override val nino: Nino, val form: LlegadaForm) : HojaAsistencia
    data class Salida(
        override val nino: Nino,
        val asistencia: Asistencia,
        val incidentes: List<Incidente>,
        val form: SalidaForm,
    ) : HojaAsistencia
}

/** hojas de llegada y salida; se usan desde mi sala y desde la ficha del dia */
class AsistenciaViewModel(
    private val jornadaRepository: JornadaRepository,
    private val incidentesRepository: IncidentesRepository,
    private val reloj: Reloj,
) : ViewModel() {

    var hoja by mutableStateOf<HojaAsistencia?>(null)
        private set

    fun abrirLlegada(nino: Nino) {
        hoja = HojaAsistencia.Llegada(nino, LlegadaForm(hora = reloj.ahora().toLocalTime().sinSegundos()))
    }

    fun abrirSalida(nino: Nino, asistencia: Asistencia) {
        val hoy = reloj.hoy()
        viewModelScope.launch {
            val incidentes = incidentesRepository.incidentesDe(nino.id, hoy, hoy).first()
            hoja = HojaAsistencia.Salida(nino, asistencia, incidentes, SalidaForm(hora = reloj.ahora().toLocalTime().sinSegundos()))
        }
    }

    fun cerrar() {
        hoja = null
    }

    fun cambiarLlegada(form: LlegadaForm) {
        (hoja as? HojaAsistencia.Llegada)?.let { hoja = it.copy(form = form.copy(error = null)) }
    }

    fun cambiarSalida(form: SalidaForm) {
        (hoja as? HojaAsistencia.Salida)?.let { hoja = it.copy(form = form.copy(error = null)) }
    }

    fun guardar(onListo: (String) -> Unit) {
        when (val h = hoja) {
            is HojaAsistencia.Llegada -> guardarLlegada(h, onListo)
            is HojaAsistencia.Salida -> guardarSalida(h, onListo)
            null -> Unit
        }
    }

    private fun guardarLlegada(h: HojaAsistencia.Llegada, onListo: (String) -> Unit) {
        val f = h.form
        if (!f.puedeGuardar) return
        hoja = h.copy(form = f.copy(guardando = true))
        viewModelScope.launch {
            try {
                jornadaRepository.registrarLlegada(
                    Asistencia(
                        id = UUID.randomUUID().toString(),
                        ninoId = h.nino.id,
                        fecha = reloj.hoy(),
                        horaEntrada = f.hora,
                        entregadoPorId = f.entregadoPorId!!,
                        recibidoPorId = "",
                        llegoLimpio = f.llegoLimpio,
                        mochilaCompleta = f.mochilaCompleta,
                        buenEstadoEntrada = f.buenEstado,
                        lesionEntrada = f.lesion.trim().takeIf { !f.buenEstado },
                        sintomas = f.sintomas.trim().takeIf { f.conSintomas },
                        medicamentoConReceta = if (f.conSintomas) f.medicamento else SiNoNoAplica.NO_APLICA,
                    ),
                )
                hoja = null
                onListo("Registramos la llegada de ${h.nino.nombre}.")
            } catch (e: Exception) {
                hoja = h.copy(form = f.copy(guardando = false, error = MENSAJE_ERROR_GUARDAR))
            }
        }
    }

    private fun guardarSalida(h: HojaAsistencia.Salida, onListo: (String) -> Unit) {
        val f = h.form
        if (!f.puedeGuardar) return
        hoja = h.copy(form = f.copy(guardando = true))
        viewModelScope.launch {
            try {
                jornadaRepository.registrarSalida(
                    h.asistencia.copy(
                        horaSalida = f.hora,
                        recogidoPorId = f.recogidoPorId,
                        salioLimpio = f.salioLimpio,
                        pertenenciasCompletas = f.pertenenciasCompletas,
                        buenEstadoSalida = f.buenEstado,
                        lesionSalida = f.lesion.trim().takeIf { !f.buenEstado },
                        observacionesSalida = f.observaciones.trim().ifBlank { null },
                    ),
                )
                hoja = null
                onListo("Registramos la salida de ${h.nino.nombre}.")
            } catch (e: Exception) {
                hoja = h.copy(form = f.copy(guardando = false, error = MENSAJE_ERROR_GUARDAR))
            }
        }
    }
}

sealed interface ItemLinea {
    val id: String
    val cuando: LocalDateTime

    data class Evento(val evento: EventoBitacora) : ItemLinea {
        override val id get() = evento.id
        override val cuando get() = evento.cuando
    }
    data class Llegada(val asistencia: Asistencia) : ItemLinea {
        override val id get() = "llegada-" + asistencia.id
        override val cuando: LocalDateTime get() = LocalDateTime.of(asistencia.fecha, asistencia.horaEntrada)
    }
    data class Salida(val asistencia: Asistencia) : ItemLinea {
        override val id get() = "salida-" + asistencia.id
        override val cuando: LocalDateTime get() = LocalDateTime.of(asistencia.fecha, asistencia.horaSalida!!)
    }
    data class ConIncidente(val incidente: Incidente) : ItemLinea {
        override val id get() = incidente.id
        override val cuando get() = incidente.cuando
    }
}

data class FichaDatos(
    val nino: Nino,
    val sala: Sala,
    val asistencia: Asistencia?,
    val linea: List<ItemLinea>,
    val siestaAbierta: EventoBitacora.Siesta?,
    val nombres: Map<String, String>,
    val hoy: LocalDate,
)

enum class HojaRapida { COMIDA, SIESTA, BANO, ANIMO, NOTA }

data class ComidaForm(val hora: LocalTime, val tiempo: TiempoComida, val porcion: Porcion? = null)
data class SiestaForm(val inicio: LocalTime, val fin: LocalTime, val manual: Boolean = false)
data class BanoForm(val hora: LocalTime, val tipo: TipoEvacuacion? = null, val control: ControlEsfinteres = ControlEsfinteres.NO_APLICA)
data class AnimoForm(
    val hora: LocalTime,
    val estado: EstadoAnimo? = null,
    val lloro: Boolean = false,
    val peleo: Boolean = false,
    val participo: Boolean = true,
)
data class NotaForm(val hora: LocalTime, val texto: String = "")

data class FichaFormState(
    val hoja: HojaRapida? = null,
    val comida: ComidaForm,
    val siesta: SiestaForm,
    val bano: BanoForm,
    val animo: AnimoForm,
    val nota: NotaForm,
    val guardando: Boolean = false,
    val error: String? = null,
) {
    val puedeGuardar: Boolean
        get() = !guardando && when (hoja) {
            HojaRapida.COMIDA -> comida.porcion != null
            HojaRapida.SIESTA -> siesta.fin.isAfter(siesta.inicio)
            HojaRapida.BANO -> bano.tipo != null
            HojaRapida.ANIMO -> animo.estado != null
            HojaRapida.NOTA -> nota.texto.isNotBlank()
            null -> false
        }
}

class FichaViewModel(
    savedStateHandle: SavedStateHandle,
    sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
    private val jornadaRepository: JornadaRepository,
    incidentesRepository: IncidentesRepository,
    private val reloj: Reloj,
) : ViewModel() {
    private val ninoId: String = checkNotNull(savedStateHandle[Route.ARG_NINO])
    private val hoy = reloj.hoy()
    private val reintentos = MutableStateFlow(0)
    private var tipoSala: TipoSala? = null

    val uiState: StateFlow<UiState<FichaDatos>> = comoUiState(reintentos) {
        val base = combine(
            ninosRepository.nino(ninoId),
            estanciasRepository.salas(),
            estanciasRepository.personal(),
            jornadaRepository.asistenciasDe(ninoId, hoy, hoy),
        ) { nino, salas, personal, asistencias -> Base(nino, salas, personal, asistencias.firstOrNull()) }
        combine(
            base,
            jornadaRepository.eventosDe(ninoId, hoy, hoy),
            incidentesRepository.incidentesDe(ninoId, hoy, hoy),
        ) { b, eventos, incidentes ->
            val nino = b.nino ?: throw IllegalStateException("sin acceso")
            val sala = b.salas.first { it.id == nino.salaId }
            tipoSala = sala.tipo
            val linea = buildList<ItemLinea> {
                b.asistencia?.let { add(ItemLinea.Llegada(it)) }
                b.asistencia?.takeIf { it.salio }?.let { add(ItemLinea.Salida(it)) }
                eventos.forEach { add(ItemLinea.Evento(it)) }
                incidentes.forEach { add(ItemLinea.ConIncidente(it)) }
            }.sortedByDescending { it.cuando }
            FichaDatos(
                nino = nino,
                sala = sala,
                asistencia = b.asistencia,
                linea = linea,
                siestaAbierta = eventos.filterIsInstance<EventoBitacora.Siesta>().lastOrNull { it.fin == null },
                nombres = b.personal.associate { it.id to it.nombre },
                hoy = hoy,
            )
        }
    }

    val ahora: StateFlow<LocalTime> = flow {
        while (true) {
            emit(reloj.ahora().toLocalTime())
            delay(30_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), reloj.ahora().toLocalTime())

    private data class Base(val nino: Nino?, val salas: List<Sala>, val personal: List<Usuario>, val asistencia: Asistencia?)

    var form by mutableStateOf(formNuevo(null))
        private set

    private fun ahora(): LocalTime = reloj.ahora().toLocalTime().sinSegundos()

    private fun formNuevo(hoja: HojaRapida?): FichaFormState {
        val h = ahora()
        return FichaFormState(
            hoja = hoja,
            comida = ComidaForm(h, tiempoComidaSugerido(h)),
            siesta = SiestaForm(inicio = h.minusMinutes(60), fin = h),
            bano = BanoForm(h, control = if (tipoSala == TipoSala.LACTANTES) ControlEsfinteres.NO_APLICA else ControlEsfinteres.AVISO),
            animo = AnimoForm(h),
            nota = NotaForm(h),
        )
    }

    fun abrir(hoja: HojaRapida) {
        form = formNuevo(hoja)
    }

    fun cerrar() {
        form = form.copy(hoja = null)
    }

    fun cambiar(nuevo: FichaFormState) {
        form = nuevo.copy(error = null)
    }

    fun reintentar() {
        reintentos.value++
    }

    private fun evento(): EventoBitacora? {
        val id = UUID.randomUUID().toString()
        val f = form
        fun en(h: LocalTime) = LocalDateTime.of(hoy, h)
        return when (f.hoja) {
            HojaRapida.COMIDA -> EventoBitacora.Alimento(id, ninoId, en(f.comida.hora), "", f.comida.tiempo, f.comida.porcion ?: return null)
            HojaRapida.SIESTA -> EventoBitacora.Siesta(id, ninoId, en(f.siesta.fin), "", f.siesta.inicio, f.siesta.fin)
            HojaRapida.BANO -> EventoBitacora.Evacuacion(id, ninoId, en(f.bano.hora), "", f.bano.tipo ?: return null, f.bano.control)
            HojaRapida.ANIMO -> EventoBitacora.Animo(id, ninoId, en(f.animo.hora), "", f.animo.estado ?: return null, f.animo.lloro, f.animo.peleo, f.animo.participo)
            HojaRapida.NOTA -> EventoBitacora.Nota(id, ninoId, en(f.nota.hora), "", f.nota.texto.trim())
            null -> null
        }
    }

    /** onListo recibe el mensaje y el id del evento para poder deshacer */
    fun guardar(onListo: (String, String) -> Unit) {
        if (!form.puedeGuardar) return
        val e = evento() ?: return
        form = form.copy(guardando = true)
        viewModelScope.launch {
            try {
                jornadaRepository.agregarEventos(listOf(e))
                form = form.copy(hoja = null, guardando = false)
                onListo(mensajeGuardado(e), e.id)
            } catch (ex: Exception) {
                form = form.copy(guardando = false, error = MENSAJE_ERROR_GUARDAR)
            }
        }
    }

    fun empezarSiesta(onListo: (String, String) -> Unit) {
        val id = UUID.randomUUID().toString()
        val h = ahora()
        form = form.copy(guardando = true)
        viewModelScope.launch {
            try {
                jornadaRepository.agregarEventos(listOf(EventoBitacora.Siesta(id, ninoId, LocalDateTime.of(hoy, h), "", h, null)))
                form = form.copy(hoja = null, guardando = false)
                onListo("Registramos que empezó a dormir.", id)
            } catch (ex: Exception) {
                form = form.copy(guardando = false, error = MENSAJE_ERROR_GUARDAR)
            }
        }
    }

    fun terminarSiesta(siestaId: String, onListo: (String) -> Unit) {
        form = form.copy(guardando = true)
        viewModelScope.launch {
            try {
                jornadaRepository.terminarSiesta(siestaId, ahora())
                form = form.copy(hoja = null, guardando = false)
                onListo("Registramos que despertó.")
            } catch (ex: Exception) {
                form = form.copy(guardando = false, error = MENSAJE_ERROR_GUARDAR)
            }
        }
    }

    fun borrar(eventoId: String, onListo: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                jornadaRepository.borrarEvento(eventoId)
                onListo("Borramos el registro.")
            } catch (ex: Exception) {
                onListo("No pudimos borrar el registro. Inténtalo de nuevo.")
            }
        }
    }

    private fun mensajeGuardado(e: EventoBitacora): String = when (e) {
        is EventoBitacora.Alimento -> "Guardamos la comida."
        is EventoBitacora.Siesta -> "Guardamos la siesta."
        is EventoBitacora.Evacuacion -> "Guardamos el registro de baño."
        is EventoBitacora.Animo -> "Guardamos el ánimo."
        is EventoBitacora.Nota -> "Guardamos la nota."
    }
}

enum class ModoGrupal { COMIDA, SIESTA }

data class RegistroGrupalForm(
    val modo: ModoGrupal = ModoGrupal.COMIDA,
    val hora: LocalTime,
    val tiempo: TiempoComida,
    val porcionGeneral: Porcion = Porcion.TODO,
    val excepciones: Map<String, Porcion> = emptyMap(),
    val seleccion: Set<String>? = null,
    val inicio: LocalTime,
    val fin: LocalTime,
    val guardando: Boolean = false,
    val error: String? = null,
) {
    fun porcionDe(ninoId: String): Porcion = excepciones[ninoId] ?: porcionGeneral
}

data class GrupalDatos(val sala: Sala, val presentes: List<Nino>)

class RegistroGrupalViewModel(
    sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
    private val jornadaRepository: JornadaRepository,
    private val reloj: Reloj,
) : ViewModel() {
    private val hoy = reloj.hoy()
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<GrupalDatos>> = comoUiState(reintentos) {
        combine(
            sesionRepository.usuario(),
            estanciasRepository.salas(),
            ninosRepository.ninos(),
            jornadaRepository.asistenciasDel(hoy),
        ) { u, salas, ninos, asistencias ->
            val presentes = asistencias.filter { !it.salio }.map { it.ninoId }.toSet()
            GrupalDatos(salas.first { it.id == u?.salaId }, ninos.filter { it.id in presentes })
        }
    }

    var form by mutableStateOf(
        reloj.ahora().toLocalTime().sinSegundos().let { h ->
            RegistroGrupalForm(hora = h, tiempo = tiempoComidaSugerido(h), inicio = h.minusMinutes(60), fin = h)
        },
    )
        private set

    fun cambiar(nuevo: RegistroGrupalForm) {
        form = nuevo.copy(error = null)
    }

    fun reintentar() {
        reintentos.value++
    }

    fun seleccionados(presentes: List<Nino>): List<Nino> =
        form.seleccion?.let { sel -> presentes.filter { it.id in sel } } ?: presentes

    fun guardar(presentes: List<Nino>, onListo: (String) -> Unit) {
        val f = form
        if (f.guardando) return
        val eventos = when (f.modo) {
            ModoGrupal.COMIDA -> presentes.map {
                EventoBitacora.Alimento(UUID.randomUUID().toString(), it.id, LocalDateTime.of(hoy, f.hora), "", f.tiempo, f.porcionDe(it.id))
            }
            ModoGrupal.SIESTA -> {
                if (!f.fin.isAfter(f.inicio)) {
                    form = f.copy(error = "La hora en que despertaron debe ser después del inicio.")
                    return
                }
                seleccionados(presentes).map {
                    EventoBitacora.Siesta(UUID.randomUUID().toString(), it.id, LocalDateTime.of(hoy, f.fin), "", f.inicio, f.fin)
                }
            }
        }
        if (eventos.isEmpty()) return
        form = f.copy(guardando = true)
        viewModelScope.launch {
            try {
                jornadaRepository.agregarEventos(eventos)
                form = form.copy(guardando = false)
                val que = if (f.modo == ModoGrupal.COMIDA) "la comida" else "la siesta"
                onListo("Guardamos $que de ${eventos.size} niños.")
            } catch (e: Exception) {
                form = form.copy(guardando = false, error = MENSAJE_ERROR_GUARDAR)
            }
        }
    }
}

data class IncidenteForm(
    val tipo: TipoIncidente? = null,
    val hora: LocalTime,
    val parteDelCuerpo: String = "",
    val descripcion: String = "",
    val accionTomada: String = "",
    val intentoGuardar: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
) {
    val errores: Map<String, String>
        get() = Validaciones.incidente(descripcion, accionTomada) +
            (if (tipo == null) mapOf("tipo" to "Elige el tipo de incidente.") else emptyMap())
    val puedeGuardar: Boolean get() = errores.isEmpty() && !guardando
}

data class IncidenteDatos(val nino: Nino, val sala: Sala)

class IncidenteViewModel(
    savedStateHandle: SavedStateHandle,
    estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
    private val incidentesRepository: IncidentesRepository,
    private val reloj: Reloj,
) : ViewModel() {
    private val ninoId: String = checkNotNull(savedStateHandle[Route.ARG_NINO])
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<IncidenteDatos>> = comoUiState(reintentos) {
        combine(ninosRepository.nino(ninoId), estanciasRepository.salas()) { nino, salas ->
            val n = nino ?: throw IllegalStateException("sin acceso")
            IncidenteDatos(n, salas.first { it.id == n.salaId })
        }
    }

    var form by mutableStateOf(IncidenteForm(hora = reloj.ahora().toLocalTime().sinSegundos()))
        private set

    fun cambiar(nuevo: IncidenteForm) {
        form = nuevo.copy(error = null)
    }

    fun reintentar() {
        reintentos.value++
    }

    fun guardar(onListo: (String) -> Unit) {
        val f = form.copy(intentoGuardar = true)
        form = f
        if (!f.puedeGuardar) return
        form = f.copy(guardando = true)
        viewModelScope.launch {
            try {
                incidentesRepository.registrar(
                    ninoId, LocalDateTime.of(reloj.hoy(), f.hora), f.tipo!!, f.parteDelCuerpo, f.descripcion, f.accionTomada,
                )
                onListo("Registramos el incidente y avisamos a la familia.")
            } catch (e: Exception) {
                form = form.copy(guardando = false, error = "No pudimos registrar el incidente. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }
}
