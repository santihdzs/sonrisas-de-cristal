package mx.tec.sonrisas.data

import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Conversacion
import mx.tec.sonrisas.domain.Credenciales
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.MaterialClase
import mx.tec.sonrisas.domain.Mensaje
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.ObjetoPerdido
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.ReporteAsistencia
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.domain.Usuario
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

interface EstanciasRepository {
    /** la fundacion ve todas; el personal y las familias solo la suya */
    fun estancias(): Flow<List<Estancia>>
    fun salas(): Flow<List<Sala>>
    fun educadoras(): Flow<List<Usuario>>

    /** personal de la estancia (educadoras y direccion), para mostrar quien registro algo */
    fun personal(): Flow<List<Usuario>>
    fun resumenFundacion(): Flow<List<ResumenEstancia>>

    /** solo la fundacion registra o edita estancias */
    suspend fun guardarEstancia(estancia: Estancia): Estancia

    /** solo la direccion, y solo salas de su estancia */
    suspend fun guardarSala(sala: Sala): Sala
}

interface NinosRepository {
    /** ya viene filtrado por la sesion: sala, estancia o hijos del tutor */
    fun ninos(): Flow<List<Nino>>
    fun nino(id: String): Flow<Nino?>
    fun tutoresDe(ninoId: String): Flow<List<Tutor>>

    /** tutores ya registrados en la estancia, para que los hermanos compartan una sola cuenta (solo direccion) */
    fun tutoresDeEstancia(): Flow<List<Tutor>>
    suspend fun guardar(nino: Nino, tutores: List<Tutor>): Nino
    suspend fun darDeBaja(id: String)
    suspend fun reactivar(id: String)
    suspend fun eliminarDefinitivamente(id: String)
    suspend fun purgarVencidos(): Int
}

interface JornadaRepository {
    fun asistenciasDel(fecha: LocalDate): Flow<List<Asistencia>>
    fun asistenciasDe(ninoId: String, desde: LocalDate, hasta: LocalDate): Flow<List<Asistencia>>
    fun eventosDe(ninoId: String, desde: LocalDate, hasta: LocalDate): Flow<List<EventoBitacora>>
    fun eventosDel(fecha: LocalDate): Flow<List<EventoBitacora>>
    fun reporte(mes: YearMonth, estanciaId: String?, salaId: String?): Flow<ReporteAsistencia>
    suspend fun registrarLlegada(asistencia: Asistencia)
    suspend fun registrarSalida(asistencia: Asistencia)
    suspend fun agregarEventos(eventos: List<EventoBitacora>)
    suspend fun terminarSiesta(id: String, fin: LocalTime)
    suspend fun borrarEvento(id: String)

    /** deja constancia de cada bitacora impresa o compartida; falla si algun nino no es visible para el usuario */
    suspend fun registrarExportacion(ninoIds: List<String>, detalle: String)
}

interface IncidentesRepository {
    fun incidentesDel(fecha: LocalDate): Flow<List<Incidente>>
    fun incidentesDe(ninoId: String, desde: LocalDate, hasta: LocalDate): Flow<List<Incidente>>
    fun incidente(id: String): Flow<Incidente?>
    suspend fun registrar(
        ninoId: String,
        cuando: LocalDateTime,
        tipo: TipoIncidente,
        parteDelCuerpo: String?,
        descripcion: String,
        accionTomada: String,
    ): Incidente
}

interface FotosRepository {
    fun fotosDeSala(salaId: String): Flow<List<Foto>>
    fun fotosDe(ninoId: String): Flow<List<Foto>>
    fun foto(id: String): Flow<Foto?>
    suspend fun registrarConsulta(id: String)
    suspend fun agregar(foto: Foto, imagen: ArchivoEntrada)
    suspend fun borrar(id: String)
}

interface AvisosRepository {
    fun avisos(): Flow<List<Aviso>>
    fun leidos(): Flow<Set<String>>
    suspend fun marcarLeidos(ids: Collection<String>)
    suspend fun publicar(tipo: TipoAviso, titulo: String, cuerpo: String, fechaEvento: LocalDateTime?): Aviso
}

interface ObjetosPerdidosRepository {
    /** el personal ve los de su sala o estancia; la familia, solo los de las salas de sus hijos */
    fun objetos(salaId: String): Flow<List<ObjetoPerdido>>
    fun vistos(): Flow<Set<String>>
    suspend fun marcarVistos(ids: Collection<String>)
    suspend fun publicar(salaId: String, descripcion: String, dondeSeEncontro: String, fecha: LocalDate, foto: ArchivoEntrada?): ObjetoPerdido
    suspend fun reclamar(id: String, nota: String?)
    suspend fun entregar(id: String)
    suspend fun rechazarReclamo(id: String)
}

interface MaterialRepository {
    /** el personal ve el de su sala o estancia; la familia, solo el de las salas de sus hijos */
    fun material(salaId: String): Flow<List<MaterialClase>>
    suspend fun subir(salaId: String, titulo: String, descripcion: String, archivo: ArchivoEntrada): MaterialClase

    /** quien lo subio o la direccion */
    suspend fun editar(id: String, titulo: String, descripcion: String)
    suspend fun borrar(id: String)
}

interface MensajesRepository {
    /** solo las conversaciones de las que el usuario es miembro, la mas reciente primero */
    fun conversaciones(): Flow<List<ResumenConversacion>>
    fun conversacion(id: String): Flow<ResumenConversacion?>
    fun mensajes(conversacionId: String): Flow<List<MensajeChat>>
    fun noLeidos(): Flow<Int>
    suspend fun enviar(conversacionId: String, texto: String)
    suspend fun marcarLeida(conversacionId: String)

    fun contactos(): Flow<List<Contacto>>

    /** devuelve la conversacion directa existente o crea una nueva */
    suspend fun abrirDirecta(usuarioId: String, salaId: String): String
}

interface HorariosRepository {
    fun bloques(salaId: String): Flow<List<BloqueHorario>>
    fun especiales(salaId: String): Flow<List<ActividadEspecial>>
    suspend fun guardarBloque(bloque: BloqueHorario)
    suspend fun borrarBloque(id: String)
    suspend fun guardarEspecial(actividad: ActividadEspecial)
    suspend fun borrarEspecial(id: String)
}

interface CuentasRepository {
    /** la fundacion ve las cuentas de direccion; la direccion, las de su estancia */
    fun cuentas(): Flow<List<Usuario>>

    /** solo la fundacion; la contrasena temporal se devuelve una sola vez */
    suspend fun crearDireccion(estanciaId: String, nombre: String): Credenciales

    /** solo la direccion, con una sala de su estancia */
    suspend fun crearEducadora(nombre: String, salaId: String): Credenciales
    suspend fun asignarSala(usuarioId: String, salaId: String)

    /** la cuenta deja de poder entrar, pero su historial se conserva */
    suspend fun retirarAcceso(usuarioId: String)

    /** cuenta de familia para un tutor de la estancia que todavia no tiene acceso */
    suspend fun crearCuentaFamilia(tutorId: String): Credenciales

    /** solo la fundacion: cuentas bloqueadas por intentos fallidos desde varios lugares */
    fun bloqueadas(): Flow<List<Usuario>>
    suspend fun desbloquear(usuarioId: String)
}

interface ActividadRepository {
    fun registros(): Flow<List<RegistroActividad>>
}
