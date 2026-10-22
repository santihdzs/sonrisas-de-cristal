package mx.tec.sonrisas.servidor

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.Visibilidad
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-02: la educadora ve solo a los ninos de su sala, no a los de todo el centro */
class RF02ListaNinosSalaTest {

    @Test
    fun `RF02 todos los ninos que ve la educadora son de su sala`() = pruebaApi {
        val sala = sesion("educadora").perfil.usuario.salaId
        val ninos = leer<List<Nino>>("/ninos", token("educadora"))
        assertTrue(ninos.isNotEmpty())
        assertTrue(ninos.all { it.salaId == sala && it.nombre.isNotBlank() })
    }

    @Test
    fun `RF02 la educadora ve su sala completa y a nadie de las otras salas de la estancia`() = pruebaApi {
        val sala = sesion("educadora").perfil.usuario.salaId
        val estancia = leer<List<Nino>>("/ninos", token("direccion")).filter { it.activo }
        val ve = leer<List<Nino>>("/ninos", token("educadora")).map { it.id }.toSet()
        assertTrue(estancia.any { it.salaId != sala })
        assertEquals(estancia.filter { it.salaId == sala }.map { it.id }.toSet(), ve)
    }

    @Test
    fun `RF02 la educadora no puede abrir el expediente de un nino de otra sala`() = pruebaApi {
        val sala = sesion("educadora").perfil.usuario.salaId
        val otro = leer<List<Nino>>("/ninos", token("direccion")).first { it.salaId != sala }
        assertEquals(HttpStatusCode.NotFound, get("/ninos/${otro.id}") { bearerAuth(token("educadora")) }.status)
    }

    @Test
    fun `RF02 la regla de visibilidad filtra por sala y estancia y oculta bajas`() {
        fun nino(id: String, estancia: String, sala: String) =
            Nino(id, id, "Prueba", LocalDate.of(2024, 1, 1), estancia, sala, emptyList(), emptyList())
        val todos = listOf(nino("a", "e1", "s1"), nino("b", "e1", "s2"), nino("c", "e2", "s1"),
            nino("d", "e1", "s1").copy(estado = mx.tec.sonrisas.domain.EstadoExpediente.BajaTemporal(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1))))
        val educadora = Usuario("u", "educadora", "Educadora", Rol.EDUCADORA, estanciaId = "e1", salaId = "s1")
        assertEquals(listOf("a"), Visibilidad.ninos(educadora, todos).map { it.id })
    }
}
