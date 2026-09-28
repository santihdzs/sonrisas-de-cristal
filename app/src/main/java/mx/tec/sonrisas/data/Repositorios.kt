package mx.tec.sonrisas.data

import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
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

class AccesoDenegado(mensaje: String = "No tienes permiso para hacer esto.") : IllegalStateException(mensaje)

data class ResumenEstancia(
    val estancia: Estancia,
    val inscritos: Int,
    val presentesHoy: Int,
    val promedioMes: Int,
)

interface EstanciasRepository {
    /** la fundacion ve todas; el personal y las familias solo la suya */
    fun estancias(): Flow<List<Estancia>>
    fun salas(): Flow<List<Sala>>
    fun educadoras(): Flow<List<Usuario>>

    /** personal de la estancia (educadoras y direccion), para mostrar quien registro algo */
    fun personal(): Flow<List<Usuario>>
    fun resumenFundacion(): Flow<List<ResumenEstancia>>
}

interface NinosRepository {
    /** ya viene filtrado por la sesion: sala, estancia o hijos del tutor */
    fun ninos(): Flow<List<Nino>>
    fun nino(id: String): Flow<Nino?>
    fun tutoresDe(ninoId: String): Flow<List<Tutor>>
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
    suspend fun agregar(foto: Foto)
    suspend fun borrar(id: String)
}

interface AvisosRepository {
    fun avisos(): Flow<List<Aviso>>
    fun leidos(): Flow<Set<String>>
    suspend fun marcarLeidos(ids: Collection<String>)
    suspend fun publicar(tipo: TipoAviso, titulo: String, cuerpo: String, fechaEvento: LocalDateTime?): Aviso
}

interface HorariosRepository {
    fun bloques(salaId: String): Flow<List<BloqueHorario>>
    fun especiales(salaId: String): Flow<List<ActividadEspecial>>
    suspend fun guardarBloque(bloque: BloqueHorario)
    suspend fun borrarBloque(id: String)
    suspend fun guardarEspecial(actividad: ActividadEspecial)
    suspend fun borrarEspecial(id: String)
}

interface ActividadRepository {
    fun registros(): Flow<List<RegistroActividad>>
}
