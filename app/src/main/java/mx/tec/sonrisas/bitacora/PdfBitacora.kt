package mx.tec.sonrisas.bitacora

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

// sans serif del sistema: siempre existe en android, asi el pdf no depende de descargar fuentes
private val normal: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
private val negrita: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

private fun pincel(tamano: Float, esNegrita: Boolean) = Paint().apply {
    typeface = if (esNegrita) negrita else normal
    textSize = tamano
    color = Color.BLACK
    isAntiAlias = true
}

object MedidorAndroid : Medidor {
    override fun ancho(texto: String, tamano: Float, negrita: Boolean): Float = pincel(tamano, negrita).measureText(texto)
}

object PdfBitacora {

    fun escribir(paginas: List<Pagina>, destino: File) {
        val doc = PdfDocument()
        val linea = Paint().apply { color = Color.rgb(90, 90, 90); strokeWidth = 0.5f; style = Paint.Style.STROKE }
        val fondo = Paint().apply { color = Color.rgb(235, 235, 235); style = Paint.Style.FILL }
        try {
            paginas.forEachIndexed { i, p ->
                val pagina = doc.startPage(PdfDocument.PageInfo.Builder(p.ancho.toInt(), p.alto.toInt(), i + 1).create())
                val c = pagina.canvas
                // primero los fondos para que las lineas y el texto queden encima
                p.elementos.filterIsInstance<Elemento.Fondo>().forEach { c.drawRect(it.x, it.y, it.x + it.ancho, it.y + it.alto, fondo) }
                p.elementos.forEach { e ->
                    when (e) {
                        is Elemento.Linea -> c.drawLine(e.x1, e.y1, e.x2, e.y2, linea)
                        is Elemento.Texto -> c.drawText(e.texto, e.x, e.y, pincel(e.tamano, e.negrita))
                        is Elemento.Fondo -> Unit
                    }
                }
                doc.finishPage(pagina)
            }
            destino.parentFile?.mkdirs()
            destino.outputStream().use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }
}
