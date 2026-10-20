package mx.tec.sonrisas.servidor

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import mx.tec.sonrisas.data.JsonApi
import mx.tec.sonrisas.data.Login
import mx.tec.sonrisas.data.RespuestaLogin
import mx.tec.sonrisas.data.ResumenConversacion
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Usuario
import java.net.URLEncoder
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pruebas de seguridad de la Parte 2 del entregable (ataques que la API debe rechazar).
 * Cada prueba tiene un id (S01, S02...) que coincide con la guia de estudio de seguridad.
 * Las que fallan antes de los parches son los huecos; despues de cada parche deben pasar.
 */
class SeguridadTest {

    private fun prueba(
        proxyConfiable: Boolean = false,
        intentosLogin: Int = 100,
        bloque: suspend HttpClient.(Db) -> Unit,
    ) = testApplication {
        val db = Db("jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
        runBlocking { Semilla.preparar(db, demo = true, reiniciar = false, claveFundacion = null) }
        application { modulo(db, proxyConfiable = proxyConfiable, intentosLogin = intentosLogin) }
        client.bloque(db)
    }

    private suspend fun HttpClient.intentar(usuario: String, contrasena: String = "demo1234", ip: String? = null): HttpResponse =
        post("/auth/login") {
            contentType(ContentType.Application.Json)
            if (ip != null) header("X-Forwarded-For", ip)
            setBody(JsonApi.encodeToString(Login.serializer(), Login(usuario, contrasena)))
        }

    private suspend fun HttpClient.token(usuario: String): String =
        JsonApi.decodeFromString(RespuestaLogin.serializer(), intentar(usuario).bodyAsText()).token

    private suspend inline fun <reified T> HttpClient.leer(ruta: String, token: String): T =
        JsonApi.decodeFromString<T>(get(ruta) { bearerAuth(token) }.bodyAsText())

    private suspend fun HttpClient.mateo(): Nino =
        leer<List<Nino>>("/ninos", token("familia")).first { it.nombre == "Mateo" }

    private fun url(texto: String): String = URLEncoder.encode(texto, Charsets.UTF_8).replace("+", "%20")

    // ---------- A. Sesiones y tokens ----------

    @Test
    fun `S01 sin token la API responde 401`() = prueba {
        assertEquals(HttpStatusCode.Unauthorized, get("/ninos").status)
    }

    @Test
    fun `S02 un token inventado responde 401`() = prueba {
        assertEquals(HttpStatusCode.Unauthorized, get("/ninos") { bearerAuth("token-inventado-123") }.status)
    }

    @Test
    fun `S03 un token real con un caracter alterado responde 401`() = prueba {
        val real = token("familia")
        val alterado = real.dropLast(1) + (if (real.last() == 'A') 'B' else 'A')
        assertEquals(HttpStatusCode.OK, get("/ninos") { bearerAuth(real) }.status)
        assertEquals(HttpStatusCode.Unauthorized, get("/ninos") { bearerAuth(alterado) }.status)
    }

    @Test
    fun `S04 un token vencido responde 401`() = prueba { db ->
        val t = token("familia")
        assertEquals(HttpStatusCode.OK, get("/ninos") { bearerAuth(t) }.status)
        // se adelanta el reloj: la sesion "vencio" en 1970
        db.escribir { Conexion(it).ejecutar("UPDATE sesiones SET expira = ?", 0L) }
        assertEquals(HttpStatusCode.Unauthorized, get("/ninos") { bearerAuth(t) }.status)
    }

    @Test
    fun `S05 un token ya no sirve despues de cerrar sesion`() = prueba {
        val t = token("familia")
        assertEquals(HttpStatusCode.NoContent, post("/auth/logout") { bearerAuth(t) }.status)
        assertEquals(HttpStatusCode.Unauthorized, get("/ninos") { bearerAuth(t) }.status)
    }

    @Test
    fun `S06 la base no guarda tokens ni contrasenas en texto plano`() = prueba { db ->
        val t = token("familia")
        val tokensGuardados = db.leer { Conexion(it).consultar("SELECT token_hash FROM sesiones") { r -> r.getString(1) } }
        assertTrue(t !in tokensGuardados)
        assertTrue(Sesiones.huella(t) in tokensGuardados)
        val claves = db.leer { Conexion(it).consultar("SELECT hash FROM claves") { r -> r.getString(1) } }
        assertTrue(claves.all { it.startsWith("pbkdf2$") && !it.contains("demo1234") })
    }

    @Test
    fun `S07 el login no delata si una cuenta existe`() = prueba {
        val inexistente = intentar("nadie.existe", "x")
        val incorrecta = intentar("familia", "x")
        assertEquals(HttpStatusCode.Unauthorized, inexistente.status)
        assertEquals(HttpStatusCode.Unauthorized, incorrecta.status)
        assertEquals(incorrecta.bodyAsText(), inexistente.bodyAsText())
    }

    // ---------- B. Inyeccion SQL ----------

    @Test
    fun `S08 inyeccion SQL clasica en el login es rechazada`() = prueba {
        assertEquals(HttpStatusCode.Unauthorized, intentar("' OR '1'='1' --", "' OR '1'='1' --").status)
        assertEquals(HttpStatusCode.Unauthorized, intentar("familia' --", "lo-que-sea").status)
    }

    @Test
    fun `S09 inyeccion SQL destructiva no borra tablas`() = prueba {
        assertEquals(HttpStatusCode.Unauthorized, intentar("x'; DROP TABLE ninos; --", "x").status)
        // la tabla sigue ahi y con datos
        assertEquals(2, leer<List<Nino>>("/ninos", token("familia")).size)
    }

    @Test
    fun `S10 inyeccion SQL en la URL y en parametros no devuelve datos`() = prueba {
        val t = token("familia.paola")
        assertEquals(HttpStatusCode.NotFound, get("/ninos/${url("' OR '1'='1")}") { bearerAuth(t) }.status)
        val r = get("/asistencias?nino=${url("' OR 1=1 --")}&desde=2026-01-01&hasta=2026-12-31") { bearerAuth(t) }
        assertEquals(HttpStatusCode.OK, r.status)
        assertEquals("[]", r.bodyAsText())
    }

    // ---------- C. IDOR / BOLA y privilegios ----------

    @Test
    fun `S11 IDOR una familia no puede ver al hijo de otra familia`() = prueba {
        val mateo = mateo()
        val paola = token("familia.paola")
        assertEquals(HttpStatusCode.NotFound, get("/ninos/${mateo.id}") { bearerAuth(paola) }.status)
        assertEquals("[]", get("/ninos/${mateo.id}/tutores") { bearerAuth(paola) }.bodyAsText())
        assertEquals("[]", get("/asistencias?nino=${mateo.id}&desde=2026-01-01&hasta=2026-12-31") { bearerAuth(paola) }.bodyAsText())
        assertEquals("[]", get("/incidentes?nino=${mateo.id}&desde=2026-01-01&hasta=2026-12-31") { bearerAuth(paola) }.bodyAsText())
    }

    @Test
    fun `S12 IDOR una direccion no puede tocar ninos de otra estancia`() = prueba {
        val mateo = mateo()
        val otraDireccion = token("direccion.vv")
        assertEquals(HttpStatusCode.NotFound, get("/ninos/${mateo.id}") { bearerAuth(otraDireccion) }.status)
        assertEquals(HttpStatusCode.Forbidden, post("/ninos/${mateo.id}/baja") { bearerAuth(otraDireccion) }.status)
        assertEquals(HttpStatusCode.Forbidden, delete("/ninos/${mateo.id}") { bearerAuth(otraDireccion) }.status)
    }

    @Test
    fun `S13 una familia no puede borrar expedientes ni crear cuentas`() = prueba {
        val mateo = mateo()
        val familia = token("familia")
        assertEquals(HttpStatusCode.Forbidden, delete("/ninos/${mateo.id}") { bearerAuth(familia) }.status)
        val cuenta = post("/cuentas/educadora") {
            bearerAuth(familia)
            contentType(ContentType.Application.Json)
            setBody("""{"nombre":"Intrusa","salaId":"${mateo.salaId}"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, cuenta.status)
    }

    @Test
    fun `S14 una educadora no puede editar los datos de una estancia`() = prueba {
        val educadora = token("educadora")
        val estancia = leer<List<Estancia>>("/estancias", educadora).first()
        val r = put("/estancias") {
            bearerAuth(educadora)
            contentType(ContentType.Application.Json)
            setBody(JsonApi.encodeToString(Estancia.serializer(), estancia.copy(nombre = "Estancia hackeada")))
        }
        assertEquals(HttpStatusCode.Forbidden, r.status)
    }

    // ---------- D. Limite de peticiones y bloqueo ----------

    @Test
    fun `S15 demasiados intentos de login desde la misma IP responden 429`() = prueba(intentosLogin = 3) {
        repeat(3) { assertEquals(HttpStatusCode.Unauthorized, intentar("familia", "mal").status) }
        assertEquals(HttpStatusCode.TooManyRequests, intentar("familia", "mal").status)
    }

    @Test
    fun `S16 el limite no se salta falsificando X-Forwarded-For`() = prueba(proxyConfiable = true, intentosLogin = 3) {
        // el atacante inventa la primera IP; el proxy de confianza agrega al final la IP real (203.0.113.7)
        val codigos = (1..4).map { i -> intentar("familia", "mal", ip = "10.0.0.$i, 203.0.113.7").status }
        assertEquals(HttpStatusCode.TooManyRequests, codigos.last())
    }

    @Test
    fun `S17 una cuenta se bloquea tras varios intentos fallidos aunque vengan de IPs distintas`() = prueba(proxyConfiable = true) {
        val fallidos = (1..5).map { i -> intentar("familia", "incorrecta", ip = "198.51.100.$i").status }
        assertTrue(fallidos.all { it == HttpStatusCode.Unauthorized })
        intentar("familia", "incorrecta", ip = "198.51.100.6")
        // aun con la contrasena correcta, la cuenta esta bloqueada un rato
        assertEquals(HttpStatusCode.TooManyRequests, intentar("familia", "demo1234", ip = "198.51.100.7").status)
    }

    @Test
    fun `S18 el bloqueo tambien aplica a usuarios que no existen`() = prueba(proxyConfiable = true) {
        // si solo se bloquearan cuentas reales, el 429 delataria cuales existen
        repeat(6) { i -> intentar("nadie.existe", "x", ip = "198.51.100.$i") }
        assertEquals(HttpStatusCode.TooManyRequests, intentar("nadie.existe", "x", ip = "198.51.100.99").status)
    }

    @Test
    fun `S19 la API corta a quien manda demasiadas peticiones`() = prueba {
        val codigos = (1..1001).map { get("/").status }
        assertTrue(HttpStatusCode.TooManyRequests in codigos)
    }

    // ---------- E. Validaciones adicionales en operaciones criticas ----------

    @Test
    fun `S20 no se puede borrar definitivamente un expediente activo`() = prueba {
        val mateo = mateo()
        val direccion = token("direccion")
        assertEquals(HttpStatusCode.Conflict, delete("/ninos/${mateo.id}") { bearerAuth(direccion) }.status)
        assertEquals(HttpStatusCode.OK, get("/ninos/${mateo.id}") { bearerAuth(direccion) }.status)
    }

    @Test
    fun `S21 borrar un expediente despues de darlo de baja sigue funcionando`() = prueba {
        val mateo = mateo()
        val direccion = token("direccion")
        assertEquals(HttpStatusCode.NoContent, post("/ninos/${mateo.id}/baja") { bearerAuth(direccion) }.status)
        assertEquals(HttpStatusCode.NoContent, delete("/ninos/${mateo.id}") { bearerAuth(direccion) }.status)
        assertEquals(HttpStatusCode.NotFound, get("/ninos/${mateo.id}") { bearerAuth(direccion) }.status)
    }

    // ---------- F. Validacion de entrada ----------

    @Test
    fun `S22 una fecha o un mes mal escritos responden 400 y no 500`() = prueba {
        val t = token("direccion")
        assertEquals(HttpStatusCode.BadRequest, get("/asistencias?fecha=2026-13-45") { bearerAuth(t) }.status)
        assertEquals(HttpStatusCode.BadRequest, get("/reporte?mes=no-es-un-mes") { bearerAuth(t) }.status)
    }

    @Test
    fun `S23 un mensaje demasiado largo se rechaza`() = prueba {
        val t = token("familia")
        val conversacion = leer<List<ResumenConversacion>>("/conversaciones", t).first().conversacion
        val r = post("/conversaciones/${conversacion.id}/mensajes") {
            bearerAuth(t)
            contentType(ContentType.Application.Json)
            setBody("""{"texto":"${"a".repeat(1001)}"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, r.status)
    }

    @Test
    fun `S24 no se acepta material con un tipo de archivo no permitido`() = prueba {
        val educadora = token("educadora")
        val sala = leer<List<Nino>>("/ninos", educadora).first().salaId
        val r = post("/material?sala=$sala&titulo=pagina&nombre=pagina.html") {
            bearerAuth(educadora)
            contentType(ContentType.Text.Html)
            setBody("<script>alert(1)</script>")
        }
        assertEquals(HttpStatusCode.BadRequest, r.status)
    }

    // ---------- G. Cabeceras HTTP de seguridad ----------

    @Test
    fun `S25 las respuestas traen cabeceras de seguridad`() = prueba {
        val r = get("/")
        assertEquals("nosniff", r.headers["X-Content-Type-Options"])
        assertEquals("DENY", r.headers["X-Frame-Options"])
        assertTrue(r.headers["Strict-Transport-Security"].orEmpty().startsWith("max-age="))
        assertTrue(r.headers["Content-Security-Policy"].orEmpty().contains("default-src 'none'"))
        val conDatos = get("/ninos") { bearerAuth(token("familia")) }
        assertTrue(conDatos.headers[HttpHeaders.CacheControl].orEmpty().contains("no-store"))
    }

    // ---------- H. Exposicion de datos ----------

    @Test
    fun `S26 una familia no ve los nombres de usuario del personal`() = prueba {
        val personal = leer<List<Usuario>>("/personal", token("familia"))
        assertTrue(personal.isNotEmpty())
        assertTrue(personal.all { it.usuario.isBlank() })
    }
}
