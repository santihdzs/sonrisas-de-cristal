package mx.tec.sonrisas.servidor

import mx.tec.sonrisas.domain.Asistencia
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

/** RF-06: la familia ve como estuvo su hijo: asistencia y bitacora que registro la educadora */
class RF06FamiliaVeElDiaTest {

    private val dia = LocalDate.of(2027, 3, 1)
    private val rango = "desde=$dia&hasta=$dia"

    // la educadora de maternales registra el dia de sofia (hija de "familia" y "familia.papa")
    private suspend fun io.ktor.client.HttpClient.registrarDiaDeSofia(): Nino {
        val t = token("educadora")
        val sofia = leer<List<Nino>>("/ninos", t).first { it.nombre == "Sofía" }
        val llegada = Asistencia("a1", sofia.id, dia, LocalTime.of(8, 0), sofia.personasAutorizadas.first().id, "")
        enviarAsistencia("/asistencias/llegada", t, llegada)
        enviarAsistencia("/asistencias/salida", t, llegada.copy(horaSalida = LocalTime.of(15, 0), recogidoPorId = sofia.personasAutorizadas.first().id))
        enviarEventos(t, listOf(
            EventoBitacora.Alimento("e1", sofia.id, LocalDateTime.of(dia, LocalTime.of(12, 0)), "", TiempoComida.COMIDA, Porcion.PIDIO_MAS),
            EventoBitacora.Nota("e2", sofia.id, LocalDateTime.of(dia, LocalTime.of(14, 0)), "", "Hoy pintó con los dedos"),
        ))
        return sofia
    }

    @Test
    fun `RF06 la familia ve la bitacora del dia que registro la educadora`() = pruebaApi {
        val sofia = registrarDiaDeSofia()
        val eventos = leer<List<EventoBitacora>>("/eventos?nino=${sofia.id}&$rango", token("familia"))
        assertEquals(setOf("e1", "e2"), eventos.map { it.id }.toSet())
        assertEquals("Hoy pintó con los dedos", eventos.filterIsInstance<EventoBitacora.Nota>().single().texto)
    }

    @Test
    fun `RF06 la familia ve a que hora llego y salio su hijo y quien lo recogio`() = pruebaApi {
        val sofia = registrarDiaDeSofia()
        val a = leer<List<Asistencia>>("/asistencias?nino=${sofia.id}&$rango", token("familia")).single()
        assertEquals(LocalTime.of(8, 0), a.horaEntrada)
        assertEquals(LocalTime.of(15, 0), a.horaSalida)
        assertEquals(sofia.personasAutorizadas.first().id, a.recogidoPorId)
    }

    @Test
    fun `RF06 ambos tutores del nino ven el mismo dia`() = pruebaApi {
        val sofia = registrarDiaDeSofia()
        val mama = leer<List<EventoBitacora>>("/eventos?nino=${sofia.id}&$rango", token("familia")).map { it.id }.toSet()
        val papa = leer<List<EventoBitacora>>("/eventos?nino=${sofia.id}&$rango", token("familia.papa")).map { it.id }.toSet()
        assertEquals(mama, papa)
        assertTrue(mama.isNotEmpty())
    }

    @Test
    fun `RF06 otra familia de la misma sala no ve el dia de ese nino`() = pruebaApi {
        val sofia = registrarDiaDeSofia()
        val otra = token("familia.paola")
        assertEquals(emptyList(), leer<List<EventoBitacora>>("/eventos?nino=${sofia.id}&$rango", otra))
        assertEquals(emptyList(), leer<List<Asistencia>>("/asistencias?nino=${sofia.id}&$rango", otra))
    }
}
