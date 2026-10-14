package mx.tec.sonrisas.data.remoto

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.ArchivoMuyGrande
import mx.tec.sonrisas.data.Error
import mx.tec.sonrisas.data.JsonApi
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** el token ya no sirve (vencio, cerraron la sesion o retiraron el acceso) */
class SesionVencida : IOException("La sesión terminó.")

/** 429: el servidor limita los intentos de inicio de sesion */
class DemasiadosIntentos : IOException("Demasiados intentos.")

/** 404: el recurso no existe o el usuario no lo puede ver */
class NoEncontrado : IOException("No encontrado.")

fun codificar(valor: String): String = URLEncoder.encode(valor, "UTF-8")

inline fun <reified T> String.como(): T = JsonApi.decodeFromString(this)

/** cliente http minimo; convierte los errores del servidor en las mismas excepciones que lanzan los repositorios falsos */
class Api(servidor: String, private val token: () -> String?, private val alVencer: () -> Unit) {
    private val base = servidor.trimEnd('/')

    suspend fun <R> pedir(
        metodo: String,
        ruta: String,
        tipo: String? = null,
        escribir: ((OutputStream) -> Unit)? = null,
        leer: (InputStream) -> R,
    ): R = withContext(Dispatchers.IO) {
        val con = URL(base + ruta).openConnection() as HttpURLConnection
        try {
            con.requestMethod = metodo
            con.connectTimeout = 10_000
            con.readTimeout = 30_000
            val enviado = token()
            enviado?.let { con.setRequestProperty("Authorization", "Bearer $it") }
            if (escribir != null) {
                con.doOutput = true
                con.setChunkedStreamingMode(0)
                tipo?.let { con.setRequestProperty("Content-Type", it) }
                con.outputStream.use(escribir)
            }
            val codigo = con.responseCode
            if (codigo in 200..299) return@withContext con.inputStream.use(leer)
            val mensaje = runCatching { JsonApi.decodeFromString(Error.serializer(), con.errorStream.readBytes().decodeToString()).mensaje }.getOrNull()
            throw when (codigo) {
                401 -> SesionVencida().also { if (enviado != null) alVencer() }
                403 -> AccesoDenegado(mensaje ?: "No tienes permiso para hacer esto.")
                404 -> NoEncontrado()
                400 -> IllegalArgumentException(mensaje)
                409 -> IllegalStateException(mensaje)
                413 -> ArchivoMuyGrande()
                429 -> DemasiadosIntentos()
                else -> IOException("El servidor respondió $codigo.")
            }
        } finally {
            con.disconnect()
        }
    }

    suspend fun texto(metodo: String, ruta: String, json: String? = null): String =
        pedir(metodo, ruta, "application/json", json?.let { j -> { it.write(j.toByteArray()) } }) { it.readBytes().decodeToString() }

    suspend inline fun <reified T> get(ruta: String): T = JsonApi.decodeFromString(texto("GET", ruta))

    /** null si el servidor responde 404 */
    suspend inline fun <reified T> getONulo(ruta: String): T? = try {
        get<T>(ruta)
    } catch (e: NoEncontrado) {
        null
    }

    /** manda el cuerpo como json y devuelve la respuesta sin leer; se lee con como<T>() */
    suspend inline fun <reified C> enviar(metodo: String, ruta: String, cuerpo: C): String =
        texto(metodo, ruta, JsonApi.encodeToString(cuerpo))

    suspend fun post(ruta: String): String = texto("POST", ruta)

    suspend fun delete(ruta: String): String = texto("DELETE", ruta)

    /** baja el archivo completo a un temporal y luego lo mueve, para no dejar archivos a medias */
    suspend fun descargar(ruta: String, destino: File) {
        val temporal = File(destino.parentFile, destino.name + ".parcial")
        destino.parentFile?.mkdirs()
        pedir("GET", ruta) { entrada -> temporal.outputStream().use { entrada.copyTo(it) } }
        if (!temporal.renameTo(destino)) throw IOException("No se pudo guardar el archivo.")
    }
}

/** avisa a las consultas abiertas que algo cambio, para que se vuelvan a pedir sin esperar al siguiente sondeo */
class Cambios {
    val version = MutableStateFlow(0)
    fun avisar() = version.update { it + 1 }
}

const val SONDEO_NORMAL = 15_000L
const val SONDEO_CHAT = 3_000L

/**
 * consulta que se repite cada intervalo o en cuanto la app cambia algo.
 * shortcut: sondeo en vez de notificaciones push; si el chat necesita ser instantaneo, websockets o fcm
 */
fun <T> Cambios.consulta(intervalo: Long = SONDEO_NORMAL, cargar: suspend () -> T): Flow<T> = flow {
    var cargado = false
    while (true) {
        val visto = version.value
        val resultado = try {
            Result.success(cargar())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // el primer error se muestra en la pantalla; despues se conserva lo ultimo que llego
            if (!cargado) throw e
            Result.failure(e)
        }
        resultado.onSuccess {
            cargado = true
            emit(it)
        }
        withTimeoutOrNull(intervalo) { version.first { it != visto } }
    }
}.distinctUntilChanged()
