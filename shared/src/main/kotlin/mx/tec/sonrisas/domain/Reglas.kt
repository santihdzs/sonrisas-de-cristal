@file:UseSerializers(MesSerializer::class)

package mx.tec.sonrisas.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

// periodo de gracia antes de borrar un expediente dado de baja.
// la fundacion todavia tiene que definir su duracion (RF-18), 30 dias es provisional.
const val DIAS_PERIODO_BAJA = 30L

fun Nino.puedeAparecerEn(uso: UsoFoto): Boolean = when (uso) {
    UsoFoto.INTERNA -> permisoImagenInterna
    UsoFoto.REDES -> permisoImagenInterna && permisoImagenRedes
}

fun folioIncidente(numero: Int): String = "INC-%04d".format(numero)

fun tiempoComidaSugerido(hora: LocalTime): TiempoComida = when {
    hora.isBefore(LocalTime.of(9, 30)) -> TiempoComida.DESAYUNO
    hora.isBefore(LocalTime.of(12, 0)) -> TiempoComida.COLACION
    else -> TiempoComida.COMIDA
}

fun LocalDate.esDiaHabil(): Boolean = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY

fun LocalDate.lunesDeLaSemana(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

data class ResumenDia(
    val asistencia: Asistencia?,
    val comidas: Map<TiempoComida, Porcion>,
    val siestas: List<EventoBitacora.Siesta>,
    val minutosSiesta: Int,
    val pipi: Int,
    val popo: Int,
    val ultimoAnimo: EventoBitacora.Animo?,
    val notas: List<EventoBitacora.Nota>,
    val incidentes: List<Incidente>,
) {
    val asistio: Boolean get() = asistencia != null
    val sinRegistros: Boolean
        get() = comidas.isEmpty() && siestas.isEmpty() && pipi == 0 && popo == 0 && ultimoAnimo == null && notas.isEmpty()

    companion object {
        fun de(asistencia: Asistencia?, eventos: List<EventoBitacora>, incidentes: List<Incidente>): ResumenDia {
            val ordenados = eventos.sortedBy { it.cuando }
            val alimentos = ordenados.filterIsInstance<EventoBitacora.Alimento>()
            val siestas = ordenados.filterIsInstance<EventoBitacora.Siesta>()
            val evacuaciones = ordenados.filterIsInstance<EventoBitacora.Evacuacion>()
            return ResumenDia(
                asistencia = asistencia,
                // si hay dos registros del mismo tiempo, cuenta el ultimo
                comidas = alimentos.associate { it.tiempo to it.porcion },
                siestas = siestas,
                minutosSiesta = siestas.sumOf { it.minutos },
                pipi = evacuaciones.count { it.tipo == TipoEvacuacion.PIPI || it.tipo == TipoEvacuacion.AMBOS },
                popo = evacuaciones.count { it.tipo == TipoEvacuacion.POPO || it.tipo == TipoEvacuacion.AMBOS },
                ultimoAnimo = ordenados.filterIsInstance<EventoBitacora.Animo>().lastOrNull(),
                notas = ordenados.filterIsInstance<EventoBitacora.Nota>(),
                incidentes = incidentes.sortedBy { it.cuando },
            )
        }
    }
}

sealed interface DiaSemana {
    val fecha: LocalDate

    data class Pendiente(override val fecha: LocalDate) : DiaSemana
    data class NoAsistio(override val fecha: LocalDate) : DiaSemana
    data class Asistio(override val fecha: LocalDate, val resumen: ResumenDia) : DiaSemana
}

data class ResumenSemana(val nombreNino: String, val lunes: LocalDate, val dias: List<DiaSemana>) {

    fun comoTexto(): String {
        val fmtDia = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_MX)
        val fmtHora = DateTimeFormatter.ofPattern("H:mm", LOCALE_MX)
        return buildString {
            appendLine("Resumen de la semana de $nombreNino")
            appendLine("Semana del ${lunes.format(DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_MX))}")
            appendLine()
            dias.forEach { dia ->
                appendLine(dia.fecha.format(fmtDia).replaceFirstChar { it.titlecase(LOCALE_MX) })
                when (dia) {
                    is DiaSemana.Pendiente -> appendLine("  Pendiente")
                    is DiaSemana.NoAsistio -> appendLine("  No asistió")
                    is DiaSemana.Asistio -> {
                        val r = dia.resumen
                        val a = r.asistencia
                        if (a != null) {
                            val salida = a.horaSalida?.let { " a ${it.format(fmtHora)}" } ?: ""
                            appendLine("  Asistencia: ${a.horaEntrada.format(fmtHora)}$salida")
                        }
                        if (r.comidas.isNotEmpty()) {
                            appendLine("  Comidas: " + TiempoComida.entries.filter { it in r.comidas }
                                .joinToString(", ") { "${nombreTiempo(it)} ${nombrePorcion(r.comidas.getValue(it))}" })
                        }
                        if (r.minutosSiesta > 0) appendLine("  Siesta: ${r.minutosSiesta} min")
                        r.ultimoAnimo?.let { appendLine("  Ánimo: ${nombreAnimo(it.estado)}") }
                        r.incidentes.forEach { appendLine("  Incidente: ${it.descripcion}") }
                        r.notas.forEach { appendLine("  Nota: ${it.texto}") }
                    }
                }
            }
            appendLine()
            append("Enviado desde la app de Sonrisas de Cristal")
        }
    }

    companion object {
        fun de(
            nombreNino: String,
            hoy: LocalDate,
            asistencias: List<Asistencia>,
            eventos: List<EventoBitacora>,
            incidentes: List<Incidente>,
        ): ResumenSemana {
            val lunes = hoy.lunesDeLaSemana()
            val dias = (0L..4L).map { lunes.plusDays(it) }.map { fecha ->
                val asistencia = asistencias.firstOrNull { it.fecha == fecha }
                when {
                    fecha.isAfter(hoy) -> DiaSemana.Pendiente(fecha)
                    asistencia == null && fecha == hoy -> DiaSemana.Pendiente(fecha)
                    asistencia == null -> DiaSemana.NoAsistio(fecha)
                    else -> DiaSemana.Asistio(
                        fecha,
                        ResumenDia.de(
                            asistencia,
                            eventos.filter { it.cuando.toLocalDate() == fecha },
                            incidentes.filter { it.cuando.toLocalDate() == fecha },
                        ),
                    )
                }
            }
            return ResumenSemana(nombreNino, lunes, dias)
        }
    }
}

sealed interface ActividadActual {
    data class Especial(val actividad: ActividadEspecial) : ActividadActual
    data class Regular(val bloque: BloqueHorario) : ActividadActual
    data object FueraDeHorario : ActividadActual

    companion object {
        fun de(bloques: List<BloqueHorario>, especiales: List<ActividadEspecial>, ahora: LocalDateTime): ActividadActual {
            val hora = ahora.toLocalTime()
            val especial = especiales.firstOrNull {
                it.fecha == ahora.toLocalDate() && !hora.isBefore(it.inicio) && hora.isBefore(it.fin)
            }
            if (especial != null) return Especial(especial)
            val bloque = bloques.firstOrNull { !hora.isBefore(it.inicio) && hora.isBefore(it.fin) }
            return if (bloque != null) Regular(bloque) else FueraDeHorario
        }
    }
}

@Serializable
data class FilaReporte(val ninoId: String, val nombre: String, val salaId: String, val asistidos: Int, val habiles: Int) {
    val porcentaje: Int get() = if (habiles == 0) 0 else (asistidos * 100.0 / habiles).roundToInt()
}

@Serializable
data class FilaSala(val salaId: String, val nombre: String, val inscritos: Int, val promedio: Int)

@Serializable
data class ReporteAsistencia(
    val mes: YearMonth,
    val diasHabiles: Int,
    val filas: List<FilaReporte>,
    val salas: List<FilaSala>,
    // se guarda aparte para poder quitar las filas por nino sin perder el promedio
    val promedio: Int = if (filas.isEmpty()) 0 else filas.map { it.porcentaje }.average().roundToInt(),
) {

    fun comoTexto(titulo: String): String = buildString {
        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_MX)
        appendLine("Reporte de asistencia: $titulo")
        appendLine("Mes: ${mes.format(fmt)}")
        appendLine("Días hábiles: $diasHabiles")
        appendLine("Asistencia promedio: $promedio %")
        appendLine()
        if (salas.size > 1) {
            appendLine("Por sala")
            salas.forEach { appendLine("  ${it.nombre}: ${it.inscritos} niños, ${it.promedio} %") }
            appendLine()
        }
        if (filas.isNotEmpty()) appendLine("Por niño")
        filas.forEach { appendLine("  ${it.nombre}: ${it.asistidos} de ${it.habiles} días (${it.porcentaje} %)") }
    }

    companion object {
        fun diasHabiles(mes: YearMonth, hoy: LocalDate): List<LocalDate> {
            val ultimo = minOf(mes.atEndOfMonth(), hoy)
            if (ultimo.isBefore(mes.atDay(1))) return emptyList()
            return generateSequence(mes.atDay(1)) { it.plusDays(1) }
                .takeWhile { !it.isAfter(ultimo) }
                .filter { it.esDiaHabil() }
                .toList()
        }

        fun de(
            mes: YearMonth,
            hoy: LocalDate,
            ninos: List<Nino>,
            salas: List<Sala>,
            asistencias: List<Asistencia>,
        ): ReporteAsistencia {
            val habiles = diasHabiles(mes, hoy).toSet()
            val porNino = asistencias.filter { it.fecha in habiles }.groupBy { it.ninoId }
            val filas = ninos.map { n ->
                FilaReporte(n.id, n.nombreCompleto, n.salaId, porNino[n.id]?.map { it.fecha }?.distinct()?.size ?: 0, habiles.size)
            }.sortedBy { it.nombre }
            val filasSala = salas.map { s ->
                val deSala = filas.filter { it.salaId == s.id }
                FilaSala(
                    s.id,
                    s.nombre,
                    deSala.size,
                    if (deSala.isEmpty()) 0 else deSala.map { it.porcentaje }.average().roundToInt(),
                )
            }
            return ReporteAsistencia(mes, habiles.size, filas, filasSala)
        }
    }
}

object Validaciones {
    private val soloDigitos = Regex("\\d{10}")

    fun requerido(texto: String, campo: String): String? =
        if (texto.isBlank()) "Escribe $campo." else null

    fun telefono(texto: String): String? {
        val limpio = texto.filter { it.isDigit() }
        return when {
            texto.isBlank() -> "Escribe el teléfono."
            !soloDigitos.matches(limpio) -> "El teléfono debe tener 10 dígitos."
            else -> null
        }
    }

    fun fechaNacimiento(fecha: LocalDate?, hoy: LocalDate): String? = when {
        fecha == null -> "Elige la fecha de nacimiento."
        fecha.isAfter(hoy) -> "La fecha no puede ser futura."
        fecha.isBefore(hoy.minusYears(6)) -> "Revisa la fecha: la estancia atiende hasta los 4 años."
        else -> null
    }

    fun incidente(descripcion: String, accion: String): Map<String, String> = buildMap {
        requerido(descripcion, "qué pasó")?.let { put("descripcion", it) }
        requerido(accion, "qué hicieron")?.let { put("accion", it) }
    }

    fun aviso(titulo: String, cuerpo: String): Map<String, String> = buildMap {
        requerido(titulo, "un título")?.let { put("titulo", it) }
        if (titulo.length > 80) put("titulo", "Usa un título de 80 caracteres o menos.")
        requerido(cuerpo, "el mensaje")?.let { put("cuerpo", it) }
    }

    fun estancia(nombre: String, colonia: String, telefono: String): Map<String, String> = buildMap {
        requerido(nombre, "el nombre de la estancia")?.let { put("nombre", it) }
        requerido(colonia, "la colonia o dirección")?.let { put("colonia", it) }
        telefono(telefono)?.let { put("telefono", it) }
    }

    /** el color de estrella identifica a la sala, asi que no se repite dentro de la estancia */
    fun sala(nombre: String, estrella: ColorEstrella?, otras: List<Sala>): Map<String, String> = buildMap {
        requerido(nombre, "el nombre de la sala")?.let { put("nombre", it) }
        if (otras.any { it.nombre.equals(nombre.trim(), ignoreCase = true) }) put("nombre", "Ya hay una sala con ese nombre.")
        when {
            estrella == null -> put("estrella", "Elige el color de la estrella.")
            otras.any { it.estrella == estrella } -> put("estrella", "Otra sala ya usa ese color. Elige uno libre.")
        }
    }

    fun bloque(inicio: LocalTime, fin: LocalTime, actividad: String, otros: List<BloqueHorario>): String? = when {
        actividad.isBlank() -> "Escribe la actividad."
        !fin.isAfter(inicio) -> "La hora de fin debe ser después del inicio."
        otros.any { inicio.isBefore(it.fin) && it.inicio.isBefore(fin) } ->
            "Se empalma con otro bloque. Ajusta las horas."
        else -> null
    }
}

private val marcasAcento = Regex("\\p{Mn}+")

/** nombre.apellido sin acentos; si ya existe se le agrega un numero */
fun usuarioSugerido(nombre: String, ocupados: Collection<String>): String {
    val partes = Normalizer.normalize(nombre.lowercase(LOCALE_MX), Normalizer.Form.NFD)
        .replace(marcasAcento, "")
        .split(Regex("[^a-z]+"))
        .filter { it.isNotBlank() }
    val base = partes.take(2).joinToString(".").ifBlank { "usuario" }
    val usados = ocupados.map { it.lowercase(LOCALE_MX) }.toSet()
    if (base !in usados) return base
    return generateSequence(2) { it + 1 }.map { "$base$it" }.first { it !in usados }
}

private val palabrasContrasena = listOf("luna", "nube", "flor", "sol", "mar", "pino", "lago", "rio")

// facil de dictar en voz alta; la persona la usa para entrar la primera vez
fun contrasenaTemporal(azar: Random = Random.Default): String =
    palabrasContrasena[azar.nextInt(palabrasContrasena.size)] + azar.nextInt(1000, 10000)

// feriados oficiales de la ley federal del trabajo (sin los de cambio de gobierno)
fun siguienteFeriado(desde: LocalDate): Pair<LocalDate, String> {
    fun nesimoLunes(anio: Int, mes: Int, n: Int): LocalDate =
        LocalDate.of(anio, mes, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, DayOfWeek.MONDAY))

    fun delAnio(anio: Int) = listOf(
        LocalDate.of(anio, 1, 1) to "Año Nuevo",
        nesimoLunes(anio, 2, 1) to "Día de la Constitución",
        nesimoLunes(anio, 3, 3) to "Natalicio de Benito Juárez",
        LocalDate.of(anio, 5, 1) to "Día del Trabajo",
        LocalDate.of(anio, 9, 16) to "Día de la Independencia",
        nesimoLunes(anio, 11, 3) to "Día de la Revolución",
        LocalDate.of(anio, 12, 25) to "Navidad",
    )
    return (delAnio(desde.year) + delAnio(desde.year + 1)).first { it.first.isAfter(desde) }
}

private val LOCALE_MX: Locale = Locale.forLanguageTag("es-MX")

private fun nombreTiempo(t: TiempoComida) = when (t) {
    TiempoComida.DESAYUNO -> "desayuno"
    TiempoComida.COLACION -> "colación"
    TiempoComida.COMIDA -> "comida"
}

private fun nombrePorcion(p: Porcion) = when (p) {
    Porcion.NADA -> "nada"
    Porcion.POCO -> "poco"
    Porcion.TODO -> "todo"
    Porcion.PIDIO_MAS -> "pidió más"
}

private fun nombreAnimo(e: EstadoAnimo) = when (e) {
    EstadoAnimo.FELIZ -> "feliz"
    EstadoAnimo.TRISTE -> "triste"
    EstadoAnimo.ENOJADO -> "enojado"
}
