package mx.tec.sonrisas.ui.screens.direccion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.ActividadRepository
import mx.tec.sonrisas.data.CuentasRepository
import mx.tec.sonrisas.data.AvisosRepository
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.HorariosRepository
import mx.tec.sonrisas.data.IncidentesRepository
import mx.tec.sonrisas.data.JornadaRepository
import mx.tec.sonrisas.data.NinosRepository
import mx.tec.sonrisas.data.ResumenEstancia
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.Credenciales
import mx.tec.sonrisas.domain.DIAS_PERIODO_BAJA
import mx.tec.sonrisas.domain.EstadoExpediente
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.PersonaAutorizada
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.ReporteAsistencia
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.Validaciones
import mx.tec.sonrisas.ui.navigation.Route
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.state.comoUiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

data class SalaResumen(val sala: Sala, val educadora: String?, val presentes: Int, val inscritos: Int)

data class InicioDatos(
    val estancia: Estancia?,
    val salas: List<SalaResumen>,
    val incidentes: List<Pair<Incidente, Nino>>,
    val avisos: List<Aviso>,
    val hoy: LocalDate,
)

class InicioViewModel(
    estanciasRepository: EstanciasRepository,
    ninosRepository: NinosRepository,
    jornadaRepository: JornadaRepository,
    incidentesRepository: IncidentesRepository,
    avisosRepository: AvisosRepository,
    reloj: Reloj,
) : ViewModel() {
    private val reintentos = MutableStateFlow(0)
    private val hoy = reloj.hoy()

    val uiState: StateFlow<UiState<InicioDatos>> = comoUiState(reintentos) {
        val base = combine(
            estanciasRepository.estancias(),
            estanciasRepository.salas(),
            estanciasRepository.educadoras(),
            ninosRepository.ninos(),
        ) { estancias, salas, educadoras, ninos -> Base(estancias.firstOrNull(), salas, educadoras, ninos) }
        combine(base, jornadaRepository.asistenciasDel(hoy), incidentesRepository.incidentesDel(hoy), avisosRepository.avisos()) { b, asistencias, incidentes, avisos ->
            val activos = b.ninos.filter { it.activo }
            val presentes = asistencias.filter { !it.salio }.map { it.ninoId }.toSet()
            InicioDatos(
                estancia = b.estancia,
                salas = b.salas.map { s ->
                    val deSala = activos.filter { it.salaId == s.id }
                    SalaResumen(s, b.educadoras.firstOrNull { it.salaId == s.id }?.nombre, deSala.count { it.id in presentes }, deSala.size)
                },
                incidentes = incidentes.mapNotNull { i -> b.ninos.firstOrNull { it.id == i.ninoId }?.let { i to it } },
                avisos = avisos.take(3),
                hoy = hoy,
            )
        }
    }

    private data class Base(val estancia: Estancia?, val salas: List<Sala>, val educadoras: List<Usuario>, val ninos: List<Nino>)

    fun reintentar() {
        reintentos.value++
    }
}

data class NinosDatos(val ninos: List<Nino>, val salas: List<Sala>, val hoy: LocalDate)

class NinosViewModel(estanciasRepository: EstanciasRepository, ninosRepository: NinosRepository, reloj: Reloj) : ViewModel() {
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<NinosDatos>> = comoUiState(reintentos) {
        combine(ninosRepository.ninos(), estanciasRepository.salas()) { n, s -> NinosDatos(n, s, reloj.hoy()) }
    }

    var busqueda by mutableStateOf("")
        private set
    var salaId by mutableStateOf<String?>(null)
        private set
    var bajas by mutableStateOf(false)
        private set

    fun buscar(texto: String) {
        busqueda = texto
    }

    fun elegirSala(id: String?) {
        salaId = id
    }

    fun verBajas(ver: Boolean) {
        bajas = ver
    }

    fun reintentar() {
        reintentos.value++
    }
}

data class ExpedienteDatos(
    val nino: Nino,
    val sala: Sala?,
    val tutores: List<Tutor>,
    val hoy: LocalDate,
    val cuentas: Map<String, Usuario> = emptyMap(),
)

class ExpedienteViewModel(
    savedStateHandle: SavedStateHandle,
    estanciasRepository: EstanciasRepository,
    private val ninosRepository: NinosRepository,
    cuentasRepository: CuentasRepository,
    reloj: Reloj,
) : ViewModel() {
    private val ninoId: String = checkNotNull(savedStateHandle[Route.ARG_NINO])
    private val reintentos = MutableStateFlow(0)

    var trabajando by mutableStateOf(false)
        private set

    val uiState: StateFlow<UiState<ExpedienteDatos?>> = comoUiState(reintentos) {
        combine(
            ninosRepository.nino(ninoId),
            estanciasRepository.salas(),
            ninosRepository.tutoresDe(ninoId),
            cuentasRepository.cuentas(),
        ) { n, salas, tutores, cuentas ->
            n?.let {
                val familia = cuentas.filter { c -> c.rol == Rol.FAMILIAR && c.activo && c.tutorId != null }.associateBy { c -> c.tutorId!! }
                ExpedienteDatos(it, salas.firstOrNull { s -> s.id == it.salaId }, tutores, reloj.hoy(), familia)
            }
        }
    }

    fun reintentar() {
        reintentos.value++
    }

    private fun accion(mensaje: String, onListo: (String) -> Unit, bloque: suspend () -> Unit) {
        if (trabajando) return
        trabajando = true
        viewModelScope.launch {
            try {
                bloque()
                onListo(mensaje)
            } catch (e: Exception) {
                onListo("No pudimos completar la acción. Revisa tu conexión e inténtalo de nuevo.")
            } finally {
                trabajando = false
            }
        }
    }

    fun darDeBaja(onListo: (String) -> Unit) =
        accion("Dimos de baja el expediente. Puedes reactivarlo durante $DIAS_PERIODO_BAJA días.", onListo) { ninosRepository.darDeBaja(ninoId) }

    fun reactivar(onListo: (String) -> Unit) =
        accion("Reactivamos el expediente.", onListo) { ninosRepository.reactivar(ninoId) }

    fun eliminar(onListo: (String) -> Unit) =
        accion("Eliminamos el expediente y todas sus fotos.", onListo) { ninosRepository.eliminarDefinitivamente(ninoId) }
}

data class FilaTutor(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val parentesco: String = "",
    val telefono: String = "",
    val puedeRecoger: Boolean = true,
    // tutor que ya existia en la estancia; asi los hermanos comparten una sola cuenta
    val existente: Boolean = false,
    val darAcceso: Boolean = false,
)

data class FilaPersona(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val parentesco: String = "",
    val telefono: String = "",
)

data class InscripcionForm(
    val nombre: String = "",
    val apellidos: String = "",
    val fechaNacimiento: LocalDate? = null,
    val salaId: String? = null,
    val alergias: String = "",
    val padecimientos: String = "",
    val medicamentos: String = "",
    val tutores: List<FilaTutor> = listOf(FilaTutor()),
    val otras: List<FilaPersona> = emptyList(),
    val permisoInterna: Boolean = false,
    val permisoRedes: Boolean = false,
    val intento: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
) {
    fun errores(hoy: LocalDate): Map<String, String> = buildMap {
        Validaciones.requerido(nombre, "el nombre")?.let { put("nombre", it) }
        Validaciones.requerido(apellidos, "los apellidos")?.let { put("apellidos", it) }
        Validaciones.fechaNacimiento(fechaNacimiento, hoy)?.let { put("fecha", it) }
        if (salaId == null) put("sala", "Elige la sala.")
        if (tutores.isEmpty()) put("tutores", "Agrega al menos un tutor.")
        tutores.forEach { t ->
            Validaciones.requerido(t.nombre, "el nombre")?.let { put("tutor.nombre.${t.id}", it) }
            Validaciones.requerido(t.parentesco, "el parentesco")?.let { put("tutor.parentesco.${t.id}", it) }
            Validaciones.telefono(t.telefono)?.let { put("tutor.telefono.${t.id}", it) }
        }
        otras.forEach { p ->
            Validaciones.requerido(p.nombre, "el nombre")?.let { put("otra.nombre.${p.id}", it) }
            Validaciones.requerido(p.parentesco, "el parentesco")?.let { put("otra.parentesco.${p.id}", it) }
            Validaciones.telefono(p.telefono)?.let { put("otra.telefono.${p.id}", it) }
        }
        if (tutores.none { it.puedeRecoger } && otras.isEmpty()) {
            put("recoger", "Debe haber al menos una persona que pueda recogerlo.")
        }
    }
}

data class InscripcionDatos(
    val salas: List<Sala>,
    val editando: Boolean,
    val hoy: LocalDate,
    val registrados: List<Tutor> = emptyList(),
    val hijosDe: Map<String, List<String>> = emptyMap(),
    val cuentas: Map<String, Usuario> = emptyMap(),
)

class InscripcionViewModel(
    savedStateHandle: SavedStateHandle,
    estanciasRepository: EstanciasRepository,
    private val ninosRepository: NinosRepository,
    private val cuentasRepository: CuentasRepository,
    private val reloj: Reloj,
) : ViewModel() {
    private val ninoId: String? = savedStateHandle[Route.ARG_NINO]
    private val reintentos = MutableStateFlow(0)
    private var original: Nino? = null

    val uiState: StateFlow<UiState<InscripcionDatos>> = comoUiState(reintentos) {
        combine(
            estanciasRepository.salas(),
            ninosRepository.tutoresDeEstancia(),
            ninosRepository.ninos(),
            cuentasRepository.cuentas(),
        ) { salas, tutores, ninos, cuentas ->
            val otros = ninos.filter { it.id != ninoId }
            InscripcionDatos(
                salas = salas,
                editando = ninoId != null,
                hoy = reloj.hoy(),
                registrados = tutores,
                hijosDe = tutores.associate { t -> t.id to otros.filter { t.id in it.tutorIds }.map { it.nombre } },
                cuentas = cuentas.filter { it.rol == Rol.FAMILIAR && it.activo && it.tutorId != null }.associateBy { it.tutorId!! },
            )
        }
    }

    fun elegirRegistrado(t: Tutor) {
        if (form.tutores.any { it.id == t.id }) return
        // si solo esta la fila vacia inicial, se reemplaza por el tutor elegido
        val base = form.tutores.filterNot { it.nombre.isBlank() && it.parentesco.isBlank() && it.telefono.isBlank() && !it.existente }
        cambiar(form.copy(tutores = base + FilaTutor(t.id, t.nombre, t.parentesco, t.telefono, existente = true)))
    }

    var form by mutableStateOf(InscripcionForm())
        private set

    init {
        if (ninoId != null) {
            viewModelScope.launch {
                val n = ninosRepository.nino(ninoId).first() ?: return@launch
                val tutores = ninosRepository.tutoresDe(ninoId).first()
                original = n
                val idsTutores = tutores.map { it.id }.toSet()
                form = InscripcionForm(
                    nombre = n.nombre,
                    apellidos = n.apellidos,
                    fechaNacimiento = n.fechaNacimiento,
                    salaId = n.salaId,
                    alergias = n.alergias,
                    padecimientos = n.padecimientos,
                    medicamentos = n.medicamentos,
                    tutores = tutores.map { t -> FilaTutor(t.id, t.nombre, t.parentesco, t.telefono, n.persona(t.id) != null, existente = true) },
                    otras = n.personasAutorizadas.filter { it.id !in idsTutores }.map { FilaPersona(it.id, it.nombre, it.parentesco, it.telefono) },
                    permisoInterna = n.permisoImagenInterna,
                    permisoRedes = n.permisoImagenRedes,
                )
            }
        }
    }

    fun cambiar(nuevo: InscripcionForm) {
        form = nuevo.copy(error = null)
    }

    fun reintentar() {
        reintentos.value++
    }

    fun guardar(onListo: (String, String, List<Credenciales>) -> Unit) {
        val f = form.copy(intento = true)
        form = f
        if (f.errores(reloj.hoy()).isNotEmpty() || f.guardando) return
        form = f.copy(guardando = true)
        val tutores = f.tutores.map { Tutor(it.id, it.nombre.trim(), it.parentesco.trim(), it.telefono.trim()) }
        val autorizadas = f.tutores.filter { it.puedeRecoger }.map { PersonaAutorizada(it.id, it.nombre.trim(), it.parentesco.trim(), it.telefono.trim()) } +
            f.otras.map { PersonaAutorizada(it.id, it.nombre.trim(), it.parentesco.trim(), it.telefono.trim()) }
        val base = original
        val nino = Nino(
            id = base?.id ?: UUID.randomUUID().toString(),
            nombre = f.nombre.trim(),
            apellidos = f.apellidos.trim(),
            fechaNacimiento = f.fechaNacimiento!!,
            estanciaId = base?.estanciaId ?: "",
            salaId = f.salaId!!,
            tutorIds = tutores.map { it.id },
            personasAutorizadas = autorizadas,
            alergias = f.alergias.trim(),
            padecimientos = f.padecimientos.trim(),
            medicamentos = f.medicamentos.trim(),
            permisoImagenInterna = f.permisoInterna,
            permisoImagenRedes = f.permisoRedes,
            estado = base?.estado ?: EstadoExpediente.Activo,
        )
        viewModelScope.launch {
            val guardado = try {
                ninosRepository.guardar(nino, tutores)
            } catch (e: Exception) {
                form = form.copy(guardando = false, error = "No pudimos guardar el expediente. Revisa tu conexión e inténtalo de nuevo.")
                return@launch
            }
            val mensaje = if (base == null) "Inscribimos a ${guardado.nombre}." else "Guardamos los cambios del expediente."
            val conCuenta = cuentasRepository.cuentas().first().mapNotNull { it.tutorId }.toSet()
            val credenciales = mutableListOf<Credenciales>()
            var fallo = false
            f.tutores.filter { it.darAcceso && it.id !in conCuenta }.forEach { t ->
                try {
                    credenciales += cuentasRepository.crearCuentaFamilia(t.id)
                } catch (e: Exception) {
                    fallo = true
                }
            }
            onListo(
                if (fallo) "$mensaje No pudimos crear todas las cuentas de familia; inténtalo de nuevo desde Editar expediente." else mensaje,
                guardado.id,
                credenciales,
            )
        }
    }
}

data class AvisoForm(
    val tipo: TipoAviso = TipoAviso.GENERAL,
    val titulo: String = "",
    val cuerpo: String = "",
    val fecha: LocalDate? = null,
    val hora: LocalTime = LocalTime.of(17, 30),
    val intento: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
) {
    val errores: Map<String, String> get() = Validaciones.aviso(titulo, cuerpo)
}

class PublicarAvisoViewModel(private val avisosRepository: AvisosRepository) : ViewModel() {
    var form by mutableStateOf(AvisoForm())
        private set

    fun cambiar(nuevo: AvisoForm) {
        form = nuevo.copy(error = null)
    }

    fun publicar(onListo: (String) -> Unit) {
        val f = form.copy(intento = true)
        form = f
        if (f.errores.isNotEmpty() || f.guardando) return
        form = f.copy(guardando = true)
        viewModelScope.launch {
            try {
                avisosRepository.publicar(f.tipo, f.titulo, f.cuerpo, f.fecha?.let { LocalDateTime.of(it, f.hora) })
                onListo("Publicamos el aviso para las familias y educadoras de la estancia.")
            } catch (e: Exception) {
                form = form.copy(guardando = false, error = "No pudimos publicar el aviso. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }
}

data class ReporteDatos(
    val reporte: ReporteAsistencia,
    val salas: List<Sala>,
    val estancias: List<Estancia>,
    val esFundacion: Boolean,
    val mesActual: YearMonth,
    val salaId: String?,
    val estanciaId: String?,
) {
    val puedeAvanzar: Boolean get() = reporte.mes < mesActual
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReporteViewModel(
    sesionRepository: SesionRepository,
    estanciasRepository: EstanciasRepository,
    private val jornadaRepository: JornadaRepository,
    reloj: Reloj,
) : ViewModel() {
    private val reintentos = MutableStateFlow(0)
    private val mesActual = YearMonth.from(reloj.hoy())
    private val rol = (sesionRepository.estado.value as? SesionEstado.Activa)?.sesion?.usuario?.rol

    private data class Filtro(val mes: YearMonth, val estanciaId: String?, val salaId: String?)
    private val filtro = MutableStateFlow(Filtro(mesActual, null, null))

    val uiState: StateFlow<UiState<ReporteDatos>> = comoUiState(reintentos) {
        combine(estanciasRepository.estancias(), estanciasRepository.salas(), filtro) { e, s, f -> Triple(e, s, f) }
            .flatMapLatest { (estancias, salas, f) ->
                val estanciaId = f.estanciaId ?: estancias.firstOrNull()?.id
                jornadaRepository.reporte(f.mes, estanciaId, f.salaId).map { r ->
                    ReporteDatos(
                        r, salas.filter { it.estanciaId == estanciaId }, estancias, rol == Rol.FUNDACION, mesActual,
                        salaId = f.salaId, estanciaId = estanciaId,
                    )
                }
            }
    }

    fun mesAnterior() {
        filtro.value = filtro.value.copy(mes = filtro.value.mes.minusMonths(1))
    }

    fun mesSiguiente() {
        if (filtro.value.mes < mesActual) filtro.value = filtro.value.copy(mes = filtro.value.mes.plusMonths(1))
    }

    fun elegirSala(id: String?) {
        filtro.value = filtro.value.copy(salaId = id)
    }

    fun elegirEstancia(id: String) {
        filtro.value = filtro.value.copy(estanciaId = id, salaId = null)
    }

    fun reintentar() {
        reintentos.value++
    }
}

sealed interface HojaHorario {
    data class Bloque(
        val id: String?,
        val inicio: LocalTime,
        val fin: LocalTime,
        val actividad: String,
        val guardando: Boolean = false,
        val error: String? = null,
    ) : HojaHorario

    data class Especial(
        val fecha: LocalDate,
        val inicio: LocalTime,
        val fin: LocalTime,
        val titulo: String = "",
        val guardando: Boolean = false,
        val error: String? = null,
    ) : HojaHorario
}

data class HorariosDatos(val salas: List<Sala>, val sala: Sala, val bloques: List<BloqueHorario>, val especiales: List<ActividadEspecial>, val hoy: LocalDate)

@OptIn(ExperimentalCoroutinesApi::class)
class HorariosViewModel(
    estanciasRepository: EstanciasRepository,
    private val horariosRepository: HorariosRepository,
    private val reloj: Reloj,
) : ViewModel() {
    private val reintentos = MutableStateFlow(0)
    private val seleccion = MutableStateFlow<String?>(null)
    private var bloquesActuales: List<BloqueHorario> = emptyList()
    private var salaActual: String? = null

    // una estancia recien registrada todavia no tiene salas; en ese caso no hay horario que mostrar
    val uiState: StateFlow<UiState<HorariosDatos?>> = comoUiState(reintentos) {
        combine(estanciasRepository.salas(), seleccion) { salas, sel -> salas to (salas.firstOrNull { it.id == sel } ?: salas.firstOrNull()) }
            .flatMapLatest { (salas, sala) ->
                if (sala == null) return@flatMapLatest flowOf(null)
                combine(horariosRepository.bloques(sala.id), horariosRepository.especiales(sala.id)) { b, e ->
                    bloquesActuales = b
                    salaActual = sala.id
                    HorariosDatos(salas, sala, b, e.filter { !it.fecha.isBefore(reloj.hoy()) }, reloj.hoy())
                }
            }
    }

    var hoja by mutableStateOf<HojaHorario?>(null)
        private set

    fun elegirSala(id: String) {
        seleccion.value = id
    }

    fun nuevoBloque() {
        val ultimo = bloquesActuales.maxByOrNull { it.fin }?.fin ?: LocalTime.of(8, 0)
        hoja = HojaHorario.Bloque(null, ultimo, ultimo.plusMinutes(30), "")
    }

    fun editarBloque(b: BloqueHorario) {
        hoja = HojaHorario.Bloque(b.id, b.inicio, b.fin, b.actividad)
    }

    fun nuevaEspecial() {
        hoja = HojaHorario.Especial(reloj.hoy(), LocalTime.of(11, 0), LocalTime.of(12, 0))
    }

    fun cambiar(nueva: HojaHorario) {
        hoja = when (nueva) {
            is HojaHorario.Bloque -> nueva.copy(error = null)
            is HojaHorario.Especial -> nueva.copy(error = null)
        }
    }

    fun cerrar() {
        hoja = null
    }

    fun reintentar() {
        reintentos.value++
    }

    fun guardar(onListo: (String) -> Unit) {
        val sala = salaActual ?: return
        when (val h = hoja) {
            is HojaHorario.Bloque -> {
                val otros = bloquesActuales.filter { it.id != h.id }
                Validaciones.bloque(h.inicio, h.fin, h.actividad, otros)?.let {
                    hoja = h.copy(error = it)
                    return
                }
                hoja = h.copy(guardando = true)
                viewModelScope.launch {
                    try {
                        horariosRepository.guardarBloque(BloqueHorario(h.id ?: UUID.randomUUID().toString(), sala, h.inicio, h.fin, h.actividad.trim()))
                        hoja = null
                        onListo("Guardamos el bloque del horario.")
                    } catch (e: Exception) {
                        hoja = h.copy(guardando = false, error = "No pudimos guardar el bloque. Inténtalo de nuevo.")
                    }
                }
            }
            is HojaHorario.Especial -> {
                val error = when {
                    h.titulo.isBlank() -> "Escribe el nombre de la actividad."
                    !h.fin.isAfter(h.inicio) -> "La hora de fin debe ser después del inicio."
                    h.fecha.isBefore(reloj.hoy()) -> "Elige hoy o una fecha futura."
                    else -> null
                }
                if (error != null) {
                    hoja = h.copy(error = error)
                    return
                }
                hoja = h.copy(guardando = true)
                viewModelScope.launch {
                    try {
                        horariosRepository.guardarEspecial(ActividadEspecial(UUID.randomUUID().toString(), sala, h.fecha, h.inicio, h.fin, h.titulo.trim()))
                        hoja = null
                        onListo("Publicamos la actividad especial. Las familias la verán en \"Ahora mismo\".")
                    } catch (e: Exception) {
                        hoja = h.copy(guardando = false, error = "No pudimos publicar la actividad. Inténtalo de nuevo.")
                    }
                }
            }
            null -> Unit
        }
    }

    fun borrarBloque(id: String, onListo: (String) -> Unit) {
        viewModelScope.launch {
            try {
                horariosRepository.borrarBloque(id)
                onListo("Borramos el bloque.")
            } catch (e: Exception) {
                onListo("No pudimos borrar el bloque. Inténtalo de nuevo.")
            }
        }
    }

    fun borrarEspecial(id: String, onListo: (String) -> Unit) {
        viewModelScope.launch {
            try {
                horariosRepository.borrarEspecial(id)
                onListo("Borramos la actividad especial.")
            } catch (e: Exception) {
                onListo("No pudimos borrar la actividad. Inténtalo de nuevo.")
            }
        }
    }
}

enum class FiltroActividad(val etiqueta: String, val acciones: Set<AccionRegistro>?) {
    TODO("Todo", null),
    CAPTURAS("Capturas", setOf(AccionRegistro.CAPTURA, AccionRegistro.EDICION, AccionRegistro.BORRADO)),
    FOTOS("Fotos consultadas", setOf(AccionRegistro.FOTO_CONSULTADA)),
    EXPEDIENTES("Expedientes", setOf(AccionRegistro.EXPEDIENTE)),
    AVISOS("Avisos", setOf(AccionRegistro.AVISO)),
    OBJETOS("Objetos perdidos", setOf(AccionRegistro.OBJETO)),
    MATERIAL("Material de clase", setOf(AccionRegistro.MATERIAL)),
    IMPRESIONES("Bitácoras exportadas", setOf(AccionRegistro.IMPRESION)),
    ADMINISTRACION("Salas y personal", setOf(AccionRegistro.ESTRUCTURA, AccionRegistro.CUENTA)),
}

data class ActividadDatos(val registros: List<RegistroActividad>, val ninos: Map<String, Nino>, val hoy: LocalDate)

class ActividadViewModel(
    actividadRepository: ActividadRepository,
    ninosRepository: NinosRepository,
    reloj: Reloj,
) : ViewModel() {
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<ActividadDatos>> = comoUiState(reintentos) {
        combine(actividadRepository.registros(), ninosRepository.ninos()) { r, n -> ActividadDatos(r, n.associateBy { it.id }, reloj.hoy()) }
    }

    var filtro by mutableStateOf(FiltroActividad.TODO)
        private set

    fun cambiarFiltro(f: FiltroActividad) {
        filtro = f
    }

    fun reintentar() {
        reintentos.value++
    }
}

data class FundacionDatos(val estancias: List<ResumenEstancia>) {
    val total: Int get() = estancias.sumOf { it.inscritos }
    val presentes: Int get() = estancias.sumOf { it.presentesHoy }
}

class FundacionViewModel(estanciasRepository: EstanciasRepository) : ViewModel() {
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<UiState<FundacionDatos>> = comoUiState(reintentos) {
        estanciasRepository.resumenFundacion().map { FundacionDatos(it) }
    }

    fun reintentar() {
        reintentos.value++
    }
}
