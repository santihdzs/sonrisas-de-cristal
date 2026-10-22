package mx.tec.sonrisas.servidor

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.Visibilidad
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-07: un familiar no accede a datos de un nino que no es suyo, ni manipulando la app (llamadas directas a la API) */
class RF07FamiliaSoloSusHijosTest {

    private suspend fun io.ktor.client.HttpClient.ajenos(familia: String): List<Nino> {
        val propios = leer<List<Nino>>("/ninos", token(familia)).map { it.id }.toSet()
        return leer<List<Nino>>("/ninos", token("direccion")).filter { it.id !in propios }
    }

    @Test
    fun `RF07 la lista del familiar solo trae ninos donde el es tutor`() = pruebaApi {
        val tutor = sesion("familia.paola").perfil.usuario.tutorId
        val ninos = leer<List<Nino>>("/ninos", token("familia.paola"))
        assertTrue(ninos.isNotEmpty())
        assertTrue(ninos.all { tutor in it.tutorIds })
    }

    @Test
    fun `RF07 pedir por id el expediente o los tutores de un nino ajeno no devuelve nada`() = pruebaApi {
        val t = token("familia.paola")
        ajenos("familia.paola").forEach { otro ->
            assertEquals(HttpStatusCode.NotFound, get("/ninos/${otro.id}") { bearerAuth(t) }.status)
            assertEquals("[]", get("/ninos/${otro.id}/tutores") { bearerAuth(t) }.bodyAsText())
        }
    }

    @Test
    fun `RF07 el historial de un nino ajeno sale vacio y no se puede escribir en el`() = pruebaApi {
        val t = token("familia.paola")
        val otro = ajenos("familia.paola").first()
        assertEquals("[]", get("/asistencias?nino=${otro.id}&desde=2020-01-01&hasta=2030-12-31") { bearerAuth(t) }.bodyAsText())
        assertEquals("[]", get("/incidentes?nino=${otro.id}&desde=2020-01-01&hasta=2030-12-31") { bearerAuth(t) }.bodyAsText())
        val falsa = Asistencia("x", otro.id, LocalDate.of(2027, 3, 1), LocalTime.of(8, 0), otro.personasAutorizadas.first().id, "")
        assertEquals(HttpStatusCode.Forbidden, enviarAsistencia("/asistencias/llegada", t, falsa).status)
    }

    @Test
    fun `RF07 la regla de visibilidad del cliente solo deja ver hijos del tutor`() {
        fun nino(id: String, tutores: List<String>) =
            Nino(id, id, "Prueba", LocalDate.of(2024, 1, 1), "e1", "s1", tutores, emptyList())
        val todos = listOf(nino("propio", listOf("t1")), nino("ajeno", listOf("t2")), nino("compartido", listOf("t2", "t1")))
        val familiar = Usuario("u", "familia", "Familia", Rol.FAMILIAR, estanciaId = "e1", tutorId = "t1")
        assertEquals(listOf("propio", "compartido"), Visibilidad.ninos(familiar, todos).map { it.id })
        assertEquals(emptyList(), Visibilidad.ninos(familiar.copy(tutorId = null), todos))
    }
}
