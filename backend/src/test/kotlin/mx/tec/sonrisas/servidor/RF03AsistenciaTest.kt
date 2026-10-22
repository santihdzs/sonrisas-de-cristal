package mx.tec.sonrisas.servidor

import io.ktor.http.HttpStatusCode
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Nino
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** RF-03: hora de llegada, hora de salida y quien recoge, por nino; al final del dia la sala tiene su registro completo */
class RF03AsistenciaTest {

    private val dia = LocalDate.of(2027, 3, 1)

    private fun llegada(n: Nino) =
        Asistencia(UUID.randomUUID().toString(), n.id, dia, LocalTime.of(8, 0), n.personasAutorizadas.first().id, "")

    @Test
    fun `RF03 un dia completo deja a toda la sala con hora de llegada, salida y quien recogio`() = pruebaApi {
        val t = token("educadora")
        val sala = leer<List<Nino>>("/ninos", t)
        sala.forEach { n ->
            val a = llegada(n)
            assertEquals(HttpStatusCode.NoContent, enviarAsistencia("/asistencias/llegada", t, a).status)
            val salida = a.copy(horaSalida = LocalTime.of(15, 30), recogidoPorId = n.personasAutorizadas.last().id)
            assertEquals(HttpStatusCode.NoContent, enviarAsistencia("/asistencias/salida", t, salida).status)
        }
        val registro = leer<List<Asistencia>>("/asistencias?fecha=$dia", t)
        assertEquals(sala.map { it.id }.toSet(), registro.map { it.ninoId }.toSet())
        assertEquals(sala.size, registro.size)
        registro.forEach {
            assertEquals(LocalTime.of(8, 0), it.horaEntrada)
            assertEquals(LocalTime.of(15, 30), it.horaSalida)
            assertNotNull(it.recogidoPorId)
        }
    }

    @Test
    fun `RF03 la salida completa el registro de llegada y firma quien recibio y quien entrego`() = pruebaApi {
        val t = token("educadora")
        val yo = sesion("educadora").perfil.usuario.id
        val n = leer<List<Nino>>("/ninos", t).first()
        enviarAsistencia("/asistencias/llegada", t, llegada(n))
        // como la app: la salida parte del registro guardado; las firmas las pone el servidor aunque se manden otras
        val a = leer<List<Asistencia>>("/asistencias?fecha=$dia", t).single()
        val salida = a.copy(recibidoPorId = "otra-persona", entregoSalidaId = "otra-persona",
            horaSalida = LocalTime.of(15, 0), recogidoPorId = n.personasAutorizadas.first().id)
        enviarAsistencia("/asistencias/salida", t, salida)
        val guardada = leer<List<Asistencia>>("/asistencias?fecha=$dia", t).single()
        assertEquals(LocalTime.of(8, 0), guardada.horaEntrada)
        assertEquals(LocalTime.of(15, 0), guardada.horaSalida)
        assertEquals(yo, guardada.recibidoPorId)
        assertEquals(yo, guardada.entregoSalidaId)
    }

    @Test
    fun `RF03 registrar la llegada dos veces no duplica el registro del dia`() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        enviarAsistencia("/asistencias/llegada", t, llegada(n))
        enviarAsistencia("/asistencias/llegada", t, llegada(n).copy(horaEntrada = LocalTime.of(8, 15)))
        val registro = leer<List<Asistencia>>("/asistencias?fecha=$dia", t)
        assertTrue(registro.single().ninoId == n.id)
        assertEquals(LocalTime.of(8, 15), registro.single().horaEntrada)
    }

    @Test
    fun `RF03 no se puede registrar una salida sin llegada previa`() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        val salida = llegada(n).copy(horaSalida = LocalTime.of(15, 0), recogidoPorId = n.personasAutorizadas.first().id)
        assertEquals(HttpStatusCode.Forbidden, enviarAsistencia("/asistencias/salida", t, salida).status)
    }
}
