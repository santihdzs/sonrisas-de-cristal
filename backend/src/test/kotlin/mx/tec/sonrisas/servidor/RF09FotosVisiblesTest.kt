package mx.tec.sonrisas.servidor

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.TemaFoto
import mx.tec.sonrisas.domain.UsoFoto
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-09: una foto etiquetada solo la ven las familias de los ninos que aparecen en ella */
class RF09FotosVisiblesTest {

    private fun foto(id: String, ninos: List<Nino>) =
        Foto(id, ninos.first().salaId, LocalDateTime.of(2027, 3, 1, 11, 0), "Clase de música", ninos.map { it.id }, UsoFoto.INTERNA, TemaFoto.MUSICA)

    // sofia (familia y familia.papa) y otros dos de maternales; diego (familia.paola) no aparece
    private suspend fun io.ktor.client.HttpClient.subirFotoDeTres(): Foto {
        val t = token("educadora")
        val sala = leer<List<Nino>>("/ninos", t)
        val tres = listOf(sala.first { it.nombre == "Sofía" }) +
            sala.filter { it.nombre !in setOf("Sofía", "Diego") && it.permisoImagenInterna }.take(2)
        return foto("f-tres", tres).also { assertEquals(HttpStatusCode.NoContent, subirFoto(t, it).status) }
    }

    @Test
    fun `RF09 las familias de los ninos etiquetados ven la foto`() = pruebaApi {
        val f = subirFotoDeTres()
        assertEquals(3, f.ninoIds.size)
        assertTrue(leer<List<Foto>>("/fotos", token("familia")).any { it.id == f.id })
        assertTrue(leer<List<Foto>>("/fotos", token("familia.papa")).any { it.id == f.id })
    }

    @Test
    fun `RF09 una familia de la misma sala que no aparece no ve la foto`() = pruebaApi {
        val f = subirFotoDeTres()
        assertTrue(leer<List<Foto>>("/fotos", token("familia.paola")).none { it.id == f.id })
    }

    @Test
    fun `RF09 pedir la foto por id o por nino ajeno tampoco la muestra`() = pruebaApi {
        val f = subirFotoDeTres()
        val otra = token("familia.paola")
        assertEquals(HttpStatusCode.NotFound, get("/fotos/${f.id}") { bearerAuth(otra) }.status)
        assertTrue(leer<List<Foto>>("/fotos?nino=${f.ninoIds.first()}", otra).none { it.id == f.id })
    }

    @Test
    fun `RF09 todas las fotos que recibe una familia incluyen a alguno de sus hijos`() = pruebaApi {
        subirFotoDeTres()
        for (familia in listOf("familia", "familia.paola")) {
            val t = token(familia)
            val hijos = leer<List<Nino>>("/ninos", t).map { it.id }.toSet()
            val fotos = leer<List<Foto>>("/fotos", t)
            assertTrue(fotos.all { f -> f.ninoIds.any { it in hijos } })
        }
    }
}
