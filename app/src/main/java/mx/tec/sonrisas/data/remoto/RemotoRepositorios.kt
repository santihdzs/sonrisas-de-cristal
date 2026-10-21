package mx.tec.sonrisas.data.remoto

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.tec.sonrisas.data.AbrirDirecta
import mx.tec.sonrisas.data.ActividadRepository
import mx.tec.sonrisas.data.ArchivoEntrada
import mx.tec.sonrisas.data.ArchivoMuyGrande
import mx.tec.sonrisas.data.AvisosRepository
import mx.tec.sonrisas.data.Contacto
import mx.tec.sonrisas.data.CuentasRepository
import mx.tec.sonrisas.data.EdicionMaterial
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.Exportacion
import mx.tec.sonrisas.data.FotosRepository
import mx.tec.sonrisas.data.GuardarNino
import mx.tec.sonrisas.data.Hora
import mx.tec.sonrisas.data.HorariosRepository
import mx.tec.sonrisas.data.Id
import mx.tec.sonrisas.data.Ids
import mx.tec.sonrisas.data.IncidentesRepository
import mx.tec.sonrisas.data.JornadaRepository
import mx.tec.sonrisas.data.LIMITE_BYTES
import mx.tec.sonrisas.data.MaterialRepository
import mx.tec.sonrisas.data.MensajeChat
import mx.tec.sonrisas.data.MensajesRepository
import mx.tec.sonrisas.data.NinosRepository
import mx.tec.sonrisas.data.NuevaCuenta
import mx.tec.sonrisas.data.NuevoAviso
import mx.tec.sonrisas.data.NuevoIncidente
import mx.tec.sonrisas.data.Numero
import mx.tec.sonrisas.data.ObjetosPerdidosRepository
import mx.tec.sonrisas.data.ResumenConversacion
import mx.tec.sonrisas.data.ResumenEstancia
import mx.tec.sonrisas.data.Texto
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.Credenciales
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.MaterialClase
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.ObjetoPerdido
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.ReporteAsistencia
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.domain.Usuario
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

// implementaciones contra el servidor; las reglas de privacidad las aplica el servidor, no la app

/** lo que comparten los repositorios remotos */
class Remoto(val api: Api, val cambios: Cambios) {
    inline fun <reified T> consulta(ruta: String, intervalo: Long = SONDEO_NORMAL): Flow<T> =
        cambios.consulta(intervalo) { api.get<T>(ruta) }

    inline fun <reified T> consultaONulo(ruta: String, intervalo: Long = SONDEO_NORMAL): Flow<T?> =
        cambios.consulta(intervalo) { api.getONulo<T>(ruta) }

    /** toda escritura avisa a las consultas abiertas para que se actualicen de inmediato */
    suspend fun <T> cambio(bloque: suspend Api.() -> T): T = api.bloque().also { cambios.avisar() }

    /** marcas de leido o consultado: si no hay red se pierden sin molestar a nadie */
    suspend fun silencioso(bloque: suspend Api.() -> Unit) {
        try {
            api.bloque()
            cambios.avisar()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    /** baja una sola vez el archivo del servidor y devuelve la ruta local que usan las pantallas */
    suspend fun local(carpeta: File, id: String, extension: String): String {
        val archivo = File(carpeta, "$id.$extension")
        if (!archivo.exists()) {
            try {
                api.descargar("/archivos/${codificar(id)}", archivo)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // la pantalla avisa que no pudo abrir el archivo; se reintenta en la siguiente consulta
            }
        }
        return archivo.path
    }

    /** sube el archivo tal cual; el limite se vuelve a contar aqui y en el servidor */
    suspend fun subir(ruta: String, archivo: ArchivoEntrada): String {
        if ((archivo.tamano ?: 0) > LIMITE_BYTES) throw ArchivoMuyGrande()
        return api.pedir("POST", ruta, archivo.tipo, { salida -> archivo.abrir().use { copiarConLimite(it, salida) } }) { it.readBytes().decodeToString() }
            .also { cambios.avisar() }
    }

    private fun copiarConLimite(entrada: java.io.InputStream, salida: OutputStream) {
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val leidos = entrada.read(buffer)
            if (leidos < 0) break
            total += leidos
            if (total > LIMITE_BYTES) throw ArchivoMuyGrande()
            salida.write(buffer, 0, leidos)
        }
    }
}

private fun rango(ninoId: String, desde: LocalDate, hasta: LocalDate) = "nino=${codificar(ninoId)}&desde=$desde&hasta=$hasta"

class RemotoEstanciasRepository(private val r: Remoto) : EstanciasRepository {
    override fun estancias(): Flow<List<Estancia>> = r.consulta("/estancias")
    override fun salas(): Flow<List<Sala>> = r.consulta("/salas")
    override fun educadoras(): Flow<List<Usuario>> = r.consulta("/educadoras")
    override fun personal(): Flow<List<Usuario>> = r.consulta("/personal")
    override fun resumenFundacion(): Flow<List<ResumenEstancia>> = r.consulta("/resumen")
    override suspend fun guardarEstancia(estancia: Estancia): Estancia = r.cambio { enviar("PUT", "/estancias", estancia).como() }
    override suspend fun guardarSala(sala: Sala): Sala = r.cambio { enviar("PUT", "/salas", sala).como() }
}

class RemotoNinosRepository(private val r: Remoto) : NinosRepository {
    override fun ninos(): Flow<List<Nino>> = r.consulta("/ninos")
    override fun nino(id: String): Flow<Nino?> = r.consultaONulo("/ninos/${codificar(id)}")
    override fun tutoresDe(ninoId: String): Flow<List<Tutor>> = r.consulta("/ninos/${codificar(ninoId)}/tutores")
    override fun tutoresDeEstancia(): Flow<List<Tutor>> = r.consulta("/tutores")
    override suspend fun guardar(nino: Nino, tutores: List<Tutor>): Nino = r.cambio { enviar("PUT", "/ninos", GuardarNino(nino, tutores)).como() }
    override suspend fun darDeBaja(id: String) { r.cambio { post("/ninos/${codificar(id)}/baja") } }
    override suspend fun reactivar(id: String) { r.cambio { post("/ninos/${codificar(id)}/reactivar") } }
    override suspend fun eliminarDefinitivamente(id: String) { r.cambio { delete("/ninos/${codificar(id)}") } }

    // con servidor la purga corre alla, cada pocas horas
    override suspend fun purgarVencidos(): Int = 0
}

class RemotoJornadaRepository(private val r: Remoto) : JornadaRepository {
    override fun asistenciasDel(fecha: LocalDate): Flow<List<Asistencia>> = r.consulta("/asistencias?fecha=$fecha")
    override fun asistenciasDe(ninoId: String, desde: LocalDate, hasta: LocalDate): Flow<List<Asistencia>> =
        r.consulta("/asistencias?${rango(ninoId, desde, hasta)}")
    override fun eventosDe(ninoId: String, desde: LocalDate, hasta: LocalDate): Flow<List<EventoBitacora>> =
        r.consulta("/eventos?${rango(ninoId, desde, hasta)}")
    override fun eventosDel(fecha: LocalDate): Flow<List<EventoBitacora>> = r.consulta("/eventos?fecha=$fecha")
    override fun reporte(mes: YearMonth, estanciaId: String?, salaId: String?): Flow<ReporteAsistencia> =
        r.consulta("/reporte?mes=$mes" + (estanciaId?.let { "&estancia=${codificar(it)}" } ?: "") + (salaId?.let { "&sala=${codificar(it)}" } ?: ""))
    override suspend fun registrarLlegada(asistencia: Asistencia) { r.cambio { enviar("POST", "/asistencias/llegada", asistencia) } }
    override suspend fun registrarSalida(asistencia: Asistencia) { r.cambio { enviar("POST", "/asistencias/salida", asistencia) } }
    override suspend fun agregarEventos(eventos: List<EventoBitacora>) { r.cambio { enviar("POST", "/eventos", eventos) } }
    override suspend fun terminarSiesta(id: String, fin: LocalTime) { r.cambio { enviar("POST", "/eventos/${codificar(id)}/fin", Hora(fin)) } }
    override suspend fun borrarEvento(id: String) { r.cambio { delete("/eventos/${codificar(id)}") } }
    override suspend fun registrarExportacion(ninoIds: List<String>, detalle: String) {
        r.cambio { enviar("POST", "/exportaciones", Exportacion(ninoIds, detalle)) }
    }
}

class RemotoIncidentesRepository(private val r: Remoto) : IncidentesRepository {
    override fun incidentesDel(fecha: LocalDate): Flow<List<Incidente>> = r.consulta("/incidentes?fecha=$fecha")
    override fun incidentesDe(ninoId: String, desde: LocalDate, hasta: LocalDate): Flow<List<Incidente>> =
        r.consulta("/incidentes?${rango(ninoId, desde, hasta)}")
    override fun incidente(id: String): Flow<Incidente?> = r.consultaONulo("/incidentes/${codificar(id)}")
    override suspend fun registrar(
        ninoId: String,
        cuando: LocalDateTime,
        tipo: TipoIncidente,
        parteDelCuerpo: String?,
        descripcion: String,
        accionTomada: String,
    ): Incidente = r.cambio { enviar("POST", "/incidentes", NuevoIncidente(ninoId, cuando, tipo, parteDelCuerpo, descripcion, accionTomada)).como() }
}

class RemotoFotosRepository(private val r: Remoto, private val carpeta: File) : FotosRepository {
    private suspend fun conImagenLocal(f: Foto) = f.copy(archivo = f.archivo?.let { r.local(carpeta, it, "jpg") })
    private fun lista(ruta: String): Flow<List<Foto>> = r.cambios.consulta { r.api.get<List<Foto>>(ruta).map { conImagenLocal(it) } }

    override fun fotosDeSala(salaId: String): Flow<List<Foto>> = lista("/fotos?sala=${codificar(salaId)}")
    override fun fotosDe(ninoId: String): Flow<List<Foto>> = lista("/fotos?nino=${codificar(ninoId)}")
    override fun foto(id: String): Flow<Foto?> = r.cambios.consulta { r.api.getONulo<Foto>("/fotos/${codificar(id)}")?.let { conImagenLocal(it) } }
    override suspend fun registrarConsulta(id: String) = r.silencioso { post("/fotos/${codificar(id)}/consulta") }

    // primero se sube la imagen y luego la foto con el id que dio el servidor
    override suspend fun agregar(foto: Foto, imagen: ArchivoEntrada) {
        val archivo = r.subir("/fotos/imagen?sala=${codificar(foto.salaId)}", imagen).como<Id>().id
        r.cambio { enviar("POST", "/fotos", foto.copy(archivo = archivo)) }
    }
    override suspend fun borrar(id: String) { r.cambio { delete("/fotos/${codificar(id)}") } }
}

class RemotoAvisosRepository(private val r: Remoto) : AvisosRepository {
    override fun avisos(): Flow<List<Aviso>> = r.consulta("/avisos")
    override fun leidos(): Flow<Set<String>> = r.consulta<List<String>>("/avisos/leidos").map { it.toSet() }
    override suspend fun marcarLeidos(ids: Collection<String>) = r.silencioso { enviar("POST", "/avisos/leidos", Ids(ids.toList())) }
    override suspend fun publicar(tipo: TipoAviso, titulo: String, cuerpo: String, fechaEvento: LocalDateTime?): Aviso =
        r.cambio { enviar("POST", "/avisos", NuevoAviso(tipo, titulo, cuerpo, fechaEvento)).como() }
}

class RemotoObjetosRepository(private val r: Remoto, private val carpeta: File) : ObjetosPerdidosRepository {
    private suspend fun conFotoLocal(o: ObjetoPerdido) = o.copy(foto = o.foto?.let { r.local(carpeta, it, "img") })

    override fun objetos(salaId: String): Flow<List<ObjetoPerdido>> =
        r.cambios.consulta { r.api.get<List<ObjetoPerdido>>("/objetos?sala=${codificar(salaId)}").map { conFotoLocal(it) } }

    override fun vistos(): Flow<Set<String>> = r.consulta<List<String>>("/objetos/vistos").map { it.toSet() }
    override suspend fun marcarVistos(ids: Collection<String>) = r.silencioso { enviar("POST", "/objetos/vistos", Ids(ids.toList())) }

    override suspend fun publicar(salaId: String, descripcion: String, dondeSeEncontro: String, fecha: LocalDate, foto: ArchivoEntrada?): ObjetoPerdido {
        val ruta = "/objetos?sala=${codificar(salaId)}&descripcion=${codificar(descripcion)}&donde=${codificar(dondeSeEncontro)}&fecha=$fecha"
        val texto = if (foto != null) r.subir(ruta, foto) else r.cambio { post(ruta) }
        return conFotoLocal(texto.como())
    }

    override suspend fun reclamar(id: String, nota: String?) { r.cambio { enviar("POST", "/objetos/${codificar(id)}/reclamo", Texto(nota)) } }
    override suspend fun entregar(id: String) { r.cambio { post("/objetos/${codificar(id)}/entrega") } }
    override suspend fun rechazarReclamo(id: String) { r.cambio { post("/objetos/${codificar(id)}/rechazo") } }
}

class RemotoMaterialRepository(private val r: Remoto, private val carpeta: File) : MaterialRepository {
    private suspend fun conArchivoLocal(m: MaterialClase) =
        m.copy(ruta = r.local(carpeta, m.ruta, m.nombreArchivo.substringAfterLast('.', "bin").take(5)))

    override fun material(salaId: String): Flow<List<MaterialClase>> =
        r.cambios.consulta { r.api.get<List<MaterialClase>>("/material?sala=${codificar(salaId)}").map { conArchivoLocal(it) } }

    override suspend fun subir(salaId: String, titulo: String, descripcion: String, archivo: ArchivoEntrada): MaterialClase {
        val ruta = "/material?sala=${codificar(salaId)}&titulo=${codificar(titulo)}&descripcion=${codificar(descripcion)}&nombre=${codificar(archivo.nombre)}"
        return conArchivoLocal(r.subir(ruta, archivo).como())
    }

    override suspend fun editar(id: String, titulo: String, descripcion: String) {
        r.cambio { enviar("PUT", "/material/${codificar(id)}", EdicionMaterial(titulo, descripcion)) }
    }

    override suspend fun borrar(id: String) { r.cambio { delete("/material/${codificar(id)}") } }
}

class RemotoMensajesRepository(private val r: Remoto) : MensajesRepository {
    override fun conversaciones(): Flow<List<ResumenConversacion>> = r.consulta("/conversaciones", SONDEO_CHAT)
    override fun conversacion(id: String): Flow<ResumenConversacion?> = r.consultaONulo("/conversaciones/${codificar(id)}", SONDEO_CHAT)
    override fun mensajes(conversacionId: String): Flow<List<MensajeChat>> = r.consulta("/conversaciones/${codificar(conversacionId)}/mensajes", SONDEO_CHAT)
    override fun noLeidos(): Flow<Int> = r.consulta<Numero>("/mensajes/no-leidos", SONDEO_CHAT).map { it.valor }
    override suspend fun enviar(conversacionId: String, texto: String) {
        r.cambio { enviar("POST", "/conversaciones/${codificar(conversacionId)}/mensajes", Texto(texto)) }
    }
    override suspend fun marcarLeida(conversacionId: String) = r.silencioso { post("/conversaciones/${codificar(conversacionId)}/leida") }
    override fun contactos(): Flow<List<Contacto>> = r.consulta("/contactos")
    override suspend fun abrirDirecta(usuarioId: String, salaId: String): String =
        r.cambio { enviar("POST", "/directas", AbrirDirecta(usuarioId, salaId)).como<Id>().id }
}

class RemotoHorariosRepository(private val r: Remoto) : HorariosRepository {
    override fun bloques(salaId: String): Flow<List<BloqueHorario>> = r.consulta("/salas/${codificar(salaId)}/bloques")
    override fun especiales(salaId: String): Flow<List<ActividadEspecial>> = r.consulta("/salas/${codificar(salaId)}/especiales")
    override suspend fun guardarBloque(bloque: BloqueHorario) { r.cambio { enviar("PUT", "/bloques", bloque) } }
    override suspend fun borrarBloque(id: String) { r.cambio { delete("/bloques/${codificar(id)}") } }
    override suspend fun guardarEspecial(actividad: ActividadEspecial) { r.cambio { enviar("PUT", "/especiales", actividad) } }
    override suspend fun borrarEspecial(id: String) { r.cambio { delete("/especiales/${codificar(id)}") } }
}

class RemotoCuentasRepository(private val r: Remoto) : CuentasRepository {
    override fun cuentas(): Flow<List<Usuario>> = r.consulta("/cuentas")
    override suspend fun crearDireccion(estanciaId: String, nombre: String): Credenciales =
        r.cambio { enviar("POST", "/cuentas/direccion", NuevaCuenta(nombre, estanciaId = estanciaId)).como() }
    override suspend fun crearEducadora(nombre: String, salaId: String): Credenciales =
        r.cambio { enviar("POST", "/cuentas/educadora", NuevaCuenta(nombre, salaId = salaId)).como() }
    override suspend fun asignarSala(usuarioId: String, salaId: String) { r.cambio { enviar("POST", "/cuentas/${codificar(usuarioId)}/sala", Id(salaId)) } }
    override suspend fun retirarAcceso(usuarioId: String) { r.cambio { post("/cuentas/${codificar(usuarioId)}/retiro") } }
    override suspend fun crearCuentaFamilia(tutorId: String): Credenciales = r.cambio { enviar("POST", "/cuentas/familia", Id(tutorId)).como() }
}

class RemotoActividadRepository(private val r: Remoto) : ActividadRepository {
    override fun registros(): Flow<List<RegistroActividad>> = r.consulta("/registros")
}
