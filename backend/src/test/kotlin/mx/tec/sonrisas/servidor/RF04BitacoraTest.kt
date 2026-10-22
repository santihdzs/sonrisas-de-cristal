package mx.tec.sonrisas.servidor

import io.ktor.client.request.post
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Porcion
import mx.tec.sonrisas.domain.TiempoComida
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-04: la educadora registra la bitacora del dia de cada nino (comida, siesta, notas) */
class RF04BitacoraTest {

    private val dia = LocalDate.of(2027, 3, 1)
    private fun a(h: Int, m: Int = 0) = LocalDateTime.of(dia, LocalTime.of(h, m))

    @Test
    fun `RF04 la bitacora del dia guarda comida, siesta y notas del nino`() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        val eventos = listOf(
            EventoBitacora.Alimento("e1", n.id, a(9), "", TiempoComida.DESAYUNO, Porcion.TODO),
            EventoBitacora.Siesta("e2", n.id, a(13), "", LocalTime.of(13, 0), LocalTime.of(14, 30)),
            EventoBitacora.Nota("e3", n.id, a(15), "", "Participó en la clase de música"),
        )
        assertEquals(HttpStatusCode.NoContent, enviarEventos(t, eventos).status)
        val delDia = leer<List<EventoBitacora>>("/eventos?fecha=$dia", t).filter { it.ninoId == n.id }
        assertEquals(setOf("e1", "e2", "e3"), delDia.map { it.id }.toSet())
        assertEquals(90, delDia.filterIsInstance<EventoBitacora.Siesta>().single().minutos)
    }

    @Test
    fun `RF04 una siesta abierta se cierra despues con su hora de fin`() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        enviarEventos(t, listOf(EventoBitacora.Siesta("s1", n.id, a(13), "", LocalTime.of(13, 0), null)))
        val r = post("/eventos/s1/fin") { bearerAuth(t); contentType(ContentType.Application.Json); setBody("""{"hora":"14:15"}""") }
        assertEquals(HttpStatusCode.NoContent, r.status)
        val siesta = leer<List<EventoBitacora>>("/eventos?fecha=$dia", t).single { it.id == "s1" } as EventoBitacora.Siesta
        assertEquals(LocalTime.of(14, 15), siesta.fin)
    }

    @Test
    fun `RF04 cada registro queda firmado por la educadora de la sesion`() = pruebaApi {
        val t = token("educadora")
        val yo = sesion("educadora").perfil.usuario.id
        val n = leer<List<Nino>>("/ninos", t).first()
        enviarEventos(t, listOf(EventoBitacora.Nota("n1", n.id, a(10), "otra-persona", "Comió bien")))
        assertEquals(yo, leer<List<EventoBitacora>>("/eventos?fecha=$dia", t).single { it.id == "n1" }.registradoPorId)
    }

    @Test
    fun `RF04 la educadora no puede escribir en la bitacora de un nino de otra sala`() = pruebaApi {
        val t = token("educadora")
        val propios = leer<List<Nino>>("/ninos", t).map { it.id }.toSet()
        val otro = leer<List<Nino>>("/ninos", token("direccion")).first { it.id !in propios }
        val r = enviarEventos(t, listOf(EventoBitacora.Nota("x", otro.id, a(10), "", "intento")))
        assertEquals(HttpStatusCode.Forbidden, r.status)
        assertTrue(leer<List<EventoBitacora>>("/eventos?fecha=$dia", token("direccion")).none { it.id == "x" })
    }
}
