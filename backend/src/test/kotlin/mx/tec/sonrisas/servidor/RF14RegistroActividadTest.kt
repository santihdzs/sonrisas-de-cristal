package mx.tec.sonrisas.servidor

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.TemaFoto
import mx.tec.sonrisas.domain.UsoFoto
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-14: la direccion sabe quien registro y quien consulto cada dato */
class RF14RegistroActividadTest {

    private suspend fun io.ktor.client.HttpClient.registros() = leer<List<RegistroActividad>>("/registros", token("direccion"))

    @Test
    fun `RF14 cada captura queda registrada con la persona y el nino`() = pruebaApi {
        val t = token("educadora")
        val yo = sesion("educadora").perfil.usuario
        val n = leer<List<Nino>>("/ninos", t).first()
        enviarEventos(t, listOf(EventoBitacora.Nota("n1", n.id, LocalDateTime.of(2027, 3, 1, 10, 0), "", "Comió bien")))
        val r = registros().first { it.accion == AccionRegistro.CAPTURA && it.ninoId == n.id && it.personaId == yo.id }
        assertEquals(yo.nombre, r.personaNombre)
    }

    @Test
    fun `RF14 cuando una familia consulta una foto queda registrado quien la vio`() = pruebaApi {
        val t = token("educadora")
        val sofia = leer<List<Nino>>("/ninos", t).first { it.nombre == "Sofía" }
        subirFoto(t, Foto("f1", sofia.salaId, LocalDateTime.of(2027, 3, 1, 11, 0), "Pintura", listOf(sofia.id), UsoFoto.INTERNA, TemaFoto.PINTURA))
        post("/fotos/f1/consulta") { bearerAuth(token("familia")) }
        val familia = sesion("familia").perfil.usuario
        assertTrue(registros().any { it.accion == AccionRegistro.FOTO_CONSULTADA && it.personaId == familia.id && it.ninoId == sofia.id })
    }

    @Test
    fun `RF14 imprimir o compartir una bitacora queda registrado`() = pruebaApi {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first()
        post("/exportaciones") {
            bearerAuth(t)
            contentType(ContentType.Application.Json)
            setBody("""{"ninoIds":["${n.id}"],"detalle":"Imprimió la bitácora semanal"}""")
        }
        assertTrue(registros().any { it.accion == AccionRegistro.IMPRESION && it.ninoId == n.id && it.detalle == "Imprimió la bitácora semanal" })
    }

    @Test
    fun `RF14 solo la direccion puede leer el registro de actividad`() = pruebaApi {
        assertTrue(registros().isNotEmpty())
        assertEquals(emptyList(), leer<List<RegistroActividad>>("/registros", token("educadora")))
        assertEquals(emptyList(), leer<List<RegistroActividad>>("/registros", token("familia")))
    }
}
