package mx.tec.sonrisas.servidor

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.Activo
import mx.tec.sonrisas.data.ArchivoMuyGrande
import mx.tec.sonrisas.data.Error
import mx.tec.sonrisas.data.JsonApi
import mx.tec.sonrisas.data.Login
import mx.tec.sonrisas.data.Perfil
import mx.tec.sonrisas.data.RespuestaLogin
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.Usuario
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** sin argumentos arranca el servidor; "reiniciar-demo" borra todo, vuelve a cargar los datos de prueba con la fecha de hoy y termina */
fun main(args: Array<String>) {
    val env = System.getenv()
    val demo = env["DATOS_DEMO"] != "false"
    val reiniciar = args.firstOrNull() == "reiniciar-demo"
    // una base de produccion nunca se borra
    if (reiniciar && !demo) error("reiniciar-demo solo funciona con DATOS_DEMO distinto de false.")
    val db = Db.desde(env["DATABASE_URL"])
    println(runBlocking { Semilla.preparar(db, demo, reiniciar, env["CLAVE_FUNDACION"]) })
    if (reiniciar) {
        db.cerrar()
        return
    }
    embeddedServer(Netty, port = env["PORT"]?.toIntOrNull() ?: 8080) {
        modulo(db, proxyConfiable = env["PROXY_CONFIABLE"] == "true")
    }.start(wait = true)
}

private fun Conexion.huella(usuarioId: String): Boolean =
    consultar("SELECT 1 FROM huellas WHERE usuario_id = ?", usuarioId) { true }.isNotEmpty()

fun Application.modulo(db: Db, proxyConfiable: Boolean = false, intentosLogin: Int = 10) {
    // detras de render u otro proxy la ip real viene en X-Forwarded-For; sin proxy ese encabezado lo podria inventar cualquiera
    // P4: la ULTIMA ip de la lista la agrega nuestro proxy (tunel o render); las anteriores las puede inventar el cliente
    if (proxyConfiable) install(XForwardedHeaders) { useLastProxy() }
    // P1: cabeceras de seguridad en cada respuesta (Seguridad.kt)
    install(CabecerasSeguras)
    install(CallLogging)
    install(ContentNegotiation) { json(JsonApi) }
    install(StatusPages) {
        exception<AccesoDenegado> { call, e -> call.respond(HttpStatusCode.Forbidden, Error(e.message ?: "No tienes permiso para hacer esto.")) }
        exception<ArchivoMuyGrande> { call, e -> call.respond(HttpStatusCode.PayloadTooLarge, Error(e.message!!)) }
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, Error("Revisa los datos enviados.")) }
        exception<IllegalArgumentException> { call, e -> call.respond(HttpStatusCode.BadRequest, Error(e.message ?: "Revisa los datos enviados.")) }
        // P3: una fecha o un mes mal escritos son error del cliente (400), no del servidor (500)
        exception<DateTimeParseException> { call, _ -> call.respond(HttpStatusCode.BadRequest, Error("Revisa el formato de la fecha.")) }
        exception<IllegalStateException> { call, e -> call.respond(HttpStatusCode.Conflict, Error(e.message ?: "No se pudo completar.")) }
        exception<Throwable> { call, e ->
            call.application.environment.log.error("error no controlado", e)
            // sin detalles: el mensaje interno no le sirve a la app y puede revelar como esta hecho el servidor
            call.respond(HttpStatusCode.InternalServerError, Error("Ocurrió un error en el servidor."))
        }
    }
    install(RateLimit) {
        // P4: limite general para toda la API, por ip; corta inundaciones (DoS) sin estorbar a la app
        global {
            rateLimiter(limit = 1000, refillPeriod = 1.minutes)
            requestKey { it.request.origin.remoteHost }
        }
        register(RateLimitName("login")) {
            rateLimiter(limit = intentosLogin, refillPeriod = 1.minutes)
            requestKey { it.request.origin.remoteHost }
        }
    }
    install(Authentication) {
        bearer("sesion") {
            authenticate { credencial -> db.leer { Sesiones.usuario(Conexion(it), credencial.token) } }
        }
    }

    // RF-18: los expedientes con baja vencida se borran en el servidor, no al abrir la app
    launch {
        while (isActive) {
            runCatching { db.escribir { Conexion(it).purgarVencidos() } }.onFailure { environment.log.error("no se pudo purgar", it) }
            delay(6.hours)
        }
    }

    val bloqueos = BloqueoCuentas()

    routing {
        get("/") { call.respondText("Sonrisas de Cristal: servidor activo") }

        rateLimit(RateLimitName("login")) {
            post("/auth/login") {
                val p = call.receive<Login>()
                // P3: textos de tamano razonable antes de tocar la base o calcular PBKDF2
                if (p.usuario.length > 100 || p.contrasena.length > 200) throw IllegalArgumentException("Revisa los datos enviados.")
                val clave = p.usuario.trim().lowercase()
                // P4: tras 5 fallos la cuenta se bloquea 15 min, venga de la ip que venga
                if (bloqueos.bloqueada(clave)) {
                    call.respond(HttpStatusCode.TooManyRequests, Error("Demasiados intentos. Espera 15 minutos."))
                    return@post
                }
                val encontrado = db.leer { c ->
                    val con = Conexion(c)
                    with(con) { Tablas.usuarios.donde("usuario = ?", clave) }.firstOrNull()
                        ?.let { u -> u to con.consultar("SELECT hash FROM claves WHERE usuario_id = ?", u.id) { it.getString(1) }.firstOrNull() }
                }
                val hash = encontrado?.second
                // se verifica aunque la cuenta no exista, para que el tiempo de respuesta no delate que usuarios hay
                val correcta = Claves.verificar(p.contrasena, hash ?: Claves.falso) && hash != null
                if (encontrado == null || !correcta) {
                    bloqueos.fallo(clave)
                    call.respond(HttpStatusCode.Unauthorized, Error("Usuario o contraseña incorrectos."))
                    return@post
                }
                bloqueos.exito(clave)
                val u = encontrado.first
                if (!u.activo) {
                    call.respond(HttpStatusCode.Forbidden, Error("Tu acceso a la app fue retirado."))
                    return@post
                }
                val respuesta = db.escribir { c ->
                    val con = Conexion(c)
                    val token = Sesiones.crear(con, u.id)
                    con.registrar(u, LocalDateTime.now(), AccionRegistro.SESION, "Inició sesión")
                    RespuestaLogin(token, Perfil(u, con.huella(u.id)))
                }
                call.respond(respuesta)
            }
        }

        authenticate("sesion") {
            get("/yo") {
                val u = call.principal<Usuario>()!!
                call.respond(Perfil(u, db.leer { Conexion(it).huella(u.id) }))
            }

            put("/yo/huella") {
                val u = call.principal<Usuario>()!!
                val activo = call.receive<Activo>().activo
                db.escribir { c ->
                    val con = Conexion(c)
                    con.ejecutar("DELETE FROM huellas WHERE usuario_id = ?", u.id)
                    if (activo) con.ejecutar("INSERT INTO huellas (usuario_id) VALUES (?)", u.id)
                }
                call.respond(HttpStatusCode.NoContent)
            }

            post("/auth/logout") {
                val u = call.principal<Usuario>()!!
                val token = call.request.header(HttpHeaders.Authorization)!!.substringAfter(' ')
                db.escribir { c ->
                    val con = Conexion(c)
                    Sesiones.cerrar(con, token)
                    con.registrar(u, LocalDateTime.now(), AccionRegistro.SESION, "Cerró sesión")
                }
                call.respond(HttpStatusCode.NoContent)
            }

            rutasEstructura(db)
            rutasNinos(db)
            rutasContenido(db)
            rutasMensajes(db)
        }
    }
}
