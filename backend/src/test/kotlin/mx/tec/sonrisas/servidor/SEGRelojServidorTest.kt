package mx.tec.sonrisas.servidor

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import mx.tec.sonrisas.data.JsonApi
import mx.tec.sonrisas.data.MensajeChat
import mx.tec.sonrisas.data.NuevoAviso
import mx.tec.sonrisas.data.NuevoIncidente
import mx.tec.sonrisas.data.ResumenConversacion
import mx.tec.sonrisas.data.Texto
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.ObjetoPerdido
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.UsoFoto
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** un telefono con la fecha alterada (2001 o 2099) no puede fechar registros: el dia y la hora tope son del servidor */
class SEGRelojServidorTest {

    private val ahora = AHORA_PRUEBAS
    private val hoy = ahora.toLocalDate()
    private val pasado = LocalDateTime.of(2001, 1, 1, 9, 0)
    private val futuro = LocalDateTime.of(2099, 12, 31, 23, 30)

    private fun cerca(esperado: LocalDateTime, real: LocalDateTime) =
        assertTrue(Duration.between(esperado, real).abs() <= Duration.ofMinutes(1), "se guardó $real, el servidor dice $esperado")

    @Test
    fun laAsistenciaUsaElDiaDelServidor() = pruebaApi {
        val t = token("educadora")
        val (n1, n2) = leer<List<Nino>>("/ninos", t)
        val viejo = Asistencia("a-2001", n1.id, pasado.toLocalDate(), pasado.toLocalTime(), n1.personasAutorizadas.first().id, "")
        val nuevo = Asistencia("a-2099", n2.id, futuro.toLocalDate(), futuro.toLocalTime(), n2.personasAutorizadas.first().id, "")
        assertEquals(HttpStatusCode.NoContent, enviarAsistencia("/asistencias/llegada", t, viejo).status)
        assertEquals(HttpStatusCode.NoContent, enviarAsistencia("/asistencias/llegada", t, nuevo).status)
        // la salida tampoco cambia el dia ni la llegada
        val salida = nuevo.copy(fecha = pasado.toLocalDate(), horaEntrada = LocalTime.of(6, 0), horaSalida = futuro.toLocalTime(), recogidoPorId = n2.personasAutorizadas.first().id)
        assertEquals(HttpStatusCode.NoContent, enviarAsistencia("/asistencias/salida", t, salida).status)
        val delDia = leer<List<Asistencia>>("/asistencias?fecha=$hoy", t).associateBy { it.id }
        assertEquals(pasado.toLocalTime(), delDia.getValue("a-2001").horaEntrada)
        val a = delDia.getValue("a-2099")
        assertEquals(ahora.toLocalTime(), a.horaEntrada)
        assertEquals(ahora.toLocalTime(), a.horaSalida)
        assertTrue(leer<List<Asistencia>>("/asistencias?fecha=${pasado.toLocalDate()}", t).isEmpty())
        assertTrue(leer<List<Asistencia>>("/asistencias?fecha=${futuro.toLocalDate()}", t).isEmpty())
    }

    @Test
    fun laBitacoraUsaElDiaDelServidor() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        val eventos = listOf(
            EventoBitacora.Nota("e-2001", n.id, pasado, "", "nota vieja"),
            EventoBitacora.Nota("e-2099", n.id, futuro, "", "nota futura"),
            EventoBitacora.Siesta("s-2099", n.id, futuro, "", futuro.toLocalTime(), null),
        )
        assertEquals(HttpStatusCode.NoContent, enviarEventos(t, eventos).status)
        post("/eventos/s-2099/fin") { bearerAuth(t); contentType(ContentType.Application.Json); setBody("""{"hora":"23:59"}""") }
        val delDia = leer<List<EventoBitacora>>("/eventos?fecha=$hoy", t).associateBy { it.id }
        assertEquals(LocalDateTime.of(hoy, pasado.toLocalTime()), delDia.getValue("e-2001").cuando)
        cerca(ahora, delDia.getValue("e-2099").cuando)
        val siesta = delDia.getValue("s-2099") as EventoBitacora.Siesta
        cerca(ahora, siesta.cuando)
        assertEquals(ahora.toLocalTime(), siesta.inicio)
        assertEquals(ahora.toLocalTime(), siesta.fin)
    }

    @Test
    fun elIncidenteUsaElDiaDelServidor() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        for (cuando in listOf(pasado, futuro)) {
            val r = post("/incidentes") {
                bearerAuth(t); contentType(ContentType.Application.Json)
                setBody(JsonApi.encodeToString(NuevoIncidente.serializer(), NuevoIncidente(n.id, cuando, TipoIncidente.CAIDA, null, "Se cayó", "Se revisó")))
            }
            val guardado = JsonApi.decodeFromString(Incidente.serializer(), r.bodyAsText())
            assertEquals(hoy, guardado.cuando.toLocalDate())
            assertTrue(!guardado.cuando.isAfter(ahora))
        }
    }

    @Test
    fun elMensajeUsaLaHoraDelServidor() = pruebaApi {
        val t = token("educadora")
        val conv = leer<List<ResumenConversacion>>("/conversaciones", t).first().conversacion.id
        val r = post("/conversaciones/$conv/mensajes") {
            bearerAuth(t); contentType(ContentType.Application.Json)
            setBody(JsonApi.encodeToString(Texto.serializer(), Texto("hola")))
        }
        assertTrue(r.status.value in 200..299)
        cerca(ahora, leer<List<MensajeChat>>("/conversaciones/$conv/mensajes", t).last().mensaje.enviado)
    }

    @Test
    fun elAvisoUsaLaHoraDelServidor() = pruebaApi {
        val r = post("/avisos") {
            bearerAuth(token("direccion")); contentType(ContentType.Application.Json)
            setBody(JsonApi.encodeToString(NuevoAviso.serializer(), NuevoAviso(TipoAviso.GENERAL, "Junta", "Mañana hay junta", null)))
        }
        cerca(ahora, JsonApi.decodeFromString(Aviso.serializer(), r.bodyAsText()).publicadoEl)
    }

    @Test
    fun laFotoUsaLaHoraDelServidor() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        for ((id, cuando) in listOf("f-2001" to pasado, "f-2099" to futuro)) {
            subirFoto(t, Foto(id, n.salaId, cuando, "Pintura", listOf(n.id), UsoFoto.INTERNA, null))
            cerca(ahora, leer<List<Foto>>("/fotos", t).single { it.id == id }.cuando)
        }
    }

    @Test
    fun elObjetoPerdidoUsaLaHoraDelServidor() = pruebaApi {
        val t = token("educadora")
        val sala = leer<List<Nino>>("/ninos", t).first().salaId
        val r = post("/objetos?sala=$sala&descripcion=gorra&donde=patio&fecha=${pasado.toLocalDate()}") { bearerAuth(t) }
        cerca(ahora, JsonApi.decodeFromString(ObjetoPerdido.serializer(), r.bodyAsText()).publicadoEl)
        // la fecha en que se encontro la elige la educadora, pero nunca en el futuro
        val futura = post("/objetos?sala=$sala&descripcion=gorra&donde=patio&fecha=${LocalDate.of(2099, 1, 1)}") { bearerAuth(t) }
        assertEquals(HttpStatusCode.BadRequest, futura.status)
    }
}
