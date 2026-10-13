package mx.tec.sonrisas.servidor

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.request.contentLength
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.ArchivoMuyGrande
import mx.tec.sonrisas.data.EdicionMaterial
import mx.tec.sonrisas.data.Error
import mx.tec.sonrisas.data.Ids
import mx.tec.sonrisas.data.LIMITE_BYTES
import mx.tec.sonrisas.data.NuevoAviso
import mx.tec.sonrisas.data.Texto
import mx.tec.sonrisas.data.tipoMaterialPermitido
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.EstadoObjeto
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.MaterialClase
import mx.tec.sonrisas.domain.ObjetoPerdido
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.Validaciones
import mx.tec.sonrisas.domain.puedeAparecerEn
import java.time.LocalDate

// FotosRepository, AvisosRepository, ObjetosPerdidosRepository y MaterialRepository

/** el archivo viene como cuerpo crudo de la peticion; se corta en cuanto pasa del limite aunque el tamano declarado mienta */
private suspend fun RoutingContext.leerArchivo(): ByteArray? {
    if ((call.request.contentLength() ?: 0) > LIMITE_BYTES) throw ArchivoMuyGrande()
    val bytes = call.receiveChannel().readRemaining(LIMITE_BYTES + 1).readByteArray()
    if (bytes.size > LIMITE_BYTES) throw ArchivoMuyGrande()
    return bytes.takeIf { it.isNotEmpty() }
}

private fun RoutingContext.tipoArchivo(): String =
    call.request.header(HttpHeaders.ContentType)?.substringBefore(';')?.trim()?.lowercase() ?: "application/octet-stream"

private fun RoutingContext.parametro(nombre: String): String =
    call.request.queryParameters[nombre] ?: throw IllegalArgumentException("Falta $nombre.")

private fun Conexion.guardarArchivo(id: String, salaId: String, tipo: String, bytes: ByteArray) =
    ejecutar("INSERT INTO archivos (id, sala_id, tipo, bytes) VALUES (?, ?, ?, ?)", id, salaId, tipo, bytes)

// una foto es visible si el usuario puede ver al menos a uno de los ninos etiquetados;
// la familia ademas solo ve fotos donde aparece su hijo (RF-09)
private fun Ctx.fotosVisibles(): List<Foto> = when (u.rol) {
    Rol.FAMILIAR -> {
        val ids = ninosVisibles().map { it.id }.toSet()
        Tablas.fotos.en("sala_id", salasEstancia().map { it.id }).filter { f -> f.ninoIds.any { it in ids } }
    }
    Rol.EDUCADORA -> u.salaId?.let { Tablas.fotos.donde("sala_id = ?", it) }.orEmpty()
    Rol.DIRECCION -> Tablas.fotos.en("sala_id", salasEstancia().map { it.id })
    Rol.FUNDACION -> emptyList()
}.sortedByDescending { it.cuando }

private fun Ctx.objeto(id: String): ObjetoPerdido = Tablas.objetos.porId(id) ?: throw AccesoDenegado()

private fun Ctx.marcarVistos(ids: Collection<String>) = ids.forEach {
    ejecutar("DELETE FROM objetos_vistos WHERE usuario_id = ? AND objeto_id = ?", u.id, it)
    ejecutar("INSERT INTO objetos_vistos (usuario_id, objeto_id) VALUES (?, ?)", u.id, it)
}

private fun Ctx.marcarAvisos(ids: Collection<String>) = ids.forEach {
    ejecutar("DELETE FROM avisos_leidos WHERE usuario_id = ? AND aviso_id = ?", u.id, it)
    ejecutar("INSERT INTO avisos_leidos (usuario_id, aviso_id) VALUES (?, ?)", u.id, it)
}

// la educadora solo cambia lo que ella subio; la direccion puede corregir cualquier material de su estancia
private fun Ctx.materialEditable(id: String): Pair<MaterialClase, String> {
    val m = Tablas.material.porId(id) ?: throw AccesoDenegado()
    val sala = personalDe(m.salaId)
    if (u.rol != Rol.DIRECCION && m.subidoPorId != u.id) throw AccesoDenegado()
    return m to sala.nombre
}

fun Route.rutasContenido(db: Db) {
    // fotos

    get("/fotos") {
        val salaId = call.request.queryParameters["sala"]
        val ninoId = call.request.queryParameters["nino"]
        lee(db) { fotosVisibles().filter { (salaId == null || it.salaId == salaId) && (ninoId == null || ninoId in it.ninoIds) } }
    }

    get("/fotos/{id}") { lee(db) { fotosVisibles().firstOrNull { it.id == call.parameters["id"] } } }

    post("/fotos/{id}/consulta") {
        escribe(db) {
            val f = Tablas.fotos.porId(call.parameters["id"]!!) ?: return@escribe Unit
            val propio = ninosVisibles().firstOrNull { it.id in f.ninoIds } ?: return@escribe Unit
            registrar(AccionRegistro.FOTO_CONSULTADA, "Consultó la foto \"${f.descripcion}\"", propio.id)
        }
    }

    post("/fotos") {
        val foto = call.receive<Foto>()
        escribe(db) {
            if (u.rol != Rol.EDUCADORA || foto.salaId != u.salaId) throw AccesoDenegado()
            if (foto.ninoIds.isEmpty()) throw IllegalArgumentException("Etiqueta al menos a un niño.")
            if (Tablas.fotos.porId(foto.id)?.let { it.salaId != u.salaId } == true) throw AccesoDenegado()
            val deSala = ninosVisibles().associateBy { it.id }
            // se valida aqui y no solo en la pantalla (RF-16)
            if (foto.ninoIds.any { deSala[it]?.puedeAparecerEn(foto.uso) != true }) {
                throw AccesoDenegado("Hay niños sin autorización de imagen para este uso.")
            }
            Tablas.fotos.guardar(foto)
            registrar(AccionRegistro.CAPTURA, "Agregó una foto: ${foto.descripcion}")
        }
    }

    delete("/fotos/{id}") {
        escribe(db) {
            val f = Tablas.fotos.porId(call.parameters["id"]!!) ?: return@escribe Unit
            val visibles = ninosVisibles().map { it.id }.toSet()
            val sala = Tablas.salas.porId(f.salaId)
            val puede = when (u.rol) {
                Rol.FAMILIAR -> f.ninoIds.any { it in visibles }
                Rol.EDUCADORA -> f.salaId == u.salaId
                Rol.DIRECCION -> sala?.estanciaId == u.estanciaId
                Rol.FUNDACION -> false
            }
            if (!puede) throw AccesoDenegado()
            // se borra para todos, no solo para quien lo pidio (RF-10)
            Tablas.fotos.borrar(f.id)
            registrar(
                AccionRegistro.BORRADO, "Borró la foto \"${f.descripcion}\" para todas las familias",
                f.ninoIds.firstOrNull { it in visibles }, estanciaId = sala?.estanciaId,
            )
        }
    }

    // avisos

    get("/avisos") {
        lee(db) { u.estanciaId?.let { Tablas.avisos.donde("estancia_id = ?", it) }.orEmpty().sortedByDescending { it.publicadoEl } }
    }

    get("/avisos/leidos") {
        lee(db) { consultar("SELECT aviso_id FROM avisos_leidos WHERE usuario_id = ?", u.id) { it.getString(1) } }
    }

    post("/avisos/leidos") {
        val ids = call.receive<Ids>().ids
        escribe(db) { marcarAvisos(ids) }
    }

    post("/avisos") {
        val p = call.receive<NuevoAviso>()
        escribe(db) {
            val estanciaId = direccion()
            if (Validaciones.aviso(p.titulo, p.cuerpo).isNotEmpty()) throw IllegalArgumentException("Revisa el aviso.")
            val aviso = Aviso(nuevoId(), estanciaId, p.tipo, p.titulo.trim(), p.cuerpo.trim(), p.fechaEvento, ahora, u.id)
            Tablas.avisos.guardar(aviso)
            marcarAvisos(listOf(aviso.id))
            registrar(AccionRegistro.AVISO, "Publicó el aviso \"${aviso.titulo}\"")
            aviso
        }
    }

    // objetos perdidos

    get("/objetos") {
        val salaId = parametro("sala")
        lee(db) {
            if (salaId !in salasVisibles()) return@lee emptyList<ObjetoPerdido>()
            Tablas.objetos.donde("sala_id = ?", salaId)
                // la familia ve que un objeto esta reclamado, pero no quien lo reclamo si no fue ella
                .map { if (u.rol == Rol.FAMILIAR && it.reclamadoPorId != u.id) it.copy(reclamadoPor = null, notaReclamo = null) else it }
                .sortedByDescending { it.publicadoEl }
        }
    }

    get("/objetos/vistos") {
        lee(db) { consultar("SELECT objeto_id FROM objetos_vistos WHERE usuario_id = ?", u.id) { it.getString(1) } }
    }

    post("/objetos/vistos") {
        val ids = call.receive<Ids>().ids
        escribe(db) { marcarVistos(ids) }
    }

    post("/objetos") {
        val salaId = parametro("sala")
        val descripcion = parametro("descripcion")
        val donde = parametro("donde")
        val fecha = LocalDate.parse(parametro("fecha"))
        val tipo = tipoArchivo()
        val foto = leerArchivo()
        escribe(db) {
            val s = personalDe(salaId)
            if (descripcion.isBlank() || donde.isBlank()) throw IllegalArgumentException("Faltan datos del objeto.")
            if (fecha.isAfter(hoy)) throw IllegalArgumentException("La fecha no puede ser futura.")
            if (foto != null && !tipo.startsWith("image/")) throw IllegalArgumentException("La foto debe ser una imagen.")
            val id = nuevoId()
            val fotoId = foto?.let { nuevoId().also { archivo -> guardarArchivo(archivo, s.id, tipo, it) } }
            val nuevo = ObjetoPerdido(id, s.id, descripcion.trim(), donde.trim(), fecha, fotoId, u.id, ahora)
            Tablas.objetos.guardar(nuevo)
            // quien lo publica no lo ve como nuevo
            marcarVistos(listOf(id))
            registrar(AccionRegistro.OBJETO, "Publicó el objeto perdido \"${nuevo.descripcion}\" en la sala ${s.nombre}", estanciaId = s.estanciaId)
            nuevo
        }
    }

    post("/objetos/{id}/reclamo") {
        val nota = call.receive<Texto>().texto
        escribe(db) {
            if (u.rol != Rol.FAMILIAR) throw AccesoDenegado()
            val o = objeto(call.parameters["id"]!!)
            val hijos = ninosVisibles().filter { it.salaId == o.salaId }
            if (hijos.isEmpty()) throw AccesoDenegado()
            if (o.estado != EstadoObjeto.DISPONIBLE) throw IllegalStateException("Alguien más ya reclamó este objeto.")
            val parentesco = u.tutorId?.let { Tablas.tutores.porId(it) }?.parentesco?.lowercase() ?: "familiar"
            val quien = "${u.nombre} ($parentesco de ${hijos.joinToString(" y ") { it.nombre }})"
            Tablas.objetos.guardar(
                o.copy(
                    estado = EstadoObjeto.RECLAMADO, reclamadoPorId = u.id, reclamadoPor = quien,
                    notaReclamo = nota?.trim()?.takeIf { it.isNotBlank() }, reclamadoEl = ahora,
                ),
            )
            val estancia = Tablas.salas.porId(o.salaId)?.estanciaId
            registrar(AccionRegistro.OBJETO, "Reclamó el objeto perdido \"${o.descripcion}\"", hijos.first().id, estanciaId = estancia)
        }
    }

    post("/objetos/{id}/entrega") {
        escribe(db) {
            val o = objeto(call.parameters["id"]!!)
            val s = personalDe(o.salaId)
            if (o.estado != EstadoObjeto.RECLAMADO) throw IllegalStateException("Solo se entrega un objeto reclamado.")
            Tablas.objetos.guardar(o.copy(estado = EstadoObjeto.ENTREGADO, entregadoEl = ahora))
            registrar(AccionRegistro.OBJETO, "Entregó el objeto perdido \"${o.descripcion}\" a ${o.reclamadoPor}", estanciaId = s.estanciaId)
        }
    }

    post("/objetos/{id}/rechazo") {
        escribe(db) {
            val o = objeto(call.parameters["id"]!!)
            val s = personalDe(o.salaId)
            if (o.estado != EstadoObjeto.RECLAMADO) return@escribe Unit
            Tablas.objetos.guardar(o.copy(estado = EstadoObjeto.DISPONIBLE, reclamadoPorId = null, reclamadoPor = null, notaReclamo = null, reclamadoEl = null))
            registrar(AccionRegistro.OBJETO, "Rechazó el reclamo de ${o.reclamadoPor} sobre \"${o.descripcion}\"", estanciaId = s.estanciaId)
        }
    }

    // material de clase

    get("/material") {
        val salaId = parametro("sala")
        lee(db) {
            if (salaId !in salasVisibles()) emptyList()
            else Tablas.material.donde("sala_id = ?", salaId).sortedByDescending { it.subidoEl }
        }
    }

    post("/material") {
        val salaId = parametro("sala")
        val titulo = parametro("titulo")
        val descripcion = call.request.queryParameters["descripcion"].orEmpty()
        val nombre = parametro("nombre")
        val tipo = tipoArchivo()
        val bytes = leerArchivo() ?: throw IllegalArgumentException("Falta el archivo.")
        escribe(db) {
            val s = personalDe(salaId)
            if (titulo.isBlank()) throw IllegalArgumentException("Escribe el título.")
            if (!tipoMaterialPermitido(tipo)) throw IllegalArgumentException("Tipo de archivo no permitido.")
            val id = nuevoId()
            guardarArchivo(id, s.id, tipo, bytes)
            val nuevo = MaterialClase(id, s.id, titulo.trim(), descripcion.trim(), nombre, tipo, bytes.size.toLong(), id, u.id, ahora)
            Tablas.material.guardar(nuevo)
            registrar(AccionRegistro.MATERIAL, "Subió el material \"${nuevo.titulo}\" a la sala ${s.nombre}", estanciaId = s.estanciaId)
            nuevo
        }
    }

    put("/material/{id}") {
        val p = call.receive<EdicionMaterial>()
        escribe(db) {
            val (m, sala) = materialEditable(call.parameters["id"]!!)
            if (p.titulo.isBlank()) throw IllegalArgumentException("Escribe el título.")
            Tablas.material.guardar(m.copy(titulo = p.titulo.trim(), descripcion = p.descripcion.trim()))
            registrar(AccionRegistro.MATERIAL, "Editó el material \"${m.titulo}\" de la sala $sala")
        }
    }

    delete("/material/{id}") {
        escribe(db) {
            val (m, sala) = materialEditable(call.parameters["id"]!!)
            Tablas.material.borrar(m.id)
            // el archivo se borra de la base, no solo de la lista
            ejecutar("DELETE FROM archivos WHERE id = ?", m.ruta)
            registrar(AccionRegistro.MATERIAL, "Borró el material \"${m.titulo}\" de la sala $sala")
        }
    }

    // archivos de material y fotos de objetos; solo para quien puede ver esa sala

    get("/archivos/{id}") {
        val u = call.principal<Usuario>()!!
        val id = call.parameters["id"]!!
        val archivo = db.leer { con ->
            val ctx = Ctx(con, u)
            ctx.consultar("SELECT sala_id, tipo, bytes FROM archivos WHERE id = ?", id) { Triple(it.getString(1), it.getString(2), it.getBytes(3)) }
                .firstOrNull()?.takeIf { it.first in ctx.salasVisibles() }
        }
        if (archivo == null) {
            call.respond(HttpStatusCode.NotFound, Error("No encontrado."))
        } else {
            call.response.header(HttpHeaders.ContentDisposition, ContentDisposition.Attachment.toString())
            call.respondBytes(archivo.third, ContentType.parse(archivo.second))
        }
    }
}
