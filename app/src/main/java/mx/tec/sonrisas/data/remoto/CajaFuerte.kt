package mx.tec.sonrisas.data.remoto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * P7: cifra el token de sesion antes de guardarlo en las preferencias del telefono.
 * - La llave AES-256 vive en el Android Keystore: la app la usa, pero nadie la puede exportar,
 *   y en muchos telefonos esta en hardware (TEE o StrongBox).
 * - AES-GCM es AEAD: oculta el token y su authentication tag detecta si alguien altero lo guardado.
 * - Se usa el Keystore directo porque EncryptedSharedPreferences quedo deprecado.
 */
object CajaFuerte {
    private const val ALIAS = "sonrisas-sesion"
    private const val TRANSFORMACION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    private fun llave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generador.generateKey()
    }

    /** iv + texto cifrado + tag, en base64; el keystore elige un iv aleatorio nuevo en cada cifrado */
    fun cifrar(texto: String): String {
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, llave()) }
        val datos = cifrador.iv + cifrador.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(datos, Base64.NO_WRAP)
    }

    /** null si no se puede descifrar: texto alterado, llave borrada o un token viejo que se guardo sin cifrar */
    fun descifrar(guardado: String): String? = runCatching {
        val datos = Base64.decode(guardado, Base64.NO_WRAP)
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply {
            init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(TAG_BITS, datos, 0, IV_BYTES))
        }
        String(cifrador.doFinal(datos, IV_BYTES, datos.size - IV_BYTES), Charsets.UTF_8)
    }.getOrNull()
}
