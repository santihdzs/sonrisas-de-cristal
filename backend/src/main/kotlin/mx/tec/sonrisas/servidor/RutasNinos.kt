package mx.tec.sonrisas.servidor

import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.Exportacion
import mx.tec.sonrisas.data.GuardarNino
import mx.tec.sonrisas.data.Hora
import mx.tec.sonrisas.data.NuevoIncidente
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.DIAS_PERIODO_BAJA
import mx.tec.sonrisas.domain.EstadoExpediente
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.ReporteAsistencia
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.describirEvento
import mx.tec.sonrisas.domain.describirIncidente
import mx.tec.sonrisas.domain.describirLlegada
import mx.tec.sonrisas.domain.describirSalida
import mx.tec.sonrisas.domain.folioIncidente
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

// NinosRepository, JornadaRepository, IncidentesRepository y HorariosRepository

private fun RoutingContext.fecha(nombre: String): LocalDate? = call.request.queryParameters[nombre]?.let(LocalDate::parse)

/** borra el expediente, su bitacora y toda foto donde aparezca (RF-10, RF-18) */
fun Conexion.borrarTodo(n: Nino) {
    Tablas.ninos.borrar(n.id)
    val salas = Tablas.salas.donde("estancia_id = ?", n.estanciaId).map { it.id }
    Tablas.fotos.en("sala_id", salas).filter { n.id in it.ninoIds }.forEach { Tablas.fotos.borrar(it.id) }
    Tablas.asistencias.borrarDonde("nino_id = ?", n.id)
    Tablas.eventos.borrarDonde("nino_id = ?", n.id)
    Tablas.incidentes.borrarDonde("nino_id = ?", n.id)
    Tablas.registros.guardar(Tablas.registros.donde("nino_id = ?", n.id).map { it.copy(ninoId = null) })
    val enUso = Tablas.ninos.donde("estancia_id = ?", n.estanciaId).flatMap { it.tutorIds }.toSet()
    n.tutorIds.filter { it !in enUso }.forEach { Tablas.tutores.borrar(it) }
}

/** expedientes cuya baja ya vencio; corre en el servidor al arrancar y cada pocas horas */
fun Conexion.purgarVencidos(): Int {
    val hoy = LocalDate.now()
    val vencidos = Tablas.ninos.todos().filter { val e = it.estado; e is EstadoExpediente.BajaTemporal && !e.eliminarEl.isAfter(hoy) }
    vencidos.forEach { n ->
        borrarTodo(n)
        registrar(
            null, LocalDateTime.now(), AccionRegistro.BORRADO,
            "Se eliminó el expediente de ${n.nombre} y sus fotos al terminar el periodo de baja", estanciaId = n.estanciaId,
        )
    }
    return vencidos.size
}

private fun Ctx.deMiEstancia(id: String): Nino {
    direccion()
    return Tablas.ninos.porId(id)?.takeIf { it.estanciaId == u.estanciaId } ?: throw AccesoDenegado()
}

private fun EventoBitacora.conAutor(usuarioId: String): EventoBitacora = when (this) {
    is EventoBitacora.Alimento -> copy(registradoPorId = usuarioId)
    is EventoBitacora.Siesta -> copy(registradoPorId = usuarioId)
    is EventoBitacora.Evacuacion -> copy(registradoPorId = usuarioId)
    is EventoBitacora.Animo -> copy(registradoPorId = usuarioId)
    is EventoBitacora.Nota -> copy(registradoPorId = usuarioId)
}

private fun Ctx.salasDeHorario(): Set<String> = when (u.rol) {
    Rol.FUNDACION -> emptySet()
    Rol.FAMILIAR -> ninosVisibles().map { it.salaId }.toSet()
    else -> salasEstancia().map { it.id }.toSet()
}

private fun Ctx.direccionDeSala(salaId: String): String {
    direccion()
    return salaDeMiEstancia(salaId).nombre
}

fun Route.rutasNinos(db: Db) {
    // ninos

    get("/ninos") { lee(db) { ninosVisibles().sortedBy { it.nombre } } }

    get("/ninos/{id}") { lee(db) { ninoVisible(call.parameters["id"]!!) } }

    get("/ninos/{id}/tutores") {
        lee(db) { ninoVisible(call.parameters["id"]!!)?.let { Tablas.tutores.en("id", it.tutorIds) }.orEmpty() }
    }

    get("/tutores") {
        lee(db) {
            if (u.rol != Rol.DIRECCION) emptyList()
            else Tablas.tutores.en("id", ninosEstancia().flatMap { it.tutorIds }.toSet()).sortedBy { it.nombre }
        }
    }

    put("/ninos") {
        val p = call.receive<GuardarNino>()
        escribe(db) {
            val estanciaId = direccion()
            val existente = Tablas.ninos.porId(p.nino.id)
            if (existente != null && existente.estanciaId != estanciaId) throw AccesoDenegado()
            val sala = salaDeMiEstancia(p.nino.salaId)
            // un tutor que ya existe solo se puede reusar si pertenece a esta estancia
            val deMiEstancia = ninosEstancia().flatMap { it.tutorIds }.toSet()
            val existentes = Tablas.tutores.en("id", p.tutores.map { it.id }).map { it.id }
            if (existentes.any { it !in deMiEstancia }) throw AccesoDenegado()
            val guardado = p.nino.copy(estanciaId = sala.estanciaId, tutorIds = p.tutores.map { it.id })
            Tablas.tutores.guardar(p.tutores)
            Tablas.ninos.guardar(guardado)
            val accion = if (existente == null) "Inscribió a" else "Editó el expediente de"
            registrar(AccionRegistro.EXPEDIENTE, "$accion ${guardado.nombreCompleto}", guardado.id)
            guardado
        }
    }

    post("/ninos/{id}/baja") {
        escribe(db) {
            val n = deMiEstancia(call.parameters["id"]!!)
            Tablas.ninos.guardar(n.copy(estado = EstadoExpediente.BajaTemporal(hoy, hoy.plusDays(DIAS_PERIODO_BAJA))))
            registrar(AccionRegistro.EXPEDIENTE, "Dio de baja temporal a ${n.nombreCompleto}", n.id)
        }
    }

    post("/ninos/{id}/reactivar") {
        escribe(db) {
            val n = deMiEstancia(call.parameters["id"]!!)
            Tablas.ninos.guardar(n.copy(estado = EstadoExpediente.Activo))
            registrar(AccionRegistro.EXPEDIENTE, "Reactivó el expediente de ${n.nombreCompleto}", n.id)
        }
    }

    delete("/ninos/{id}") {
        escribe(db) {
            val n = deMiEstancia(call.parameters["id"]!!)
            // P2: la app solo ofrece borrar expedientes en baja; el servidor no confia en eso y lo vuelve a revisar (409)
            if (n.estado !is EstadoExpediente.BajaTemporal) throw IllegalStateException("Primero da de baja el expediente.")
            borrarTodo(n)
            registrar(AccionRegistro.BORRADO, "Eliminó definitivamente el expediente de ${n.nombreCompleto} y sus fotos")
        }
    }

    // jornada

    get("/asistencias") {
        val dia = fecha("fecha")
        val ninoId = call.request.queryParameters["nino"]
        val desde = fecha("desde")
        val hasta = fecha("hasta")
        lee(db) {
            if (dia != null) Tablas.asistencias.en("nino_id", ninosVisibles().map { it.id }, "AND fecha = ?", dia)
            else if (ninoId == null || desde == null || hasta == null) throw IllegalArgumentException("Faltan fechas.")
            else if (ninoVisible(ninoId) == null) emptyList()
            else Tablas.asistencias.donde("nino_id = ? AND fecha >= ? AND fecha <= ?", ninoId, desde, hasta).sortedBy { it.fecha }
        }
    }

    get("/eventos") {
        val dia = fecha("fecha")
        val ninoId = call.request.queryParameters["nino"]
        val desde = fecha("desde")
        val hasta = fecha("hasta")
        lee(db) {
            if (dia != null) Tablas.eventos.en("nino_id", ninosVisibles().map { it.id }, "AND fecha = ?", dia)
            else if (ninoId == null || desde == null || hasta == null) throw IllegalArgumentException("Faltan fechas.")
            else if (ninoVisible(ninoId) == null) emptyList()
            else Tablas.eventos.donde("nino_id = ? AND fecha >= ? AND fecha <= ?", ninoId, desde, hasta)
        }
    }

    get("/reporte") {
        val mes = YearMonth.parse(call.request.queryParameters["mes"] ?: throw IllegalArgumentException("Falta el mes."))
        val estanciaPedida = call.request.queryParameters["estancia"]
        val salaId = call.request.queryParameters["sala"]
        lee(db) {
            val estancia = when (u.rol) {
                Rol.DIRECCION -> u.estanciaId
                Rol.FUNDACION -> estanciaPedida
                else -> null
            }
            val salas = when {
                estancia != null -> Tablas.salas.donde("estancia_id = ?", estancia)
                u.rol == Rol.FUNDACION -> Tablas.salas.todos()
                else -> emptyList()
            }.filter { salaId == null || it.id == salaId }
            val salaIds = salas.map { it.id }
            val ninos = if (u.rol == Rol.EDUCADORA || u.rol == Rol.FAMILIAR) emptyList()
            else Tablas.ninos.en("sala_id", salaIds).filter { it.activo }
            val asistencias = Tablas.asistencias.en("nino_id", ninos.map { it.id }, "AND fecha >= ? AND fecha <= ?", mes.atDay(1), mes.atEndOfMonth())
            val reporte = ReporteAsistencia.de(mes, hoy, ninos, salas, asistencias)
            // la fundacion solo recibe datos consolidados, sin nombres de ninos (RF-17)
            if (u.rol == Rol.FUNDACION) reporte.copy(filas = emptyList()) else reporte
        }
    }

    post("/asistencias/llegada") {
        val a = call.receive<Asistencia>()
        escribe(db) {
            val n = capturaDe(a.ninoId)
            // solo se acepta a alguien de la lista de personas autorizadas (RF-15)
            if (n.persona(a.entregadoPorId) == null) throw IllegalArgumentException("Elige a una persona autorizada.")
            val otra = Tablas.asistencias.porId(a.id)
            if (otra != null && otra.ninoId != n.id) throw AccesoDenegado()
            val guardada = a.copy(recibidoPorId = u.id)
            Tablas.asistencias.borrarDonde("nino_id = ? AND fecha = ?", n.id, guardada.fecha)
            Tablas.asistencias.guardar(guardada)
            registrar(AccionRegistro.CAPTURA, describirLlegada(n, guardada), n.id)
        }
    }

    post("/asistencias/salida") {
        val a = call.receive<Asistencia>()
        escribe(db) {
            val n = capturaDe(a.ninoId)
            if (n.persona(a.recogidoPorId) == null) throw IllegalArgumentException("Elige a una persona autorizada.")
            val existente = Tablas.asistencias.porId(a.id)?.takeIf { it.ninoId == n.id } ?: throw AccesoDenegado()
            // la llegada no cambia al registrar la salida
            val guardada = a.copy(recibidoPorId = existente.recibidoPorId, entregoSalidaId = u.id)
            Tablas.asistencias.guardar(guardada)
            registrar(AccionRegistro.CAPTURA, describirSalida(n, guardada), n.id)
        }
    }

    post("/eventos") {
        val eventos = call.receive<List<EventoBitacora>>()
        escribe(db) {
            // el autor siempre sale de la sesion, no de lo que mande la app (RF-14)
            val firmados = eventos.map { e ->
                val n = capturaDe(e.ninoId)
                val otro = Tablas.eventos.porId(e.id)
                if (otro != null && otro.ninoId != n.id) throw AccesoDenegado()
                e.conAutor(u.id) to n
            }
            Tablas.eventos.guardar(firmados.map { it.first })
            firmados.forEach { (e, n) -> registrar(AccionRegistro.CAPTURA, describirEvento(n, e), n.id) }
        }
    }

    post("/eventos/{id}/fin") {
        val fin = call.receive<Hora>().hora
        escribe(db) {
            val siesta = Tablas.eventos.porId(call.parameters["id"]!!) as? EventoBitacora.Siesta ?: return@escribe Unit
            val n = capturaDe(siesta.ninoId)
            val terminada = siesta.copy(fin = fin, cuando = LocalDateTime.of(siesta.cuando.toLocalDate(), fin))
            Tablas.eventos.guardar(terminada)
            registrar(AccionRegistro.EDICION, describirEvento(n, terminada), n.id)
        }
    }

    delete("/eventos/{id}") {
        escribe(db) {
            val evento = Tablas.eventos.porId(call.parameters["id"]!!) ?: return@escribe Unit
            val n = capturaDe(evento.ninoId)
            Tablas.eventos.borrar(evento.id)
            registrar(AccionRegistro.BORRADO, "Borró: " + describirEvento(n, evento).replaceFirstChar { it.lowercase() }, n.id)
        }
    }

    post("/exportaciones") {
        val p = call.receive<Exportacion>()
        escribe(db) {
            val visibles = ninosVisibles().associateBy { it.id }
            if (p.ninoIds.isEmpty() || p.ninoIds.any { it !in visibles }) throw AccesoDenegado()
            val estancia = visibles.getValue(p.ninoIds.first()).estanciaId
            registrar(AccionRegistro.IMPRESION, p.detalle, p.ninoIds.singleOrNull(), estanciaId = estancia)
        }
    }

    // incidentes

    get("/incidentes") {
        val dia = fecha("fecha")
        val ninoId = call.request.queryParameters["nino"]
        val desde = fecha("desde")
        val hasta = fecha("hasta")
        lee(db) {
            if (dia != null) Tablas.incidentes.en("nino_id", ninosVisibles().map { it.id }, "AND fecha = ?", dia).sortedByDescending { it.cuando }
            else if (ninoId == null || desde == null || hasta == null) throw IllegalArgumentException("Faltan fechas.")
            else if (ninoVisible(ninoId) == null) emptyList()
            else Tablas.incidentes.donde("nino_id = ? AND fecha >= ? AND fecha <= ?", ninoId, desde, hasta).sortedByDescending { it.cuando }
        }
    }

    get("/incidentes/{id}") {
        lee(db) { Tablas.incidentes.porId(call.parameters["id"]!!)?.takeIf { ninoVisible(it.ninoId) != null } }
    }

    post("/incidentes") {
        val p = call.receive<NuevoIncidente>()
        escribe(db) {
            val n = capturaDe(p.ninoId)
            ejecutar("UPDATE contadores SET valor = valor + 1 WHERE nombre = 'folio'")
            val numero = consultar("SELECT valor FROM contadores WHERE nombre = 'folio'") { it.getInt(1) }.firstOrNull()
                ?: 1.also { ejecutar("INSERT INTO contadores (nombre, valor) VALUES ('folio', 1)") }
            val incidente = Incidente(
                nuevoId(), folioIncidente(numero), n.id, p.cuando, p.tipo,
                p.parteDelCuerpo?.takeIf { it.isNotBlank() }, p.descripcion.trim(), p.accionTomada.trim(), u.id,
            )
            Tablas.incidentes.guardar(incidente)
            registrar(AccionRegistro.CAPTURA, describirIncidente(n, incidente), n.id)
            incidente
        }
    }

    // horarios

    get("/salas/{id}/bloques") {
        val salaId = call.parameters["id"]!!
        lee(db) { if (salaId in salasDeHorario()) Tablas.bloques.donde("sala_id = ?", salaId).sortedBy { it.inicio } else emptyList() }
    }

    get("/salas/{id}/especiales") {
        val salaId = call.parameters["id"]!!
        lee(db) {
            if (salaId in salasDeHorario()) Tablas.especiales.donde("sala_id = ?", salaId).sortedWith(compareBy({ it.fecha }, { it.inicio }))
            else emptyList()
        }
    }

    put("/bloques") {
        val bloque = call.receive<BloqueHorario>()
        escribe(db) {
            val sala = direccionDeSala(bloque.salaId)
            val existente = Tablas.bloques.porId(bloque.id)
            if (existente != null) direccionDeSala(existente.salaId)
            Tablas.bloques.guardar(bloque)
            val verbo = if (existente != null) "Editó" else "Agregó"
            registrar(if (existente != null) AccionRegistro.EDICION else AccionRegistro.CAPTURA, "$verbo el bloque \"${bloque.actividad}\" del horario de $sala")
        }
    }

    delete("/bloques/{id}") {
        escribe(db) {
            val bloque = Tablas.bloques.porId(call.parameters["id"]!!) ?: return@escribe Unit
            val sala = direccionDeSala(bloque.salaId)
            Tablas.bloques.borrar(bloque.id)
            registrar(AccionRegistro.BORRADO, "Borró el bloque \"${bloque.actividad}\" del horario de $sala")
        }
    }

    put("/especiales") {
        val actividad = call.receive<ActividadEspecial>()
        escribe(db) {
            val sala = direccionDeSala(actividad.salaId)
            Tablas.especiales.porId(actividad.id)?.let { direccionDeSala(it.salaId) }
            Tablas.especiales.guardar(actividad)
            registrar(AccionRegistro.CAPTURA, "Publicó la actividad especial \"${actividad.titulo}\" en $sala")
        }
    }

    delete("/especiales/{id}") {
        escribe(db) {
            val actividad = Tablas.especiales.porId(call.parameters["id"]!!) ?: return@escribe Unit
            direccionDeSala(actividad.salaId)
            Tablas.especiales.borrar(actividad.id)
            registrar(AccionRegistro.BORRADO, "Borró la actividad especial \"${actividad.titulo}\"")
        }
    }
}
