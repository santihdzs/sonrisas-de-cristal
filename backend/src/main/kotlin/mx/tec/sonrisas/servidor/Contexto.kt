package mx.tec.sonrisas.servidor

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.Error
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.RelojSistema
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.Visibilidad
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

fun nuevoId(): String = UUID.randomUUID().toString()

fun Conexion.registrar(
    autor: Usuario?,
    cuando: LocalDateTime,
    accion: AccionRegistro,
    detalle: String,
    ninoId: String? = null,
    estanciaId: String? = autor?.estanciaId,
) = Tablas.registros.guardar(
    RegistroActividad(nuevoId(), cuando, autor?.id ?: "sistema", autor?.nombre ?: "Sistema", accion, detalle, ninoId, estanciaId),
)

// shortcut: reloj global para que las pruebas fijen "ahora"; pasarlo por modulo() si las pruebas corren en paralelo
var relojServidor: Reloj = RelojSistema

/** una peticion autenticada: la conexion, quien la hace y la hora del servidor */
class Ctx(con: Connection, val u: Usuario) : Conexion(con) {
    val ahora: LocalDateTime = relojServidor.ahora()
    val hoy: LocalDate = ahora.toLocalDate()

    // la app deja elegir la hora (se captura despues de que paso), pero el dia es el del servidor y la hora no puede ser futura;
    // asi un reloj alterado en el telefono no fecha registros en otro dia
    fun horaDeHoy(h: LocalTime): LocalTime = minOf(h, ahora.toLocalTime())

    fun momentoDeHoy(c: LocalDateTime): LocalDateTime = LocalDateTime.of(hoy, horaDeHoy(c.toLocalTime()))

    fun registrar(accion: AccionRegistro, detalle: String, ninoId: String? = null, estanciaId: String? = u.estanciaId) =
        registrar(u, ahora, accion, detalle, ninoId, estanciaId)

    // todas las reglas son por estancia, asi que basta con cargar la estancia del usuario
    fun ninosEstancia(): List<Nino> = u.estanciaId?.let { Tablas.ninos.donde("estancia_id = ?", it) }.orEmpty()

    fun salasEstancia(): List<Sala> = u.estanciaId?.let { Tablas.salas.donde("estancia_id = ?", it) }.orEmpty()

    fun ninosVisibles(): List<Nino> = Visibilidad.ninos(u, ninosEstancia())

    fun ninoVisible(id: String): Nino? = ninosVisibles().firstOrNull { it.id == id }

    /** salas cuyo material, objetos perdidos y chat puede ver el usuario */
    fun salasVisibles(): Set<String> = Visibilidad.salas(u, salasEstancia(), ninosEstancia())

    fun direccion(): String {
        val estancia = u.estanciaId
        if (u.rol != Rol.DIRECCION || estancia == null) throw AccesoDenegado()
        return estancia
    }

    fun salaDeMiEstancia(salaId: String): Sala =
        Tablas.salas.porId(salaId)?.takeIf { it.estanciaId == u.estanciaId } ?: throw AccesoDenegado()

    /** educadoras de la sala y direccion de su estancia */
    fun personalDe(salaId: String): Sala {
        val s = Tablas.salas.porId(salaId)
        if (!Visibilidad.esPersonalDe(u, s)) throw AccesoDenegado()
        return s!!
    }

    /** la educadora o la direccion que captura algo de un nino que puede ver */
    fun capturaDe(ninoId: String): Nino {
        if (u.rol != Rol.EDUCADORA && u.rol != Rol.DIRECCION) throw AccesoDenegado()
        return ninoVisible(ninoId) ?: throw AccesoDenegado()
    }
}

suspend inline fun <reified R : Any> RoutingContext.responder(r: R?) {
    when (r) {
        null -> call.respond(HttpStatusCode.NotFound, Error("No encontrado."))
        is Unit -> call.respond(HttpStatusCode.NoContent)
        else -> call.respond(r)
    }
}

/** lectura con el usuario de la sesion */
suspend inline fun <reified R : Any> RoutingContext.lee(db: Db, crossinline bloque: Ctx.() -> R?) {
    val u = call.principal<Usuario>()!!
    responder(db.leer { Ctx(it, u).bloque() })
}

/** escritura en una transaccion; si algo falla no queda nada a medias */
suspend inline fun <reified R : Any> RoutingContext.escribe(db: Db, crossinline bloque: Ctx.() -> R?) {
    val u = call.principal<Usuario>()!!
    responder(db.escribir { Ctx(it, u).bloque() })
}
