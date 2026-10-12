package mx.tec.sonrisas.ui.components

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocaleMx: Locale = Locale.forLanguageTag("es-MX")

private val formatoHora = DateTimeFormatter.ofPattern("H:mm", LocaleMx)
private val formatoFechaLarga = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LocaleMx)
private val formatoFechaCorta = DateTimeFormatter.ofPattern("d MMM yyyy", LocaleMx)
private val formatoDiaMes = DateTimeFormatter.ofPattern("d 'de' MMMM", LocaleMx)
private val formatoMes = DateTimeFormatter.ofPattern("MMMM yyyy", LocaleMx)
private val formatoDiaSemana = DateTimeFormatter.ofPattern("EEEE", LocaleMx)

private val acentos = Regex("\\p{Mn}+")

/** para buscar "Sofia" y encontrar "Sofía" */
fun String.coincideCon(busqueda: String): Boolean {
    fun normal(t: String) = java.text.Normalizer.normalize(t.lowercase(LocaleMx), java.text.Normalizer.Form.NFD).replace(acentos, "")
    return busqueda.isBlank() || normal(this).contains(normal(busqueda.trim()))
}

fun String.conMayuscula(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(LocaleMx) else it.toString() }

fun LocalTime.formatoHora(): String = format(formatoHora)
fun LocalDateTime.formatoHora(): String = format(formatoHora)
fun LocalDate.formatoLargo(): String = format(formatoFechaLarga).conMayuscula()
fun LocalDate.formatoCorto(): String = format(formatoFechaCorta).replace(".", "")
fun LocalDate.formatoDiaMes(): String = format(formatoDiaMes)
fun LocalDate.formatoMes(): String = format(formatoMes).conMayuscula()
fun LocalDate.nombreDia(): String = format(formatoDiaSemana).conMayuscula()

fun LocalDateTime.formatoFechaHora(): String = "${toLocalDate().formatoDiaMes()}, ${formatoHora()}"

fun LocalDate.relativoA(hoy: LocalDate): String = when (this) {
    hoy -> "Hoy"
    hoy.minusDays(1) -> "Ayer"
    else -> formatoLargo()
}

fun edadTexto(nacimiento: LocalDate, hoy: LocalDate): String {
    val p = Period.between(nacimiento, hoy)
    val meses = p.years * 12 + p.months
    return when {
        meses < 1 -> "Recién nacido"
        meses < 24 -> if (meses == 1) "1 mes" else "$meses meses"
        p.months == 0 -> "${p.years} años"
        else -> "${p.years} años ${p.months} m"
    }
}

fun minutosTexto(minutos: Int): String {
    val h = minutos / 60
    val m = minutos % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

fun iniciales(nombre: String, apellidos: String): String =
    (nombre.take(1) + apellidos.take(1)).uppercase(LocaleMx)

fun primerNombre(nombre: String): String = nombre.substringBefore(" ")
