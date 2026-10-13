package mx.tec.sonrisas.data

import java.io.File
import java.io.InputStream

/** lo que la pantalla entrega a la capa de datos; abrir() se llama una sola vez */
class ArchivoEntrada(val nombre: String, val tipo: String, val tamano: Long?, val abrir: () -> InputStream)

/** copias en el almacenamiento privado de la app; ni la galeria ni otras apps las ven */
class AlmacenArchivos(private val raiz: File) {

    /** devuelve la ruta absoluta del archivo guardado */
    fun guardar(carpeta: String, id: String, entrada: ArchivoEntrada): String {
        if ((entrada.tamano ?: 0) > LIMITE_BYTES) throw ArchivoMuyGrande()
        val destino = File(raiz, "$carpeta/$id.${extension(entrada)}")
        destino.parentFile?.mkdirs()
        try {
            entrada.abrir().use { origen ->
                destino.outputStream().use { salida ->
                    val buffer = ByteArray(8 * 1024)
                    var total = 0L
                    while (true) {
                        val leidos = origen.read(buffer)
                        if (leidos < 0) break
                        total += leidos
                        // el tamano reportado puede faltar o mentir, asi que se vuelve a contar al copiar
                        if (total > LIMITE_BYTES) throw ArchivoMuyGrande()
                        salida.write(buffer, 0, leidos)
                    }
                }
            }
        } catch (e: Exception) {
            destino.delete()
            throw e
        }
        return destino.path
    }

    /** para los archivos de los datos de prueba */
    fun escribir(rutaRelativa: String, bytes: ByteArray): String {
        val destino = File(raiz, rutaRelativa)
        destino.parentFile?.mkdirs()
        destino.writeBytes(bytes)
        return destino.path
    }

    fun vaciar(carpeta: String) {
        File(raiz, carpeta).deleteRecursively()
    }

    fun borrar(ruta: String) {
        File(ruta).delete()
    }

    private fun extension(entrada: ArchivoEntrada): String {
        val porNombre = entrada.nombre.substringAfterLast('.', "").lowercase()
        if (porNombre.isNotBlank() && porNombre.length <= 5) return porNombre
        return when (entrada.tipo) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            else -> "bin"
        }
    }
}
