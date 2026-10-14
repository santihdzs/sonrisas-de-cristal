package mx.tec.sonrisas.servidor

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
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
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.ReporteAsistencia
import mx.tec.sonrisas.domain.Usuario
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// reglas de privacidad y seguridad contra el servidor real, con h2 en memoria y los datos de prueba
class ServidorTest {

    private fun prueba(intentosLogin: Int = 100, bloque: suspend HttpClient.() -> Unit) = testApplication {
        val db = Db("jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
        runBlocking { Semilla.preparar(db, demo = true, reiniciar = false, claveFundacion = null) }
        application { modulo(db, intentosLogin = intentosLogin) }
        client.bloque()
    }

    private suspend fun HttpClient.intentar(usuario: String, contrasena: String = "demo1234"): HttpResponse = post("/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(JsonApi.encodeToString(Login.serializer(), Login(usuario, contrasena)))
    }

    private suspend fun HttpClient.token(usuario: String): String =
        JsonApi.decodeFromString(RespuestaLogin.serializer(), intentar(usuario).bodyAsText()).token

    private suspend inline fun <reified T> HttpClient.leer(ruta: String, token: String): T =
        JsonApi.decodeFromString<T>(get(ruta) { bearerAuth(token) }.bodyAsText())

    @Test
    fun laFamiliaSoloVeASusHijos() = prueba {
        val ninos = leer<List<Nino>>("/ninos", token("familia"))
        assertEquals(listOf("Mateo", "Sofía"), ninos.map { it.nombre })
    }

    @Test
    fun laEducadoraNoVeNinosDeOtraSala() = prueba {
        val mateo = leer<List<Nino>>("/ninos", token("familia")).first { it.nombre == "Mateo" }
        val educadora = token("educadora")
        assertTrue(leer<List<Nino>>("/ninos", educadora).none { it.id == mateo.id })
        assertEquals(HttpStatusCode.NotFound, get("/ninos/${mateo.id}") { bearerAuth(educadora) }.status)
        val evento = """[{"type":"mx.tec.sonrisas.domain.EventoBitacora.Nota","id":"x","ninoId":"${mateo.id}","cuando":"2026-10-14T10:00","registradoPorId":"","texto":"hola"}]"""
        val captura = post("/eventos") { bearerAuth(educadora); contentType(ContentType.Application.Json); setBody(evento) }
        assertEquals(HttpStatusCode.Forbidden, captura.status)
    }

    @Test
    fun laFundacionNoRecibeNombresDeNinos() = prueba {
        val reporte = leer<ReporteAsistencia>("/reporte?mes=2026-10", token("fundacion"))
        assertTrue(reporte.filas.isEmpty())
        assertTrue(reporte.salas.isNotEmpty())
        assertTrue(leer<List<Nino>>("/ninos", token("fundacion")).isEmpty())
    }

    @Test
    fun retirarAccesoCierraLaSesionAbierta() = prueba {
        val lactantes = token("educadora.lactantes")
        val direccion = token("direccion")
        val cuenta = leer<List<Usuario>>("/cuentas", direccion).first { it.usuario == "educadora.lactantes" }
        assertEquals(HttpStatusCode.NoContent, post("/cuentas/${cuenta.id}/retiro") { bearerAuth(direccion) }.status)
        assertEquals(HttpStatusCode.Unauthorized, get("/ninos") { bearerAuth(lactantes) }.status)
        assertEquals(HttpStatusCode.Forbidden, intentar("educadora.lactantes").status)
    }

    @Test
    fun unArchivoDeMasDeDiezMegasSeRechaza() = prueba {
        val educadora = token("educadora")
        val sala = leer<List<Nino>>("/ninos", educadora).first().salaId
        val respuesta = post("/material?sala=$sala&titulo=grande&nombre=grande.pdf") {
            bearerAuth(educadora)
            contentType(ContentType.Application.Pdf)
            setBody(ByteArray(10 * 1024 * 1024 + 1))
        }
        assertEquals(HttpStatusCode.PayloadTooLarge, respuesta.status)
    }

    @Test
    fun unaFamiliaNoDescargaMaterialDeOtraSala() = prueba {
        // "rutina" es material de Lactantes: Paola solo tiene a Diego en Maternales, Laura tiene a Mateo en Lactantes
        assertEquals(HttpStatusCode.NotFound, get("/archivos/demo-rutina") { bearerAuth(token("familia.paola")) }.status)
        assertEquals(HttpStatusCode.OK, get("/archivos/demo-rutina") { bearerAuth(token("familia")) }.status)
    }

    @Test
    fun elLoginTieneLimiteDeIntentos() = prueba(intentosLogin = 3) {
        repeat(3) { assertEquals(HttpStatusCode.Unauthorized, intentar("familia", "mal").status) }
        assertEquals(HttpStatusCode.TooManyRequests, intentar("familia", "mal").status)
    }
}
