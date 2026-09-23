package mx.tec.sonrisas.data.fake

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.ActividadRepository
import mx.tec.sonrisas.data.AvisosRepository
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.FotosRepository
import mx.tec.sonrisas.data.HorariosRepository
import mx.tec.sonrisas.data.IncidentesRepository
import mx.tec.sonrisas.data.JornadaRepository
import mx.tec.sonrisas.data.NinosRepository
import mx.tec.sonrisas.data.ResumenEstancia
import mx.tec.sonrisas.data.Sesion
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.DIAS_PERIODO_BAJA
import mx.tec.sonrisas.domain.EstadoExpediente
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.ReporteAsistencia
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.folioIncidente
import mx.tec.sonrisas.domain.puedeAparecerEn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random

// simula la latencia de red para que se vean los estados de carga y "Guardando..."
private suspend fun latencia() = delay(Random.nextLong(200, 401))

/** reglas de quien ve a que nino (RF-02, RF-07, RF-17) */
internal object Visibilidad {
    fun ninos(usuario: Usuario?, todos: List<Nino>): List<Nino> = when (usuario?.rol) {
        Rol.EDUCADORA -> todos.filter { it.estanciaId == usuario.estanciaId && it.salaId == usuario.salaId && it.activo }
        Rol.FAMILIAR -> todos.filter { usuario.tutorId != null && usuario.tutorId in it.tutorIds && it.activo }
        Rol.DIRECCION -> todos.filter { it.estanciaId == usuario.estanciaId }
        // la fundacion solo ve datos consolidados, nunca expedientes individuales
        Rol.FUNDACION, null -> emptyList()
    }
}

class FakeSesionRepository(
    private val almacen: AlmacenMemoria,
    private val reloj: Reloj,
    scope: CoroutineScope,
) : SesionRepository {

    private val _estado = MutableStateFlow<SesionEstado>(SesionEstado.Cargando)
    override val estado: StateFlow<SesionEstado> = _estado.asStateFlow()

    val usuario: Flow<Usuario?> = estado.map { (it as? SesionEstado.Activa)?.sesion?.usuario }.distinctUntilChanged()

    fun usuarioActual(): Usuario? = (estado.value as? SesionEstado.Activa)?.sesion?.usuario

    init {
        scope.launch {
            delay(700)
            _estado.value = SesionEstado.Anonima
        }
    }

    override suspend fun iniciar(usuario: String, contrasena: String): Boolean {
        latencia()
        val encontrado = almacen.usuarios.value.firstOrNull {
            it.usuario.equals(usuario.trim(), ignoreCase = true) && it.contrasena == contrasena
        } ?: return false
        _estado.value = SesionEstado.Activa(Sesion(encontrado, encontrado.id in almacen.desbloqueoHuella.value))
        almacen.registrar(encontrado, reloj.ahora(), AccionRegistro.SESION, "Inició sesión")
        return true
    }

    override fun cuentasDemo(): List<Usuario> {
        val principales = listOf("educadora", "familia", "direccion", "fundacion")
        return principales.mapNotNull { u -> almacen.usuarios.value.firstOrNull { it.usuario == u } }
    }

    override suspend fun cerrar() {
        usuarioActual()?.let { almacen.registrar(it, reloj.ahora(), AccionRegistro.SESION, "Cerró sesión") }
        _estado.value = SesionEstado.Anonima
    }

    override fun cambiarDesbloqueoHuella(activo: Boolean) {
        val actual = (estado.value as? SesionEstado.Activa)?.sesion ?: return
        almacen.desbloqueoHuella.update { if (activo) it + actual.usuario.id else it - actual.usuario.id }
        _estado.value = SesionEstado.Activa(actual.copy(desbloqueoHuella = activo))
    }
}

class FakeEstanciasRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val reloj: Reloj,
) : EstanciasRepository {

    private fun visibles(u: Usuario?, estancias: List<Estancia>) = when (u?.rol) {
        null -> emptyList()
        Rol.FUNDACION -> estancias
        else -> estancias.filter { it.id == u.estanciaId }
    }

    override fun estancias(): Flow<List<Estancia>> =
        combine(sesion.usuario, almacen.estancias) { u, e -> visibles(u, e) }

    override fun salas(): Flow<List<Sala>> =
        combine(sesion.usuario, almacen.salas) { u, s ->
            when (u?.rol) {
                null -> emptyList()
                Rol.FUNDACION -> s
                else -> s.filter { it.estanciaId == u.estanciaId }
            }
        }

    override fun educadoras(): Flow<List<Usuario>> =
        combine(sesion.usuario, almacen.usuarios) { u, us ->
            if (u == null || u.rol == Rol.FAMILIAR) emptyList()
            else us.filter { it.rol == Rol.EDUCADORA && (u.rol == Rol.FUNDACION || it.estanciaId == u.estanciaId) }
        }

    override fun resumenFundacion(): Flow<List<ResumenEstancia>> =
        combine(sesion.usuario, almacen.estancias, almacen.ninos, almacen.asistencias) { u, estancias, ninos, asistencias ->
            if (u?.rol != Rol.FUNDACION && u?.rol != Rol.DIRECCION) return@combine emptyList()
            val hoy = reloj.hoy()
            val mes = YearMonth.from(hoy)
            visibles(u, estancias).map { e ->
                val activos = ninos.filter { it.estanciaId == e.id && it.activo }
                val ids = activos.map { it.id }.toSet()
                val deEstancia = asistencias.filter { it.ninoId in ids }
                val reporte = ReporteAsistencia.de(mes, hoy, activos, emptyList(), deEstancia)
                ResumenEstancia(
                    estancia = e,
                    inscritos = activos.size,
                    presentesHoy = deEstancia.count { it.fecha == hoy && !it.salio },
                    promedioMes = reporte.promedio,
                )
            }
        }
}

class FakeNinosRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val reloj: Reloj,
) : NinosRepository {

    override fun ninos(): Flow<List<Nino>> =
        combine(sesion.usuario, almacen.ninos) { u, n -> Visibilidad.ninos(u, n).sortedBy { it.nombre } }

    override fun nino(id: String): Flow<Nino?> = ninos().map { lista -> lista.firstOrNull { it.id == id } }

    override fun tutoresDe(ninoId: String): Flow<List<Tutor>> =
        combine(nino(ninoId), almacen.tutores) { n, t -> if (n == null) emptyList() else t.filter { it.id in n.tutorIds } }

    private fun direccion(): Usuario {
        val u = sesion.usuarioActual()
        if (u?.rol != Rol.DIRECCION) throw AccesoDenegado()
        return u
    }

    private fun deMiEstancia(id: String, u: Usuario): Nino =
        almacen.ninos.value.firstOrNull { it.id == id && it.estanciaId == u.estanciaId } ?: throw AccesoDenegado()

    override suspend fun guardar(nino: Nino, tutores: List<Tutor>): Nino {
        latencia()
        val u = direccion()
        val existente = almacen.ninos.value.firstOrNull { it.id == nino.id }
        if (existente != null && existente.estanciaId != u.estanciaId) throw AccesoDenegado()
        val sala = almacen.salas.value.firstOrNull { it.id == nino.salaId && it.estanciaId == u.estanciaId } ?: throw AccesoDenegado()
        val guardado = nino.copy(estanciaId = sala.estanciaId, tutorIds = tutores.map { it.id })
        almacen.tutores.update { actuales -> actuales.filter { t -> tutores.none { it.id == t.id } } + tutores }
        almacen.ninos.update { lista -> if (existente == null) lista + guardado else lista.map { if (it.id == guardado.id) guardado else it } }
        val accion = if (existente == null) "Inscribió a" else "Editó el expediente de"
        almacen.registrar(u, reloj.ahora(), AccionRegistro.EXPEDIENTE, "$accion ${guardado.nombreCompleto}", guardado.id)
        return guardado
    }

    override suspend fun darDeBaja(id: String) {
        latencia()
        val u = direccion()
        val n = deMiEstancia(id, u)
        val hoy = reloj.hoy()
        val baja = n.copy(estado = EstadoExpediente.BajaTemporal(hoy, hoy.plusDays(DIAS_PERIODO_BAJA)))
        almacen.ninos.update { lista -> lista.map { if (it.id == id) baja else it } }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.EXPEDIENTE, "Dio de baja temporal a ${n.nombreCompleto}", id)
    }

    override suspend fun reactivar(id: String) {
        latencia()
        val u = direccion()
        val n = deMiEstancia(id, u)
        almacen.ninos.update { lista -> lista.map { if (it.id == id) it.copy(estado = EstadoExpediente.Activo) else it } }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.EXPEDIENTE, "Reactivó el expediente de ${n.nombreCompleto}", id)
    }

    override suspend fun eliminarDefinitivamente(id: String) {
        latencia()
        val u = direccion()
        val n = deMiEstancia(id, u)
        borrarTodo(n)
        almacen.registrar(u, reloj.ahora(), AccionRegistro.BORRADO, "Eliminó definitivamente el expediente de ${n.nombreCompleto} y sus fotos")
    }

    // borra el expediente, su bitacora y toda foto donde aparezca (RF-10, RF-18)
    private fun borrarTodo(n: Nino) {
        almacen.ninos.update { lista -> lista.filterNot { it.id == n.id } }
        almacen.fotos.update { lista -> lista.filterNot { n.id in it.ninoIds } }
        almacen.asistencias.update { lista -> lista.filterNot { it.ninoId == n.id } }
        almacen.eventos.update { lista -> lista.filterNot { it.ninoId == n.id } }
        almacen.incidentes.update { lista -> lista.filterNot { it.ninoId == n.id } }
        almacen.registros.update { lista -> lista.map { if (it.ninoId == n.id) it.copy(ninoId = null) else it } }
        val tutoresEnUso = almacen.ninos.value.flatMap { it.tutorIds }.toSet()
        almacen.tutores.update { lista -> lista.filter { it.id in tutoresEnUso || it.id !in n.tutorIds } }
    }

    override suspend fun purgarVencidos(): Int {
        val hoy = reloj.hoy()
        val vencidos = almacen.ninos.value.filter {
            val e = it.estado
            e is EstadoExpediente.BajaTemporal && !e.eliminarEl.isAfter(hoy)
        }
        vencidos.forEach { n ->
            borrarTodo(n)
            almacen.registrar(
                null, reloj.ahora(), AccionRegistro.BORRADO,
                "Se eliminó el expediente de ${n.nombre} y sus fotos al terminar el periodo de baja",
                estanciaId = n.estanciaId,
            )
        }
        return vencidos.size
    }
}

class FakeJornadaRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val reloj: Reloj,
) : JornadaRepository {

    private val idsVisibles: Flow<Set<String>> =
        combine(sesion.usuario, almacen.ninos) { u, n -> Visibilidad.ninos(u, n).map { it.id }.toSet() }.distinctUntilChanged()

    override fun asistenciasDel(fecha: LocalDate): Flow<List<Asistencia>> =
        combine(idsVisibles, almacen.asistencias) { ids, a -> a.filter { it.fecha == fecha && it.ninoId in ids } }

    override fun asistenciasDe(ninoId: String, desde: LocalDate, hasta: LocalDate): Flow<List<Asistencia>> =
        combine(idsVisibles, almacen.asistencias) { ids, a ->
            if (ninoId !in ids) emptyList()
            else a.filter { it.ninoId == ninoId && !it.fecha.isBefore(desde) && !it.fecha.isAfter(hasta) }.sortedBy { it.fecha }
        }

    override fun eventosDe(ninoId: String, desde: LocalDate, hasta: LocalDate): Flow<List<EventoBitacora>> =
        combine(idsVisibles, almacen.eventos) { ids, e ->
            if (ninoId !in ids) emptyList()
            else e.filter {
                val f = it.cuando.toLocalDate()
                it.ninoId == ninoId && !f.isBefore(desde) && !f.isAfter(hasta)
            }
        }

    override fun eventosDel(fecha: LocalDate): Flow<List<EventoBitacora>> =
        combine(idsVisibles, almacen.eventos) { ids, e -> e.filter { it.cuando.toLocalDate() == fecha && it.ninoId in ids } }

    override fun reporte(mes: YearMonth, estanciaId: String?, salaId: String?): Flow<ReporteAsistencia> =
        combine(sesion.usuario, almacen.ninos, almacen.salas, almacen.asistencias) { u, ninos, salas, asistencias ->
            val estancia = when (u?.rol) {
                Rol.DIRECCION -> u.estanciaId
                Rol.FUNDACION -> estanciaId
                else -> null
            }
            val salasFiltradas = salas.filter { (estancia == null && u?.rol == Rol.FUNDACION) || it.estanciaId == estancia }
                .filter { salaId == null || it.id == salaId }
            val salaIds = salasFiltradas.map { it.id }.toSet()
            val deReporte = if (u == null || u.rol == Rol.EDUCADORA || u.rol == Rol.FAMILIAR) emptyList()
            else ninos.filter { it.activo && it.salaId in salaIds }
            val ids = deReporte.map { it.id }.toSet()
            ReporteAsistencia.de(mes, reloj.hoy(), deReporte, salasFiltradas, asistencias.filter { it.ninoId in ids })
        }

    private fun educadoraCon(ninoId: String): Pair<Usuario, Nino> {
        val u = sesion.usuarioActual()
        if (u?.rol != Rol.EDUCADORA && u?.rol != Rol.DIRECCION) throw AccesoDenegado()
        val n = Visibilidad.ninos(u, almacen.ninos.value).firstOrNull { it.id == ninoId } ?: throw AccesoDenegado()
        return u to n
    }

    override suspend fun registrarLlegada(asistencia: Asistencia) {
        latencia()
        val (u, n) = educadoraCon(asistencia.ninoId)
        // solo se acepta a alguien de la lista de personas autorizadas (RF-15)
        if (n.persona(asistencia.entregadoPorId) == null) throw IllegalArgumentException("Elige a una persona autorizada.")
        val guardada = asistencia.copy(recibidoPorId = u.id)
        almacen.asistencias.update { lista ->
            lista.filterNot { it.ninoId == guardada.ninoId && it.fecha == guardada.fecha } + guardada
        }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.CAPTURA, describirLlegada(n, guardada), n.id)
    }

    override suspend fun registrarSalida(asistencia: Asistencia) {
        latencia()
        val (u, n) = educadoraCon(asistencia.ninoId)
        if (n.persona(asistencia.recogidoPorId) == null) throw IllegalArgumentException("Elige a una persona autorizada.")
        almacen.asistencias.update { lista -> lista.map { if (it.id == asistencia.id) asistencia else it } }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.CAPTURA, describirSalida(n, asistencia), n.id)
    }

    override suspend fun agregarEventos(eventos: List<EventoBitacora>) {
        latencia()
        val ninos = eventos.map { e -> educadoraCon(e.ninoId) }
        almacen.eventos.update { it + eventos }
        val ahora = reloj.ahora()
        eventos.zip(ninos).forEach { (e, par) -> almacen.registrar(par.first, ahora, AccionRegistro.CAPTURA, describirEvento(par.second, e), e.ninoId) }
    }

    override suspend fun terminarSiesta(id: String, fin: LocalTime) {
        latencia()
        val siesta = almacen.eventos.value.firstOrNull { it.id == id } as? EventoBitacora.Siesta ?: return
        val (u, n) = educadoraCon(siesta.ninoId)
        val terminada = siesta.copy(fin = fin, cuando = LocalDateTime.of(siesta.cuando.toLocalDate(), fin))
        almacen.eventos.update { lista -> lista.map { if (it.id == id) terminada else it } }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.EDICION, describirEvento(n, terminada), n.id)
    }

    override suspend fun borrarEvento(id: String) {
        latencia()
        val evento = almacen.eventos.value.firstOrNull { it.id == id } ?: return
        val (u, n) = educadoraCon(evento.ninoId)
        almacen.eventos.update { lista -> lista.filterNot { it.id == id } }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.BORRADO, "Borró: " + describirEvento(n, evento).replaceFirstChar { it.lowercase() }, n.id)
    }
}

class FakeIncidentesRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val reloj: Reloj,
) : IncidentesRepository {

    private val idsVisibles: Flow<Set<String>> =
        combine(sesion.usuario, almacen.ninos) { u, n -> Visibilidad.ninos(u, n).map { it.id }.toSet() }.distinctUntilChanged()

    override fun incidentesDel(fecha: LocalDate): Flow<List<Incidente>> =
        combine(idsVisibles, almacen.incidentes) { ids, i ->
            i.filter { it.cuando.toLocalDate() == fecha && it.ninoId in ids }.sortedByDescending { it.cuando }
        }

    override fun incidentesDe(ninoId: String, desde: LocalDate, hasta: LocalDate): Flow<List<Incidente>> =
        combine(idsVisibles, almacen.incidentes) { ids, i ->
            if (ninoId !in ids) emptyList()
            else i.filter {
                val f = it.cuando.toLocalDate()
                it.ninoId == ninoId && !f.isBefore(desde) && !f.isAfter(hasta)
            }.sortedByDescending { it.cuando }
        }

    override fun incidente(id: String): Flow<Incidente?> =
        combine(idsVisibles, almacen.incidentes) { ids, i -> i.firstOrNull { it.id == id && it.ninoId in ids } }

    override suspend fun registrar(
        ninoId: String,
        cuando: LocalDateTime,
        tipo: TipoIncidente,
        parteDelCuerpo: String?,
        descripcion: String,
        accionTomada: String,
    ): Incidente {
        latencia()
        val u = sesion.usuarioActual()
        if (u?.rol != Rol.EDUCADORA && u?.rol != Rol.DIRECCION) throw AccesoDenegado()
        val n = Visibilidad.ninos(u, almacen.ninos.value).firstOrNull { it.id == ninoId } ?: throw AccesoDenegado()
        val incidente = Incidente(
            UUID.randomUUID().toString(), folioIncidente(almacen.siguienteFolio++), ninoId, cuando, tipo,
            parteDelCuerpo?.takeIf { it.isNotBlank() }, descripcion.trim(), accionTomada.trim(), u.id,
        )
        almacen.incidentes.update { it + incidente }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.CAPTURA, describirIncidente(n, incidente), n.id)
        return incidente
    }
}

class FakeFotosRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val reloj: Reloj,
) : FotosRepository {

    // una foto es visible si el usuario puede ver al menos a uno de los ninos etiquetados;
    // la familia ademas solo ve fotos donde aparece su hijo (RF-09)
    private val fotosVisibles: Flow<List<Foto>> =
        combine(sesion.usuario, almacen.ninos, almacen.fotos, almacen.salas) { u, ninos, fotos, salas ->
            val ids = Visibilidad.ninos(u, ninos).map { it.id }.toSet()
            when (u?.rol) {
                Rol.FAMILIAR -> fotos.filter { f -> f.ninoIds.any { it in ids } }
                Rol.EDUCADORA -> fotos.filter { it.salaId == u.salaId }
                Rol.DIRECCION -> {
                    val propias = salas.filter { it.estanciaId == u.estanciaId }.map { it.id }.toSet()
                    fotos.filter { it.salaId in propias }
                }
                Rol.FUNDACION, null -> emptyList()
            }.sortedByDescending { it.cuando }
        }

    override fun fotosDeSala(salaId: String): Flow<List<Foto>> = fotosVisibles.map { l -> l.filter { it.salaId == salaId } }

    override fun fotosDe(ninoId: String): Flow<List<Foto>> = fotosVisibles.map { l -> l.filter { ninoId in it.ninoIds } }

    override fun foto(id: String): Flow<Foto?> = fotosVisibles.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun registrarConsulta(id: String) {
        val u = sesion.usuarioActual() ?: return
        val f = almacen.fotos.value.firstOrNull { it.id == id } ?: return
        val ninoPropio = Visibilidad.ninos(u, almacen.ninos.value).firstOrNull { it.id in f.ninoIds }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.FOTO_CONSULTADA, "Consultó la foto \"${f.descripcion}\"", ninoPropio?.id)
    }

    override suspend fun agregar(foto: Foto) {
        latencia()
        val u = sesion.usuarioActual()
        if (u?.rol != Rol.EDUCADORA || foto.salaId != u.salaId) throw AccesoDenegado()
        if (foto.ninoIds.isEmpty()) throw IllegalArgumentException("Etiqueta al menos a un niño.")
        val deSala = Visibilidad.ninos(u, almacen.ninos.value).associateBy { it.id }
        // se valida aqui y no solo en la pantalla (RF-16)
        val sinPermiso = foto.ninoIds.filter { deSala[it]?.puedeAparecerEn(foto.uso) != true }
        if (sinPermiso.isNotEmpty()) throw AccesoDenegado("Hay niños sin autorización de imagen para este uso.")
        almacen.fotos.update { listOf(foto) + it }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.CAPTURA, "Agregó una foto: ${foto.descripcion}")
    }

    override suspend fun borrar(id: String) {
        latencia()
        val u = sesion.usuarioActual() ?: throw AccesoDenegado()
        val f = almacen.fotos.value.firstOrNull { it.id == id } ?: return
        val visibles = Visibilidad.ninos(u, almacen.ninos.value).map { it.id }.toSet()
        val puede = when (u.rol) {
            Rol.FAMILIAR -> f.ninoIds.any { it in visibles }
            Rol.EDUCADORA -> f.salaId == u.salaId
            Rol.DIRECCION -> almacen.salas.value.any { it.id == f.salaId && it.estanciaId == u.estanciaId }
            Rol.FUNDACION -> false
        }
        if (!puede) throw AccesoDenegado()
        // se borra para todos, no solo para quien lo pidio (RF-10)
        almacen.fotos.update { lista -> lista.filterNot { it.id == id } }
        val ninoId = f.ninoIds.firstOrNull { it in visibles }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.BORRADO, "Borró la foto \"${f.descripcion}\" para todas las familias", ninoId,
            estanciaId = almacen.salas.value.firstOrNull { it.id == f.salaId }?.estanciaId)
    }
}

class FakeAvisosRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val reloj: Reloj,
) : AvisosRepository {

    override fun avisos(): Flow<List<Aviso>> =
        combine(sesion.usuario, almacen.avisos) { u, a ->
            if (u?.estanciaId == null) emptyList() else a.filter { it.estanciaId == u.estanciaId }.sortedByDescending { it.publicadoEl }
        }

    override fun leidos(): Flow<Set<String>> =
        combine(sesion.usuario, almacen.avisosLeidos) { u, l -> if (u == null) emptySet() else l[u.id].orEmpty() }

    override suspend fun marcarLeidos(ids: Collection<String>) {
        val u = sesion.usuarioActual() ?: return
        almacen.avisosLeidos.update { mapa -> mapa + (u.id to (mapa[u.id].orEmpty() + ids)) }
    }

    override suspend fun publicar(tipo: TipoAviso, titulo: String, cuerpo: String, fechaEvento: LocalDateTime?): Aviso {
        latencia()
        val u = sesion.usuarioActual()
        if (u?.rol != Rol.DIRECCION || u.estanciaId == null) throw AccesoDenegado()
        val aviso = Aviso(UUID.randomUUID().toString(), u.estanciaId, tipo, titulo.trim(), cuerpo.trim(), fechaEvento, reloj.ahora(), u.id)
        almacen.avisos.update { it + aviso }
        almacen.avisosLeidos.update { mapa -> mapa + (u.id to (mapa[u.id].orEmpty() + aviso.id)) }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.AVISO, "Publicó el aviso \"${aviso.titulo}\"")
        return aviso
    }
}

class FakeHorariosRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
    private val reloj: Reloj,
) : HorariosRepository {

    private val salasVisibles: Flow<Set<String>> =
        combine(sesion.usuario, almacen.salas, almacen.ninos) { u, salas, ninos ->
            when (u?.rol) {
                null, Rol.FUNDACION -> emptySet()
                Rol.FAMILIAR -> Visibilidad.ninos(u, ninos).map { it.salaId }.toSet()
                else -> salas.filter { it.estanciaId == u.estanciaId }.map { it.id }.toSet()
            }
        }

    override fun bloques(salaId: String): Flow<List<BloqueHorario>> =
        combine(salasVisibles, almacen.bloques) { s, b -> if (salaId in s) b.filter { it.salaId == salaId }.sortedBy { it.inicio } else emptyList() }

    override fun especiales(salaId: String): Flow<List<ActividadEspecial>> =
        combine(salasVisibles, almacen.especiales) { s, e ->
            if (salaId in s) e.filter { it.salaId == salaId }.sortedWith(compareBy({ it.fecha }, { it.inicio })) else emptyList()
        }

    private fun direccionDe(salaId: String): Usuario {
        val u = sesion.usuarioActual()
        if (u?.rol != Rol.DIRECCION) throw AccesoDenegado()
        if (almacen.salas.value.none { it.id == salaId && it.estanciaId == u.estanciaId }) throw AccesoDenegado()
        return u
    }

    private fun nombreSala(id: String) = almacen.salas.value.firstOrNull { it.id == id }?.nombre ?: ""

    override suspend fun guardarBloque(bloque: BloqueHorario) {
        latencia()
        val u = direccionDe(bloque.salaId)
        val existe = almacen.bloques.value.any { it.id == bloque.id }
        almacen.bloques.update { lista -> lista.filterNot { it.id == bloque.id } + bloque }
        val verbo = if (existe) "Editó" else "Agregó"
        almacen.registrar(u, reloj.ahora(), if (existe) AccionRegistro.EDICION else AccionRegistro.CAPTURA,
            "$verbo el bloque \"${bloque.actividad}\" del horario de ${nombreSala(bloque.salaId)}")
    }

    override suspend fun borrarBloque(id: String) {
        latencia()
        val bloque = almacen.bloques.value.firstOrNull { it.id == id } ?: return
        val u = direccionDe(bloque.salaId)
        almacen.bloques.update { lista -> lista.filterNot { it.id == id } }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.BORRADO, "Borró el bloque \"${bloque.actividad}\" del horario de ${nombreSala(bloque.salaId)}")
    }

    override suspend fun guardarEspecial(actividad: ActividadEspecial) {
        latencia()
        val u = direccionDe(actividad.salaId)
        almacen.especiales.update { lista -> lista.filterNot { it.id == actividad.id } + actividad }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.CAPTURA, "Publicó la actividad especial \"${actividad.titulo}\" en ${nombreSala(actividad.salaId)}")
    }

    override suspend fun borrarEspecial(id: String) {
        latencia()
        val actividad = almacen.especiales.value.firstOrNull { it.id == id } ?: return
        val u = direccionDe(actividad.salaId)
        almacen.especiales.update { lista -> lista.filterNot { it.id == id } }
        almacen.registrar(u, reloj.ahora(), AccionRegistro.BORRADO, "Borró la actividad especial \"${actividad.titulo}\"")
    }
}

class FakeActividadRepository(
    private val almacen: AlmacenMemoria,
    private val sesion: FakeSesionRepository,
) : ActividadRepository {

    override fun registros(): Flow<List<RegistroActividad>> =
        combine(sesion.usuario, almacen.registros) { u, r ->
            if (u?.rol != Rol.DIRECCION) emptyList() else r.filter { it.estanciaId == u.estanciaId }.sortedByDescending { it.cuando }
        }
}
