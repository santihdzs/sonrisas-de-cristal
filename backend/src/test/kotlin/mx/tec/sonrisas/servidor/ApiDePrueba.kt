package mx.tec.sonrisas.servidor

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import mx.tec.sonrisas.data.JsonApi
import mx.tec.sonrisas.data.Login
import mx.tec.sonrisas.data.RespuestaLogin
import kotlinx.serialization.builtins.ListSerializer
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.RelojSistema
import java.sql.DriverManager
import java.time.LocalDateTime
import java.util.UUID

// el servidor pone el dia de cada registro; las pruebas fijan su reloj al dia que usan (1 de marzo de 2027, ya terminada la jornada)
val AHORA_PRUEBAS: LocalDateTime = LocalDateTime.of(2027, 3, 1, 18, 0)

/**
 * una base nueva por prueba: h2 en memoria, o postgres real si existe PRUEBAS_DATABASE_URL
 * (postgres://usuario:clave@host:puerto/base); en postgres cada prueba usa su propio esquema y lo borra al final
 */
fun <T> conBaseDePrueba(bloque: (Db) -> T): T {
    val pg = System.getenv("PRUEBAS_DATABASE_URL")
    if (pg.isNullOrBlank()) {
        val url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
        val db = Db(url)
        try {
            return bloque(db)
        } finally {
            // DB_CLOSE_DELAY=-1 deja viva cada base en memoria; sin esto la suite completa se queda sin heap
            db.cerrar()
            DriverManager.getConnection(url).use { it.createStatement().execute("SHUTDOWN") }
        }
    }
    val (jdbc, usuario, clave) = Db.jdbcDe(pg)
    val esquema = "prueba_" + UUID.randomUUID().toString().replace("-", "")
    fun sql(q: String) = DriverManager.getConnection(jdbc, usuario, clave).use { it.createStatement().execute(q) }
    sql("CREATE SCHEMA $esquema")
    val db = Db(jdbc + (if ("?" in jdbc) "&" else "?") + "currentSchema=$esquema", usuario, clave)
    try {
        return bloque(db)
    } finally {
        db.cerrar()
        sql("DROP SCHEMA $esquema CASCADE")
    }
}

// servidor real con los datos de prueba, compartido por las pruebas de requisitos (RF-xx)
fun pruebaApi(ahora: LocalDateTime = AHORA_PRUEBAS, bloque: suspend HttpClient.() -> Unit) = conBaseDePrueba { db ->
    relojServidor = object : Reloj { override fun ahora() = ahora }
    try {
        testApplication {
            runBlocking { Semilla.preparar(db, demo = true, reiniciar = false, claveFundacion = null) }
            application { modulo(db, intentosLogin = 100) }
            client.bloque()
        }
    } finally {
        relojServidor = RelojSistema
    }
}

suspend fun HttpClient.login(usuario: String, contrasena: String = "demo"): HttpResponse = post("/auth/login") {
    contentType(ContentType.Application.Json)
    setBody(JsonApi.encodeToString(Login.serializer(), Login(usuario, contrasena)))
}

suspend fun HttpClient.sesion(usuario: String): RespuestaLogin =
    JsonApi.decodeFromString(RespuestaLogin.serializer(), login(usuario).bodyAsText())

suspend fun HttpClient.token(usuario: String): String = sesion(usuario).token

suspend inline fun <reified T> HttpClient.leer(ruta: String, token: String): T =
    JsonApi.decodeFromString<T>(get(ruta) { bearerAuth(token) }.bodyAsText())

suspend fun HttpClient.enviarAsistencia(ruta: String, token: String, a: Asistencia): HttpResponse = post(ruta) {
    bearerAuth(token)
    contentType(ContentType.Application.Json)
    setBody(JsonApi.encodeToString(Asistencia.serializer(), a))
}

suspend fun HttpClient.enviarEventos(token: String, eventos: List<EventoBitacora>): HttpResponse = post("/eventos") {
    bearerAuth(token)
    contentType(ContentType.Application.Json)
    setBody(JsonApi.encodeToString(ListSerializer(EventoBitacora.serializer()), eventos))
}

suspend fun HttpClient.subirFoto(token: String, foto: Foto): HttpResponse = post("/fotos") {
    bearerAuth(token)
    contentType(ContentType.Application.Json)
    setBody(JsonApi.encodeToString(Foto.serializer(), foto))
}

// una imagen real chica, hecha con ImageIO, para las subidas de fotos
fun imagenDePrueba(formato: String = "jpeg"): ByteArray = java.io.ByteArrayOutputStream().also {
    javax.imageio.ImageIO.write(java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_RGB), formato, it)
}.toByteArray()
