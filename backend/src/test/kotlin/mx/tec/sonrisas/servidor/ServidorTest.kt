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
import io.ktor.http.isSuccess
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import mx.tec.sonrisas.data.Id
import mx.tec.sonrisas.data.JsonApi
import mx.tec.sonrisas.data.Login
import mx.tec.sonrisas.data.RespuestaLogin
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.ReporteAsistencia
import mx.tec.sonrisas.domain.UsoFoto
import mx.tec.sonrisas.domain.Usuario
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// reglas de privacidad y seguridad contra el servidor real, con h2 en memoria y los datos de prueba
class ServidorTest {

    private fun prueba(intentosLogin: Int = 100, bloque: suspend HttpClient.() -> Unit) = conBaseDePrueba { db ->
        testApplication {
            runBlocking { Semilla.preparar(db, demo = true, reiniciar = false, claveFundacion = null) }
            application { modulo(db, intentosLogin = intentosLogin) }
            client.bloque()
        }
    }

    private suspend fun HttpClient.intentar(usuario: String, contrasena: String = "demo"): HttpResponse = post("/auth/login") {
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
    fun laImagenDeUnaFotoSoloLaDescargaQuienVeLaFoto() = prueba {
        val educadora = token("educadora")
        val sofia = leer<List<Nino>>("/ninos", educadora).first { it.nombre == "Sofía" }
        val subida = post("/fotos/imagen?sala=${sofia.salaId}") { bearerAuth(educadora); contentType(ContentType.Image.JPEG); setBody(imagenDePrueba()) }
        val archivo = JsonApi.decodeFromString(Id.serializer(), subida.bodyAsText()).id
        val foto = Foto("f-camara", sofia.salaId, LocalDateTime.of(2027, 3, 1, 11, 0), "Pintura", listOf(sofia.id), UsoFoto.INTERNA, null, archivo)
        val guardada = post("/fotos") { bearerAuth(educadora); contentType(ContentType.Application.Json); setBody(JsonApi.encodeToString(Foto.serializer(), foto)) }
        assertTrue(guardada.status.isSuccess())
        // Laura es mamá de Sofía; Paola tiene a Diego en la misma sala, pero Diego no sale en la foto
        assertEquals(HttpStatusCode.OK, get("/archivos/$archivo") { bearerAuth(token("familia")) }.status)
        assertEquals(HttpStatusCode.NotFound, get("/archivos/$archivo") { bearerAuth(token("familia.paola")) }.status)
    }

    @Test
    fun elLoginTieneLimiteDeIntentos() = prueba(intentosLogin = 3) {
        repeat(3) { assertEquals(HttpStatusCode.Unauthorized, intentar("familia", "mal").status) }
        assertEquals(HttpStatusCode.TooManyRequests, intentar("familia", "mal").status)
    }
}
