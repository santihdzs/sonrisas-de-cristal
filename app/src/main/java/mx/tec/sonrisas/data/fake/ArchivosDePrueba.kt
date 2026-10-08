package mx.tec.sonrisas.data.fake

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import mx.tec.sonrisas.data.AlmacenArchivos
import java.io.ByteArrayOutputStream
import java.io.File

data class ArchivoDemo(val clave: String, val nombre: String, val tipo: String, val ruta: String, val tamano: Long)

/** archivos reales y pequenos para que "Abrir" funcione en la demo; se generan una vez en el almacenamiento privado */
object ArchivosDePrueba {

    // los datos de prueba viven en memoria: lo que se subio en una ejecucion anterior ya no tiene dueno y se borra
    fun crear(archivos: AlmacenArchivos): List<ArchivoDemo> {
        listOf("material", "objetos").forEach { archivos.vaciar(it) }
        return generar(archivos)
    }

    private fun generar(archivos: AlmacenArchivos): List<ArchivoDemo> = listOf(
        escribir(archivos, "cancion", "Cancion de los dias de la semana.pdf", "application/pdf") {
            pdf(
                "Canción de los días de la semana",
                listOf(
                    "Lunes, martes, miércoles,", "jueves y viernes también;", "sábado y domingo", "descansamos otra vez.", "",
                    "Cantamos y aplaudimos", "un día después de otro:", "siete días tiene la semana", "y los decimos todos.", "",
                    "Para practicar en casa: canten juntos cada mañana y pregunten",
                    "qué día es hoy y qué día fue ayer.",
                ),
            )
        },
        escribir(archivos, "colores", "Actividad de colores para casa.png", "image/png") { imagenColores() },
        escribir(archivos, "rutina", "Rutina de sueno en casa.pdf", "application/pdf") {
            pdf(
                "Rutina de sueño en casa",
                listOf(
                    "Una rutina constante ayuda a que tu bebé duerma mejor.", "",
                    "1. Baño tibio a la misma hora cada noche.",
                    "2. Luz baja y voz tranquila.",
                    "3. Biberón o pecho antes de acostarlo.",
                    "4. Una canción de cuna corta.",
                    "5. Acostarlo somnoliento, pero despierto.", "",
                    "Si tienes dudas, platícalo con la educadora de la sala.",
                ),
            )
        },
    )

    private fun escribir(archivos: AlmacenArchivos, clave: String, nombre: String, tipo: String, contenido: () -> ByteArray): ArchivoDemo {
        val extension = nombre.substringAfterLast('.')
        val relativa = "material/demo-$clave.$extension"
        val ruta = archivos.escribir(relativa, contenido())
        return ArchivoDemo(clave, nombre, tipo, ruta, File(ruta).length())
    }

    private fun pdf(titulo: String, lineas: List<String>): ByteArray {
        val doc = PdfDocument()
        val pagina = doc.startPage(PdfDocument.PageInfo.Builder(612, 792, 1).create())
        val c = pagina.canvas
        val tituloPaint = Paint().apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 22f; isAntiAlias = true }
        val texto = Paint().apply { typeface = Typeface.SANS_SERIF; textSize = 14f; isAntiAlias = true }
        c.drawText("Fundación Estancias Infantiles Sonrisas de Cristal", 54f, 64f, texto)
        c.drawText(titulo, 54f, 110f, tituloPaint)
        lineas.forEachIndexed { i, l -> c.drawText(l, 54f, 160f + i * 24f, texto) }
        doc.finishPage(pagina)
        val salida = ByteArrayOutputStream()
        doc.writeTo(salida)
        doc.close()
        return salida.toByteArray()
    }

    private fun imagenColores(): ByteArray {
        val bmp = Bitmap.createBitmap(900, 640, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val texto = Paint().apply { color = Color.rgb(40, 40, 40); textSize = 34f; isAntiAlias = true; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        val chico = Paint(texto).apply { textSize = 26f; typeface = Typeface.SANS_SERIF }
        c.drawText("Actividad de colores para casa", 40f, 70f, texto)
        c.drawText("Busca en casa algo de cada color y dibújalo junto a su círculo.", 40f, 115f, chico)
        val colores = listOf("Rojo" to Color.rgb(214, 48, 49), "Azul" to Color.rgb(9, 132, 227), "Amarillo" to Color.rgb(253, 203, 110), "Verde" to Color.rgb(0, 184, 148))
        colores.forEachIndexed { i, (nombre, color) ->
            val x = 130f + i * 210f
            c.drawCircle(x, 300f, 80f, Paint().apply { this.color = color; isAntiAlias = true })
            c.drawText(nombre, x - chico.measureText(nombre) / 2, 430f, chico)
        }
        val salida = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, salida)
        bmp.recycle()
        return salida.toByteArray()
    }
}
