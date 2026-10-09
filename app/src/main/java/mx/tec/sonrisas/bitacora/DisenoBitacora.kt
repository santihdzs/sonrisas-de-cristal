package mx.tec.sonrisas.bitacora

// todo en puntos (1/72 de pulgada); el mismo contenido siempre produce las mismas paginas

data class Fila(val etiqueta: String, val valores: List<String>)

data class Seccion(val titulo: String, val filas: List<Fila>)

/** lo que se imprime de un nino, ya en texto; no sabe nada de paginas */
data class ContenidoBitacora(
    val titulo: String,
    val datosGenerales: List<Pair<String, String>>,
    val columnas: List<String>,
    val secciones: List<Seccion>,
    val pie: String,
)

enum class Orientacion(val ancho: Float, val alto: Float, val anchoEtiqueta: Float) {
    // carta horizontal para la semana (cinco columnas) y vertical para un solo dia
    HORIZONTAL(792f, 612f, 150f),
    VERTICAL(612f, 792f, 190f),
}

sealed interface Elemento {
    /** y es la linea base del texto */
    data class Texto(val x: Float, val y: Float, val texto: String, val tamano: Float, val negrita: Boolean = false) : Elemento
    data class Linea(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : Elemento
    data class Fondo(val x: Float, val y: Float, val ancho: Float, val alto: Float) : Elemento
}

data class Pagina(val ancho: Float, val alto: Float, val elementos: List<Elemento>)

fun interface Medidor {
    fun ancho(texto: String, tamano: Float, negrita: Boolean): Float
}

object Tipografia {
    const val TITULO = 12f
    const val ENCABEZADO = 8f
    const val SECCION = 8.5f
    const val CELDA = 7.5f

    // ningun texto del pdf baja de aqui; si no cabe, se continua en otra pagina
    const val MINIMO = 7f
    fun interlineado(tamano: Float) = tamano * 1.25f
}

private const val MARGEN = 32f
private const val RELLENO = 3f
private const val MAX_LINEAS_CELDA = 4

/** corta por palabras; una palabra mas ancha que la columna se parte en pedazos */
fun envolver(texto: String, ancho: Float, tamano: Float, negrita: Boolean, medidor: Medidor): List<String> {
    if (texto.isBlank()) return listOf("")
    val lineas = mutableListOf<String>()
    texto.split("\n").forEach { parrafo ->
        var actual = ""
        parrafo.split(" ").filter { it.isNotEmpty() }.forEach { palabra ->
            val tentativa = if (actual.isEmpty()) palabra else "$actual $palabra"
            when {
                medidor.ancho(tentativa, tamano, negrita) <= ancho -> actual = tentativa
                medidor.ancho(palabra, tamano, negrita) <= ancho -> {
                    lineas += actual
                    actual = palabra
                }
                else -> {
                    if (actual.isNotEmpty()) lineas += actual
                    var resto = palabra
                    while (medidor.ancho(resto, tamano, negrita) > ancho && resto.length > 1) {
                        var corte = resto.length - 1
                        while (corte > 1 && medidor.ancho(resto.take(corte), tamano, negrita) > ancho) corte--
                        lineas += resto.take(corte)
                        resto = resto.drop(corte)
                    }
                    actual = resto
                }
            }
        }
        lineas += actual
    }
    return lineas
}

data class Nota(val numero: Int, val referencia: String, val texto: String)

/** calcula las paginas de un nino: encabezado, datos generales, tabla por secciones y anexo de observaciones */
class DisenoBitacora(private val orientacion: Orientacion, private val medidor: Medidor) {
    private val ancho = orientacion.ancho
    private val alto = orientacion.alto
    private val limite = alto - MARGEN - 14f
    private val anchoUtil = ancho - 2 * MARGEN

    private fun anchoColumna(columnas: Int) = (anchoUtil - orientacion.anchoEtiqueta) / columnas

    fun paginar(c: ContenidoBitacora, generado: String): List<Pagina> {
        val notas = mutableListOf<Nota>()
        val paginas = mutableListOf<MutableList<Elemento>>()
        var y = 0f
        lateinit var actual: MutableList<Elemento>

        fun nueva(conDatos: Boolean) {
            actual = mutableListOf()
            paginas += actual
            y = encabezado(actual, c, generado)
            if (conDatos) y = datosGenerales(actual, c, y)
            y = cabeceraColumnas(actual, c, y)
        }
        nueva(conDatos = true)

        c.secciones.forEach { seccion ->
            seccion.filas.forEachIndexed { i, fila ->
                val celdas = celdas(fila, c, seccion, notas)
                val altoFila = altoDe(celdas)
                val altoTitulo = if (i == 0) Tipografia.interlineado(Tipografia.SECCION) + 2 * RELLENO else 0f
                // el titulo de una seccion nunca queda solo al final de la pagina
                if (y + altoTitulo + altoFila > limite) nueva(conDatos = false)
                if (i == 0) y = tituloSeccion(actual, seccion.titulo, y)
                y = fila(actual, celdas, c.columnas.size, y)
            }
        }

        var anexo = notas.toList()
        while (anexo.isNotEmpty()) {
            actual = mutableListOf()
            paginas += actual
            y = encabezado(actual, c, generado)
            y = tituloSeccion(actual, "Observaciones (continuación de las notas largas)", y)
            val pendientes = escribirNotas(actual, anexo, y)
            // si una pagina vacia no avanza, se detiene en lugar de generar paginas sin fin
            if (pendientes == anexo) break
            anexo = pendientes
        }

        val total = paginas.size
        return paginas.mapIndexed { i, elementos ->
            val pie = "${c.pie} · Página ${i + 1} de $total"
            elementos += Elemento.Texto(MARGEN, alto - MARGEN + 4f, pie, Tipografia.MINIMO)
            Pagina(ancho, alto, elementos)
        }
    }

    private fun encabezado(e: MutableList<Elemento>, c: ContenidoBitacora, generado: String): Float {
        var y = MARGEN + Tipografia.ENCABEZADO
        e += Elemento.Texto(MARGEN, y, "Fundación Estancias Infantiles Sonrisas de Cristal", Tipografia.ENCABEZADO, negrita = true)
        val gen = "Generado el $generado"
        e += Elemento.Texto(ancho - MARGEN - medidor.ancho(gen, Tipografia.ENCABEZADO, false), y, gen, Tipografia.ENCABEZADO)
        y += Tipografia.interlineado(Tipografia.TITULO) + 2f
        envolver(c.titulo, anchoUtil, Tipografia.TITULO, true, medidor).forEach {
            e += Elemento.Texto(MARGEN, y, it, Tipografia.TITULO, negrita = true)
            y += Tipografia.interlineado(Tipografia.TITULO)
        }
        e += Elemento.Linea(MARGEN, y - 6f, ancho - MARGEN, y - 6f)
        return y
    }

    private fun datosGenerales(e: MutableList<Elemento>, c: ContenidoBitacora, inicio: Float): Float {
        var y = tituloSeccion(e, "I. Datos generales", inicio)
        val mitad = anchoUtil / 2
        c.datosGenerales.chunked(2).forEach { par ->
            var alturaFila = 0f
            par.forEachIndexed { i, (k, v) ->
                val lineas = envolver("$k: $v", mitad - 2 * RELLENO, Tipografia.CELDA, false, medidor)
                lineas.forEachIndexed { n, l ->
                    e += Elemento.Texto(MARGEN + i * mitad + RELLENO, y + Tipografia.CELDA + n * Tipografia.interlineado(Tipografia.CELDA), l, Tipografia.CELDA)
                }
                alturaFila = maxOf(alturaFila, lineas.size * Tipografia.interlineado(Tipografia.CELDA))
            }
            y += alturaFila + RELLENO
        }
        return y + RELLENO
    }

    private fun cabeceraColumnas(e: MutableList<Elemento>, c: ContenidoBitacora, inicio: Float): Float {
        val col = anchoColumna(c.columnas.size)
        val lineas = c.columnas.map { envolver(it, col - 2 * RELLENO, Tipografia.CELDA, true, medidor) }
        val alto = (lineas.maxOf { it.size }) * Tipografia.interlineado(Tipografia.CELDA) + 2 * RELLENO
        e += Elemento.Fondo(MARGEN, inicio, anchoUtil, alto)
        e += Elemento.Texto(MARGEN + RELLENO, inicio + RELLENO + Tipografia.CELDA, "Fecha", Tipografia.CELDA, negrita = true)
        lineas.forEachIndexed { i, ls ->
            ls.forEachIndexed { n, l ->
                e += Elemento.Texto(
                    MARGEN + orientacion.anchoEtiqueta + i * col + RELLENO,
                    inicio + RELLENO + Tipografia.CELDA + n * Tipografia.interlineado(Tipografia.CELDA),
                    l, Tipografia.CELDA, negrita = true,
                )
            }
        }
        rejilla(e, inicio, alto, c.columnas.size)
        return inicio + alto
    }

    private fun tituloSeccion(e: MutableList<Elemento>, titulo: String, inicio: Float): Float {
        val alto = Tipografia.interlineado(Tipografia.SECCION) + 2 * RELLENO
        e += Elemento.Fondo(MARGEN, inicio, anchoUtil, alto)
        e += Elemento.Texto(MARGEN + RELLENO, inicio + RELLENO + Tipografia.SECCION, titulo, Tipografia.SECCION, negrita = true)
        e += Elemento.Linea(MARGEN, inicio, ancho - MARGEN, inicio)
        return inicio + alto
    }

    // una celda que no cabe en MAX_LINEAS_CELDA deja una referencia y el texto completo va al anexo
    private fun celdas(fila: Fila, c: ContenidoBitacora, seccion: Seccion, notas: MutableList<Nota>): List<List<String>> {
        val col = anchoColumna(c.columnas.size) - 2 * RELLENO
        val etiqueta = envolver(fila.etiqueta, orientacion.anchoEtiqueta - 2 * RELLENO, Tipografia.CELDA, true, medidor)
        val valores = fila.valores.mapIndexed { i, v ->
            val lineas = envolver(v, col, Tipografia.CELDA, false, medidor)
            if (lineas.size <= MAX_LINEAS_CELDA) lineas
            else {
                val numero = notas.size + 1
                notas += Nota(numero, "${c.columnas.getOrElse(i) { "" }} · ${seccion.titulo} · ${fila.etiqueta}", v)
                val referencia = envolver("(sigue en Observaciones, nota $numero)", col, Tipografia.CELDA, false, medidor)
                lineas.take(MAX_LINEAS_CELDA - referencia.size) + referencia
            }
        }
        return listOf(etiqueta) + valores
    }

    private fun altoDe(celdas: List<List<String>>) = celdas.maxOf { it.size } * Tipografia.interlineado(Tipografia.CELDA) + 2 * RELLENO

    private fun fila(e: MutableList<Elemento>, celdas: List<List<String>>, columnas: Int, inicio: Float): Float {
        val col = anchoColumna(columnas)
        val alto = altoDe(celdas)
        celdas.forEachIndexed { i, lineas ->
            val x = if (i == 0) MARGEN + RELLENO else MARGEN + orientacion.anchoEtiqueta + (i - 1) * col + RELLENO
            lineas.forEachIndexed { n, l ->
                e += Elemento.Texto(x, inicio + RELLENO + Tipografia.CELDA + n * Tipografia.interlineado(Tipografia.CELDA), l, Tipografia.CELDA, negrita = i == 0)
            }
        }
        rejilla(e, inicio, alto, columnas)
        return inicio + alto
    }

    private fun rejilla(e: MutableList<Elemento>, inicio: Float, alto: Float, columnas: Int) {
        val col = anchoColumna(columnas)
        e += Elemento.Linea(MARGEN, inicio + alto, ancho - MARGEN, inicio + alto)
        e += Elemento.Linea(MARGEN, inicio, MARGEN, inicio + alto)
        e += Elemento.Linea(ancho - MARGEN, inicio, ancho - MARGEN, inicio + alto)
        (0 until columnas).forEach { i ->
            val x = MARGEN + orientacion.anchoEtiqueta + i * col
            e += Elemento.Linea(x, inicio, x, inicio + alto)
        }
    }

    /** escribe las notas que caben y devuelve las que faltan; una nota larga puede partirse entre paginas */
    private fun escribirNotas(e: MutableList<Elemento>, notas: List<Nota>, inicio: Float): List<Nota> {
        var y = inicio + RELLENO
        val salto = Tipografia.interlineado(Tipografia.CELDA)
        notas.forEachIndexed { i, nota ->
            val cabeza = envolver("Nota ${nota.numero} · ${nota.referencia}", anchoUtil - 2 * RELLENO, Tipografia.CELDA, true, medidor)
            val lineas = envolver(nota.texto, anchoUtil - 2 * RELLENO, Tipografia.CELDA, false, medidor)
            if (y + (cabeza.size + 2) * salto > limite) return notas.drop(i)
            cabeza.forEach {
                e += Elemento.Texto(MARGEN + RELLENO, y + Tipografia.CELDA, it, Tipografia.CELDA, negrita = true)
                y += salto
            }
            lineas.forEachIndexed { n, l ->
                if (y + salto > limite) {
                    val resto = lineas.drop(n).joinToString(" ")
                    return listOf(nota.copy(referencia = "${nota.referencia} (continuación)", texto = resto)) + notas.drop(i + 1)
                }
                e += Elemento.Texto(MARGEN + RELLENO, y + Tipografia.CELDA, l, Tipografia.CELDA)
                y += salto
            }
            y += RELLENO
        }
        return emptyList()
    }
}
