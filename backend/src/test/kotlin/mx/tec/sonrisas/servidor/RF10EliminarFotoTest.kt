package mx.tec.sonrisas.servidor

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.http.HttpStatusCode
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.TemaFoto
import mx.tec.sonrisas.domain.UsoFoto
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-10: la familia de un nino que aparece en una foto puede pedir que se elimine */
class RF10EliminarFotoTest {

    private suspend fun io.ktor.client.HttpClient.subirFotoCon(nombre: String): Foto {
        val t = token("educadora")
        val n = leer<List<Nino>>("/ninos", t).first { it.nombre == nombre }
        val f = Foto("f-$nombre", n.salaId, LocalDateTime.of(2027, 3, 1, 11, 0), "Jardín", listOf(n.id), UsoFoto.INTERNA, TemaFoto.JARDIN)
        subirFoto(t, f)
        return f
    }

    @Test
    fun `RF10 la familia elimina una foto donde aparece su hijo`() = pruebaApi {
        val f = subirFotoCon("Sofía")
        val t = token("familia")
        assertEquals(HttpStatusCode.NoContent, delete("/fotos/${f.id}") { bearerAuth(t) }.status)
        assertTrue(leer<List<Foto>>("/fotos", t).none { it.id == f.id })
    }

    @Test
    fun `RF10 la foto eliminada desaparece para todos, no solo para quien la pidio`() = pruebaApi {
        val f = subirFotoCon("Sofía")
        delete("/fotos/${f.id}") { bearerAuth(token("familia")) }
        assertTrue(leer<List<Foto>>("/fotos", token("familia.papa")).none { it.id == f.id })
        assertTrue(leer<List<Foto>>("/fotos", token("educadora")).none { it.id == f.id })
        assertTrue(leer<List<Foto>>("/fotos", token("direccion")).none { it.id == f.id })
    }

    @Test
    fun `RF10 una familia no puede eliminar una foto donde no aparece su hijo`() = pruebaApi {
        val f = subirFotoCon("Diego")
        assertEquals(HttpStatusCode.Forbidden, delete("/fotos/${f.id}") { bearerAuth(token("familia")) }.status)
        assertTrue(leer<List<Foto>>("/fotos", token("familia.paola")).any { it.id == f.id })
    }

    @Test
    fun `RF10 la eliminacion queda en el registro de actividad de la direccion`() = pruebaApi {
        val f = subirFotoCon("Sofía")
        delete("/fotos/${f.id}") { bearerAuth(token("familia")) }
        val familia = sesion("familia").perfil.usuario
        val registros = leer<List<RegistroActividad>>("/registros", token("direccion"))
        assertTrue(registros.any { it.accion == AccionRegistro.BORRADO && it.personaId == familia.id && "Jardín" in it.detalle })
    }
}
