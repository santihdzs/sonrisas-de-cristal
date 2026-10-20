package mx.tec.sonrisas.servidor

import io.ktor.server.application.createApplicationPlugin
import mx.tec.sonrisas.domain.Usuario
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

val azarSeguro = SecureRandom()

private val b64 = Base64.getEncoder().withoutPadding()
private val b64url = Base64.getUrlEncoder().withoutPadding()
private val b64Leer = Base64.getDecoder()

/** contrasenas con pbkdf2 de la jdk: "pbkdf2$iteraciones$sal$hash" */
object Claves {
    private const val ITERACIONES = 210_000

    private fun derivar(clave: String, sal: ByteArray, iteraciones: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(clave.toCharArray(), sal, iteraciones, 256))
            .encoded

    fun hash(clave: String): String {
        val sal = ByteArray(16).also(azarSeguro::nextBytes)
        return "pbkdf2$$ITERACIONES$${b64.encodeToString(sal)}$${b64.encodeToString(derivar(clave, sal, ITERACIONES))}"
    }

    fun verificar(clave: String, guardado: String): Boolean {
        val partes = guardado.split("$")
        if (partes.size != 4 || partes[0] != "pbkdf2") return false
        val esperado = b64Leer.decode(partes[3])
        return MessageDigest.isEqual(esperado, derivar(clave, b64Leer.decode(partes[2]), partes[1].toInt()))
    }

    // se verifica contra este hash cuando el usuario no existe, para que tarde lo mismo y no delate que cuentas existen
    val falso: String by lazy { hash("sin-cuenta") }
}

/** tokens opacos: el telefono guarda el token y la base solo su sha-256, asi que una copia de la base no sirve para entrar */
object Sesiones {
    val DURACION = 30.days

    fun nuevoToken(): String = b64url.encodeToString(ByteArray(32).also(azarSeguro::nextBytes))

    fun huella(token: String): String = b64.encodeToString(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))

    fun crear(c: Conexion, usuarioId: String): String {
        val token = nuevoToken()
        c.ejecutar(
            "INSERT INTO sesiones (token_hash, usuario_id, expira) VALUES (?, ?, ?)",
            huella(token), usuarioId, System.currentTimeMillis() + DURACION.inWholeMilliseconds,
        )
        return token
    }

    /** el usuario vigente del token; una cuenta sin acceso deja de entrar aunque su token no haya vencido */
    fun usuario(c: Conexion, token: String): Usuario? {
        val id = c.consultar(
            "SELECT usuario_id FROM sesiones WHERE token_hash = ? AND expira > ?", huella(token), System.currentTimeMillis(),
        ) { it.getString(1) }.firstOrNull() ?: return null
        return with(c) { Tablas.usuarios.porId(id) }?.takeIf { it.activo }
    }

    fun cerrar(c: Conexion, token: String) = c.ejecutar("DELETE FROM sesiones WHERE token_hash = ?", huella(token))

    fun cerrarTodas(c: Conexion, usuarioId: String) = c.ejecutar("DELETE FROM sesiones WHERE usuario_id = ?", usuarioId)
}

/**
 * P4: bloqueo temporal por cuenta. Tras [maxFallos] contrasenas incorrectas dentro de [duracion], esa cuenta queda
 * bloqueada [duracion], venga de la ip que venga. Cuenta tambien nombres que no existen: si solo se bloquearan
 * cuentas reales, el 429 delataria cuales existen. Vive en memoria: al reiniciar el servidor se limpia.
 */
class BloqueoCuentas(
    private val maxFallos: Int = 5,
    private val duracion: Duration = 15.minutes,
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    private class Estado(val fallos: Int, val desde: Long, val bloqueadaHasta: Long)

    private val estados = ConcurrentHashMap<String, Estado>()

    fun bloqueada(usuario: String): Boolean = (estados[usuario]?.bloqueadaHasta ?: 0L) > reloj()

    fun fallo(usuario: String) {
        val ahora = reloj()
        val ventana = duracion.inWholeMilliseconds
        // memoria acotada: si alguien prueba miles de nombres distintos, se olvidan los que ya vencieron
        if (estados.size > 10_000) estados.entries.removeIf { ahora - it.value.desde > ventana && it.value.bloqueadaHasta < ahora }
        estados.compute(usuario) { _, previo ->
            val vigente = previo?.takeIf { ahora - it.desde < ventana || it.bloqueadaHasta > ahora }
            val fallos = (vigente?.fallos ?: 0) + 1
            Estado(fallos, vigente?.desde ?: ahora, if (fallos >= maxFallos) ahora + ventana else 0L)
        }
    }

    fun exito(usuario: String) {
        estados.remove(usuario)
    }
}

/**
 * P1: cabeceras de seguridad en TODAS las respuestas, tambien en los errores.
 * - HSTS: el navegador ya no vuelve a entrar por http (evita SSL stripping)
 * - nosniff: nadie interpreta un archivo como si fuera de otro tipo (MIME sniffing)
 * - X-Frame-Options y frame-ancestors: la API no se puede meter en un iframe (clickjacking)
 * - CSP: una API de json no necesita cargar scripts ni recursos de ningun lado
 * - Referrer-Policy: las urls con ids no se filtran en la cabecera Referer
 * - no-store: ni proxies ni el telefono guardan en cache datos de ninos
 */
val CabecerasSeguras = createApplicationPlugin("CabecerasSeguras") {
    onCall { call ->
        with(call.response.headers) {
            append("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
            append("X-Content-Type-Options", "nosniff")
            append("X-Frame-Options", "DENY")
            append("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
            append("Referrer-Policy", "no-referrer")
            append("Cache-Control", "no-store")
        }
    }
}
