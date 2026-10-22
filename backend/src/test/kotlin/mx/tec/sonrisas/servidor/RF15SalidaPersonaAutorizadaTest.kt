package mx.tec.sonrisas.servidor

import io.ktor.http.HttpStatusCode
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Nino
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** RF-15: la salida solo acepta personas autorizadas del expediente, no nombres escritos a mano */
class RF15SalidaPersonaAutorizadaTest {

    private val dia = LocalDate.of(2027, 3, 1)

    private fun llegada(n: Nino) =
        Asistencia(UUID.randomUUID().toString(), n.id, dia, LocalTime.of(8, 0), n.personasAutorizadas.first().id, "")

    @Test
    fun `RF15 la salida con una persona autorizada del expediente se guarda`() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        val a = llegada(n).also { enviarAsistencia("/asistencias/llegada", t, it) }
        val autorizada = n.personasAutorizadas.last().id
        val r = enviarAsistencia("/asistencias/salida", t, a.copy(horaSalida = LocalTime.of(15, 0), recogidoPorId = autorizada))
        assertEquals(HttpStatusCode.NoContent, r.status)
        assertEquals(autorizada, leer<List<Asistencia>>("/asistencias?fecha=$dia", t).single().recogidoPorId)
    }

    @Test
    fun `RF15 un nombre escrito a mano se rechaza y la salida no se registra`() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        val a = llegada(n).also { enviarAsistencia("/asistencias/llegada", t, it) }
        val r = enviarAsistencia("/asistencias/salida", t, a.copy(horaSalida = LocalTime.of(15, 0), recogidoPorId = "Juan Pérez"))
        assertEquals(HttpStatusCode.BadRequest, r.status)
        assertNull(leer<List<Asistencia>>("/asistencias?fecha=$dia", t).single().horaSalida)
    }

    @Test
    fun `RF15 una persona autorizada de otro nino no puede recogerlo`() = pruebaApi {
        val t = token("educadora")
        val sala = leer<List<Nino>>("/ninos", t)
        val n = sala.first()
        val deOtro = sala.flatMap { it.personasAutorizadas }.first { p -> n.persona(p.id) == null }
        val a = llegada(n).also { enviarAsistencia("/asistencias/llegada", t, it) }
        val r = enviarAsistencia("/asistencias/salida", t, a.copy(horaSalida = LocalTime.of(15, 0), recogidoPorId = deOtro.id))
        assertEquals(HttpStatusCode.BadRequest, r.status)
    }

    @Test
    fun `RF15 la llegada tambien exige que lo entregue una persona autorizada`() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        val r = enviarAsistencia("/asistencias/llegada", t, llegada(n).copy(entregadoPorId = "Alguien sin registro"))
        assertEquals(HttpStatusCode.BadRequest, r.status)
    }
}
