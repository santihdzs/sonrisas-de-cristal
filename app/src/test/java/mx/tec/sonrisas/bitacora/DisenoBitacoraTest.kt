package mx.tec.sonrisas.bitacora

import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.PersonaAutorizada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ancho fijo por caracter: suficiente para probar la paginacion sin android
private val medidor = Medidor { texto, tamano, negrita -> texto.length * tamano * (if (negrita) 0.55f else 0.5f) }

private fun textos(p: Pagina) = p.elementos.filterIsInstance<Elemento.Texto>()

private fun contenido(filas: Int, valor: String = "Sí", columnas: Int = 5) = ContenidoBitacora(
    titulo = "Bitácora de filtro diario",
    datosGenerales = listOf("Estancia infantil" to "San Bernabé", "Grupo" to "Maternales"),
    columnas = List(columnas) { "Día $it" },
    secciones = listOf(Seccion("II. Registro de ingreso", List(filas) { Fila("Fila $it", List(columnas) { valor }) })),
    pie = "Sofía Ramírez Treviño",
)

class DisenoBitacoraTest {

    private val diseno = DisenoBitacora(Orientacion.HORIZONTAL, medidor)

    @Test
    fun contenidoCortoCabeEnUnaPagina() {
        val paginas = diseno.paginar(contenido(5), "12/10/2026 9:00")
        assertEquals(1, paginas.size)
        assertTrue(textos(paginas[0]).any { it.texto.endsWith("Página 1 de 1") })
    }

    @Test
    fun muchasFilasContinuanEnOtraPaginaConEncabezados() {
        val paginas = diseno.paginar(contenido(80), "12/10/2026 9:00")
        assertTrue(paginas.size > 1)
        paginas.forEach { p ->
            // cada pagina repite la fila de fechas y numera el total
            assertTrue(textos(p).any { it.texto == "Fecha" })
            assertTrue(textos(p).any { it.texto.contains("Página") && it.texto.endsWith("de ${paginas.size}") })
        }
        val filas = paginas.flatMap { textos(it) }.filter { it.texto.startsWith("Fila ") }.map { it.texto }
        assertEquals(List(80) { "Fila $it" }, filas)
    }

    @Test
    fun nadaSeSaleDeLaPaginaNiBajaDelTamanoMinimo() {
        val largo = List(60) { "palabra$it" }.joinToString(" ")
        val etiquetaLarga = contenido(60, largo).let { c ->
            c.copy(secciones = c.secciones.map { s -> s.copy(filas = s.filas.map { f -> f.copy(etiqueta = f.etiqueta + " " + "muy larga ".repeat(30)) }) })
        }
        val paginas = diseno.paginar(etiquetaLarga, "12/10/2026 9:00")
        assertTrue(paginas.any { p -> textos(p).any { it.texto.startsWith("Observaciones") } })
        paginas.forEach { p ->
            textos(p).forEach { t ->
                assertTrue(t.tamano >= Tipografia.MINIMO)
                assertTrue("texto fuera: ${t.texto} y=${t.y}", t.y <= p.alto - 20f && t.y > 0f)
                assertTrue(t.x + medidor.ancho(t.texto, t.tamano, t.negrita) <= p.ancho - 31f)
            }
        }
    }

    @Test
    fun notaLargaVaAlAnexoCompletaYConReferencia() {
        val nota = List(120) { "texto$it" }.joinToString(" ")
        val paginas = diseno.paginar(contenido(3, nota, columnas = 5), "12/10/2026 9:00")
        // la referencia puede ocupar dos renglones de la celda
        val todos = paginas.flatMap { textos(it) }.joinToString(" ") { it.texto }
        assertTrue(todos.contains("(sigue en Observaciones, nota 1)"))
        val anexo = paginas.filter { p -> textos(p).any { it.texto.startsWith("Observaciones") } }
        assertTrue(anexo.isNotEmpty())
        val enAnexo = anexo.flatMap { textos(it) }.joinToString(" ") { it.texto }
        // ninguna palabra de la nota se pierde
        (0 until 120).forEach { assertTrue("falta texto$it", enAnexo.contains("texto$it")) }
        assertTrue(enAnexo.contains("Nota 1 · Día 0"))
    }

    @Test
    fun mismoContenidoMismasPaginas() {
        val c = contenido(40, "Laura Treviño Garza (mamá)")
        assertEquals(diseno.paginar(c, "12/10/2026 9:00"), diseno.paginar(c, "12/10/2026 9:00"))
    }

    @Test
    fun envolverRespetaElAncho() {
        val lineas = envolver("una frase bastante larga con unapalabraenormesinespaciosquenocabe", 60f, 7.5f, false, medidor)
        lineas.forEach { assertTrue(medidor.ancho(it, 7.5f, false) <= 60f) }
        assertEquals("una frase bastante larga con unapalabraenormesinespaciosquenocabe".replace(" ", ""), lineas.joinToString("").replace(" ", ""))
    }
}

class ArmadoBitacoraTest {
    private val lunes = LocalDate.of(2026, 10, 5)
    private val mama = PersonaAutorizada("p", "Laura Treviño Garza", "Mamá", "8100000000")
    private val nino = Nino("n", "Sofía", "Ramírez Treviño", LocalDate.of(2024, 5, 1), "e", "s", listOf("p"), listOf(mama))
    private val ctx = Contexto("Estancia San Bernabé", "Maternales", "Mariana López", mapOf("u" to "Mariana López"), hoy = lunes.plusDays(3))

    private fun valores(c: ContenidoBitacora, etiqueta: String) = c.secciones.flatMap { it.filas }.first { it.etiqueta == etiqueta }.valores

    @Test
    fun diasSinAsistenciaYCamposVacios() {
        val asistencia = Asistencia("a", "n", lunes, LocalTime.of(8, 5), "p", "u")
        val nota = EventoBitacora.Nota("e", "n", LocalDateTime.of(lunes, LocalTime.of(14, 0)), "u", "Comió muy bien.")
        val dias = (0L..4L).map { lunes.plusDays(it) }.map { f ->
            if (f == lunes) DiaBitacora(f, asistencia, listOf(nota), emptyList()) else DiaBitacora(f, null, emptyList(), emptyList())
        }
        val c = ArmadoBitacora.semanal(nino, dias, ctx)
        assertEquals(listOf("8:05 hrs", NO_ASISTIO, NO_ASISTIO, PENDIENTE, PENDIENTE), valores(c, "Hora de entrada"))
        assertEquals("Laura Treviño Garza (mamá)", valores(c, "Quién entrega")[0])
        assertEquals(SIN_REGISTRO, valores(c, "Desayuno")[0])
        assertEquals("Comió muy bien.", valores(c, "Observaciones generales")[0])
        assertEquals(SIN_REGISTRO, valores(c, "Hora de salida")[0])
        // el orden de secciones sigue al formato oficial
        assertTrue(c.secciones.first().titulo.startsWith("II."))
        assertTrue(c.secciones.last().titulo.startsWith("IV."))
    }
}
