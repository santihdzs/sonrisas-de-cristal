package mx.tec.sonrisas.servidor

import io.ktor.http.HttpStatusCode
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Rol
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** RF-01: educadora y familiar entran con sus credenciales y cada quien recibe su propio perfil y vista */
class RF01AccesoPerfilesTest {

    @Test
    fun `RF01 la educadora entra con sus credenciales y recibe perfil de educadora con su sala`() = pruebaApi {
        val r = login("educadora")
        assertEquals(HttpStatusCode.OK, r.status)
        val u = sesion("educadora").perfil.usuario
        assertEquals(Rol.EDUCADORA, u.rol)
        assertNotNull(u.salaId)
    }

    @Test
    fun `RF01 el familiar entra con sus credenciales y recibe perfil de familiar ligado a su tutor`() = pruebaApi {
        val u = sesion("familia").perfil.usuario
        assertEquals(Rol.FAMILIAR, u.rol)
        assertNotNull(u.tutorId)
    }

    @Test
    fun `RF01 cada perfil recibe datos distintos y el familiar no puede usar funciones de educadora`() = pruebaApi {
        val educadora = token("educadora")
        val familia = token("familia")
        val deEducadora = leer<List<Nino>>("/ninos", educadora).map { it.id }.toSet()
        val deFamilia = leer<List<Nino>>("/ninos", familia).map { it.id }.toSet()
        assertTrue(deEducadora.isNotEmpty() && deFamilia.isNotEmpty())
        assertNotEquals(deEducadora, deFamilia)
        val hijo = leer<List<Nino>>("/ninos", familia).first()
        val llegada = Asistencia("x", hijo.id, LocalDate.of(2027, 3, 1), LocalTime.of(8, 0), hijo.personasAutorizadas.first().id, "")
        assertEquals(HttpStatusCode.Forbidden, enviarAsistencia("/asistencias/llegada", familia, llegada).status)
    }

    @Test
    fun `RF01 con una contrasena incorrecta no se entra a ningun perfil`() = pruebaApi {
        assertEquals(HttpStatusCode.Unauthorized, login("educadora", "incorrecta").status)
        assertEquals(HttpStatusCode.Unauthorized, login("familia", "incorrecta").status)
    }
}
