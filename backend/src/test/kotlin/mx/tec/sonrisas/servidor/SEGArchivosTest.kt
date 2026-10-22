package mx.tec.sonrisas.servidor

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import mx.tec.sonrisas.data.Id
import mx.tec.sonrisas.data.JsonApi
import mx.tec.sonrisas.data.LIMITE_BYTES
import mx.tec.sonrisas.domain.MaterialClase
import mx.tec.sonrisas.domain.Nino
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** subidas de fotos: solo jpeg o png de verdad, y sin metadatos (gps) al guardarse */
class SEGArchivosTest {

    private val gps = "GPSLatitude 25.6866 N GPSLongitude 100.3161 W"

    // segmento APP1 "Exif" con datos de ubicacion, metido justo despues del inicio del jpeg
    private fun jpegConGps(): ByteArray {
        val limpio = imagenDePrueba()
        val datos = "Exif\u0000\u0000".toByteArray() + gps.toByteArray()
        val largo = datos.size + 2
        val app1 = byteArrayOf(0xFF.toByte(), 0xE1.toByte(), (largo shr 8).toByte(), largo.toByte()) + datos
        return limpio.copyOfRange(0, 2) + app1 + limpio.copyOfRange(2, limpio.size)
    }

    private suspend fun HttpClient.sala(t: String) = leer<List<Nino>>("/ninos", t).first().salaId

    private suspend fun HttpClient.subir(t: String, cuerpo: ByteArray, tipo: ContentType = ContentType.Image.JPEG): HttpResponse =
        post("/fotos/imagen?sala=${sala(t)}") { bearerAuth(t); contentType(tipo); setBody(cuerpo) }

    private suspend fun HttpResponse.id() = JsonApi.decodeFromString(Id.serializer(), bodyAsText()).id

    @Test
    fun laFotoSeGuardaSinExifNiGps() = pruebaApi {
        val t = token("educadora")
        val original = jpegConGps()
        assertEquals(true, String(original, Charsets.ISO_8859_1).contains(gps))
        val subida = subir(t, original)
        assertEquals(HttpStatusCode.OK, subida.status)
        val guardada = get("/archivos/${subida.id()}") { bearerAuth(t) }
        assertEquals(ContentType.Image.JPEG, guardada.headers["Content-Type"]?.let(ContentType::parse))
        val bytes = guardada.readRawBytes()
        val texto = String(bytes, Charsets.ISO_8859_1)
        assertFalse(texto.contains("Exif"))
        assertFalse(texto.contains("GPS"))
        assertFalse((0 until bytes.size - 1).any { bytes[it] == 0xFF.toByte() && bytes[it + 1] == 0xE1.toByte() })
        // el material de clase tambien: una imagen subida como material pierde su gps
        val material = post("/material?sala=${sala(t)}&titulo=foto&nombre=foto.jpg") { bearerAuth(t); contentType(ContentType.Image.JPEG); setBody(original) }
        val ruta = JsonApi.decodeFromString(MaterialClase.serializer(), material.bodyAsText()).ruta
        assertFalse(String(get("/archivos/$ruta") { bearerAuth(t) }.readRawBytes(), Charsets.ISO_8859_1).contains("GPS"))
    }

    @Test
    fun unPngSeAceptaYSeGuardaComoPng() = pruebaApi {
        val t = token("educadora")
        // el cliente dice jpeg, pero se guarda el tipo real
        val subida = subir(t, imagenDePrueba("png"))
        assertEquals(HttpStatusCode.OK, subida.status)
        val guardada = get("/archivos/${subida.id()}") { bearerAuth(t) }
        assertEquals(ContentType.Image.PNG, guardada.headers["Content-Type"]?.let(ContentType::parse))
    }

    @Test
    fun unPhpDisfrazadoDeJpegSeRechaza() = pruebaApi {
        val t = token("educadora")
        val php = "<?php system(\$_GET['c']); ?>".toByteArray()
        assertEquals(HttpStatusCode.BadRequest, subir(t, php).status)
        val objeto = post("/objetos?sala=${sala(t)}&descripcion=gorra&donde=patio&fecha=2026-01-05&nombre=foto.jpg") {
            bearerAuth(t); contentType(ContentType.Image.JPEG); setBody(php)
        }
        assertEquals(HttpStatusCode.BadRequest, objeto.status)
    }

    @Test
    fun unaFotoDeMasDelLimiteSeRechaza() = pruebaApi {
        val t = token("educadora")
        assertEquals(HttpStatusCode.PayloadTooLarge, subir(t, ByteArray(LIMITE_BYTES.toInt() + 1)).status)
    }

    @Test
    fun unEncabezadoJpegConBasuraSeRechaza() = pruebaApi {
        val t = token("educadora")
        val basura = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + "<?php echo 1; ?>".repeat(200).toByteArray()
        assertEquals(HttpStatusCode.BadRequest, subir(t, basura).status)
    }

    @Test
    fun unaRafagaDeSubidasRecibe429() = pruebaApi {
        val t = token("educadora")
        // el limite "subidas" es de 30 por minuto y por ip (Main.kt); la prueba manda una de mas
        val estados = List(31) { subir(t, imagenDePrueba()).status }
        assertTrue(estados.take(30).all { it == HttpStatusCode.OK })
        assertEquals(HttpStatusCode.TooManyRequests, estados.last())
    }
}
