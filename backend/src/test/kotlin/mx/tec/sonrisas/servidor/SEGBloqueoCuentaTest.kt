package mx.tec.sonrisas.servidor

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import mx.tec.sonrisas.data.JsonApi
import mx.tec.sonrisas.data.Login
import mx.tec.sonrisas.data.RespuestaLogin
import mx.tec.sonrisas.domain.Usuario
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/** P4: fallos de login desde 3 ips distintas bloquean la cuenta 3 dias, en la base, hasta que la fundacion la desbloquee */
class SEGBloqueoCuentaTest {

    private var ahora = System.currentTimeMillis()

    private fun conBase(bloque: (Db) -> Unit) {
        val url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
        val db = Db(url)
        try {
            runBlocking { Semilla.preparar(db, demo = true, reiniciar = false, claveFundacion = null) }
            bloque(db)
        } finally {
            db.cerrar()
            DriverManager.getConnection(url).use { it.createStatement().execute("SHUTDOWN") }
        }
    }

    // cada llamada es un servidor nuevo sobre la misma base: lo que vive en memoria se pierde, como al reiniciar
    private fun servidor(db: Db, bloque: suspend HttpClient.() -> Unit) = testApplication {
        application { modulo(db, proxyConfiable = true, intentosLogin = 100, reloj = { ahora }) }
        client.bloque()
    }

    private suspend fun HttpClient.intentar(usuario: String, contrasena: String, ip: String): HttpResponse = post("/auth/login") {
        contentType(ContentType.Application.Json)
        header("X-Forwarded-For", ip)
        setBody(JsonApi.encodeToString(Login.serializer(), Login(usuario, contrasena)))
    }

    private suspend fun HttpClient.token(usuario: String): String =
        JsonApi.decodeFromString(RespuestaLogin.serializer(), intentar(usuario, "demo", "192.0.2.1").bodyAsText()).token

    private suspend fun HttpClient.atacarDesdeTresIps(usuario: String) =
        (1..3).forEach { i -> assertEquals(HttpStatusCode.Unauthorized, intentar(usuario, "mal", "198.51.100.$i").status) }

    private suspend fun HttpClient.idDe(usuario: String, tokenFundacion: String): String =
        leer<List<Usuario>>("/bloqueos", tokenFundacion).first { it.usuario == usuario }.id

    @Test
    fun `fallos desde 3 ips distintas bloquean la cuenta aunque luego llegue la contrasena correcta`() = conBase { db ->
        servidor(db) {
            atacarDesdeTresIps("familia")
            val r = intentar("familia", "demo", "192.0.2.50")
            assertEquals(HttpStatusCode.TooManyRequests, r.status)
            // misma respuesta que un nombre inexistente bloqueado: no delata que la cuenta existe
            atacarDesdeTresIps("nadie.existe")
            assertEquals(r.bodyAsText(), intentar("nadie.existe", "x", "192.0.2.50").bodyAsText())
        }
    }

    @Test
    fun `solo la fundacion ve y quita bloqueos`() = conBase { db ->
        servidor(db) {
            atacarDesdeTresIps("educadora")
            val id = idDe("educadora", token("fundacion"))
            val direccion = token("direccion")
            assertEquals(HttpStatusCode.Forbidden, get("/bloqueos") { bearerAuth(direccion) }.status)
            assertEquals(HttpStatusCode.Forbidden, post("/cuentas/$id/desbloqueo") { bearerAuth(direccion) }.status)
            assertEquals(HttpStatusCode.TooManyRequests, intentar("educadora", "demo", "192.0.2.50").status)
        }
    }

    @Test
    fun `la fundacion desbloquea y la cuenta vuelve a entrar`() = conBase { db ->
        servidor(db) {
            atacarDesdeTresIps("educadora")
            assertEquals(HttpStatusCode.TooManyRequests, intentar("educadora", "demo", "192.0.2.50").status)
            val fundacion = token("fundacion")
            val id = idDe("educadora", fundacion)
            assertEquals(HttpStatusCode.NoContent, post("/cuentas/$id/desbloqueo") { bearerAuth(fundacion) }.status)
            assertEquals(HttpStatusCode.OK, intentar("educadora", "demo", "192.0.2.50").status)
            assertTrue(leer<List<Usuario>>("/bloqueos", fundacion).isEmpty())
        }
    }

    @Test
    fun `el bloqueo sobrevive un reinicio y vence a los 3 dias`() = conBase { db ->
        servidor(db) { atacarDesdeTresIps("familia") }
        servidor(db) {
            assertEquals(HttpStatusCode.TooManyRequests, intentar("familia", "demo", "192.0.2.50").status)
            ahora += 3.days.inWholeMilliseconds - 1
            assertEquals(HttpStatusCode.TooManyRequests, intentar("familia", "demo", "192.0.2.50").status)
            ahora += 2
            assertEquals(HttpStatusCode.OK, intentar("familia", "demo", "192.0.2.50").status)
        }
    }
}
