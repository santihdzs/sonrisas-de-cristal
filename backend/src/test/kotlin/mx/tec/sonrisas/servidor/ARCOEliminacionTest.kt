package mx.tec.sonrisas.servidor

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import mx.tec.sonrisas.data.Id
import mx.tec.sonrisas.data.JsonApi
import mx.tec.sonrisas.data.NuevoIncidente
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.UsoFoto
import java.sql.DriverManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** ARCO, cancelacion: borrar un expediente borra de verdad sus datos, imagenes, registros y la cuenta de su familia */
class ARCOEliminacionTest {

    private suspend fun HttpClient.subirImagen(t: String, sala: String): String {
        val r = post("/fotos/imagen?sala=$sala") { bearerAuth(t); contentType(ContentType.Image.JPEG); setBody(imagenDePrueba()) }
        return JsonApi.decodeFromString(Id.serializer(), r.bodyAsText()).id
    }

    @Test
    fun `ARCO borrar a un nino borra sus fotos, imagenes, registros y la cuenta de su familia`() = pruebaApi {
        val educadora = token("educadora")
        val ninos = leer<List<Nino>>("/ninos", educadora)
        // Diego solo tiene a Paola como tutora; Sofia esta en la misma sala con otra familia
        val diego = ninos.first { it.nombre == "Diego" }
        val sofia = ninos.first { it.nombre == "Sofía" }
        val cuando = LocalDateTime.of(2027, 3, 1, 11, 0)

        val imagenSola = subirImagen(educadora, diego.salaId)
        val sola = Foto("f-sola", diego.salaId, cuando, "Diego pintando", listOf(diego.id), UsoFoto.INTERNA, null, imagenSola)
        assertTrue(subirFoto(educadora, sola).status.value in 200..299)
        val imagenGrupo = subirImagen(educadora, diego.salaId)
        val grupo = Foto("f-grupo", diego.salaId, cuando, "Jardín", listOf(diego.id, sofia.id), UsoFoto.INTERNA, null, imagenGrupo)
        assertTrue(subirFoto(educadora, grupo).status.value in 200..299)

        val llegada = Asistencia(UUID.randomUUID().toString(), diego.id, LocalDate.of(2027, 3, 1), LocalTime.of(8, 0), diego.personasAutorizadas.first().id, "")
        assertEquals(HttpStatusCode.NoContent, enviarAsistencia("/asistencias/llegada", educadora, llegada).status)
        val incidente = post("/incidentes") {
            bearerAuth(educadora)
            contentType(ContentType.Application.Json)
            setBody(JsonApi.encodeToString(NuevoIncidente.serializer(), NuevoIncidente(diego.id, cuando, TipoIncidente.FIEBRE, null, "Fiebre de 38", "Se llamó a la mamá")))
        }
        assertTrue(incidente.status.value in 200..299)
        assertEquals(HttpStatusCode.OK, login("familia.paola").status)

        val direccion = token("direccion")
        assertTrue(leer<List<RegistroActividad>>("/registros", direccion).any { it.ninoId == diego.id })
        assertEquals(HttpStatusCode.NoContent, post("/ninos/${diego.id}/baja") { bearerAuth(direccion) }.status)
        assertEquals(HttpStatusCode.NoContent, delete("/ninos/${diego.id}") { bearerAuth(direccion) }.status)

        // la foto donde solo salia el y su imagen ya no existen para nadie
        val fotos = leer<List<Foto>>("/fotos", direccion)
        assertTrue(fotos.none { it.id == sola.id })
        assertEquals(HttpStatusCode.NotFound, get("/archivos/$imagenSola") { bearerAuth(educadora) }.status)
        assertEquals(HttpStatusCode.NotFound, get("/archivos/$imagenSola") { bearerAuth(direccion) }.status)
        // la foto grupal sigue, ya sin su etiqueta, y la familia de Sofia la sigue viendo
        assertEquals(listOf(sofia.id), fotos.first { it.id == grupo.id }.ninoIds)
        assertEquals(HttpStatusCode.OK, get("/archivos/$imagenGrupo") { bearerAuth(token("familia")) }.status)
        // asistencia e incidentes ya no estan
        assertTrue(leer<List<Incidente>>("/incidentes?fecha=2027-03-01", direccion).none { it.ninoId == diego.id })
        assertTrue(leer<List<Asistencia>>("/asistencias?fecha=2027-03-01", direccion).none { it.ninoId == diego.id })
        // la cuenta de su unica tutora ya no entra
        assertEquals(HttpStatusCode.Unauthorized, login("familia.paola").status)
        // el registro de actividad ya no guarda nada de el, y el aviso del borrado no lleva su nombre
        val registros = leer<List<RegistroActividad>>("/registros", direccion)
        assertTrue(registros.none { it.ninoId == diego.id })
        assertTrue(registros.any { it.detalle == "Eliminó un expediente dado de baja y sus fotos" })
        assertTrue(registros.none { "Cavazos" in it.detalle && it.accion == mx.tec.sonrisas.domain.AccionRegistro.BORRADO })
    }

    @Test
    fun `ARCO una imagen subida sin foto no la descarga ninguna familia`() = pruebaApi {
        val educadora = token("educadora")
        val sala = leer<List<Nino>>("/ninos", educadora).first().salaId
        val imagen = subirImagen(educadora, sala)
        assertEquals(HttpStatusCode.OK, get("/archivos/$imagen") { bearerAuth(educadora) }.status)
        // Laura y Paola tienen hijos en esta sala, pero la imagen no es de ninguna foto que puedan ver
        assertEquals(HttpStatusCode.NotFound, get("/archivos/$imagen") { bearerAuth(token("familia")) }.status)
        assertEquals(HttpStatusCode.NotFound, get("/archivos/$imagen") { bearerAuth(token("familia.paola")) }.status)
    }

    @Test
    fun `ARCO la purga borra la cuenta y chats de la familia, sesiones vencidas y archivos huerfanos viejos`() {
        val url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
        val db = Db(url)
        try {
            runBlocking {
                Semilla.preparar(db, demo = true, reiniciar = false, claveFundacion = null)
                db.escribir { c ->
                    with(Conexion(c)) {
                        val paola = Tablas.usuarios.donde("usuario = ?", "familia.paola").single()
                        val diego = Tablas.ninos.todos().single { paola.tutorId in it.tutorIds }
                        assertTrue(Tablas.mensajes.donde("autor_id = ?", paola.id).isNotEmpty())
                        borrarTodo(diego)
                        assertTrue(Tablas.usuarios.porId(paola.id) == null)
                        assertTrue(Tablas.mensajes.donde("autor_id = ?", paola.id).isEmpty())
                        assertTrue(Tablas.conversaciones.todos().none { paola.id in it.participantes })
                        listOf("claves", "sesiones", "lecturas").forEach { tabla ->
                            assertEquals(0, consultar("SELECT COUNT(*) FROM $tabla WHERE usuario_id = ?", paola.id) { it.getInt(1) }.single(), tabla)
                        }

                        val ahora = System.currentTimeMillis()
                        ejecutar("INSERT INTO sesiones (token_hash, usuario_id, expira) VALUES ('vencida', 'x', ?)", ahora - 1)
                        ejecutar("INSERT INTO sesiones (token_hash, usuario_id, expira) VALUES ('vigente', 'x', ?)", ahora + 60_000)
                        val sala = Tablas.salas.todos().first().id
                        ejecutar("INSERT INTO archivos (id, sala_id, tipo, bytes, creado) VALUES ('viejo', ?, 'image/png', ?, ?)", sala, byteArrayOf(1), ahora - 25 * 3_600_000L)
                        ejecutar("INSERT INTO archivos (id, sala_id, tipo, bytes, creado) VALUES ('nuevo', ?, 'image/png', ?, ?)", sala, byteArrayOf(1), ahora)
                        purgarVencidos()
                        val sesiones = consultar("SELECT token_hash FROM sesiones WHERE usuario_id = 'x'") { it.getString(1) }
                        assertEquals(listOf("vigente"), sesiones)
                        val archivos = consultar("SELECT id FROM archivos") { it.getString(1) }
                        assertTrue("viejo" !in archivos && "nuevo" in archivos)
                        // el material de los datos de prueba no tiene fecha, pero lo usa una clase: no se borra
                        assertTrue(Tablas.material.todos().all { it.ruta in archivos })
                    }
                }
            }
        } finally {
            db.cerrar()
            DriverManager.getConnection(url).use { it.createStatement().execute("SHUTDOWN") }
        }
    }
}
