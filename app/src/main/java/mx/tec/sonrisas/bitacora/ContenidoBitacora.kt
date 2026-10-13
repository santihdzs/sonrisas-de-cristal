package mx.tec.sonrisas.bitacora

import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.ControlEsfinteres
import mx.tec.sonrisas.domain.EstadoAnimo
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Porcion
import mx.tec.sonrisas.domain.ResumenDia
import mx.tec.sonrisas.domain.SiNoNoAplica
import mx.tec.sonrisas.domain.TiempoComida
import mx.tec.sonrisas.domain.TipoIncidente
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

const val SIN_REGISTRO = "Sin registro"
const val NO_ASISTIO = "No asistió"
const val PENDIENTE = "Pendiente"

private val mx = Locale.forLanguageTag("es-MX")
private val hora = DateTimeFormatter.ofPattern("H:mm", mx)
private val diaColumna = DateTimeFormatter.ofPattern("EEEE d/MM/yyyy", mx)
private val diaLargo = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", mx)

// los accidentes y los problemas de salud van en secciones distintas del formato oficial
private val tiposSalud = setOf(TipoIncidente.REACCION_ALERGICA, TipoIncidente.FIEBRE, TipoIncidente.MALESTAR)

/** lo que se registro de un nino en una fecha; asistencia nula y fecha pasada es "No asistio" */
data class DiaBitacora(
    val fecha: LocalDate,
    val asistencia: Asistencia?,
    val eventos: List<EventoBitacora>,
    val incidentes: List<Incidente>,
)

data class Contexto(
    val estancia: String,
    val sala: String,
    val asistente: String,
    val personal: Map<String, String>,
    val hoy: LocalDate,
)

private fun siNo(v: Boolean?) = when (v) {
    true -> "Sí"
    false -> "No"
    null -> SIN_REGISTRO
}

private fun TiempoComida.nombre() = when (this) {
    TiempoComida.DESAYUNO -> "Desayuno"
    TiempoComida.COLACION -> "Colación"
    TiempoComida.COMIDA -> "Comida"
}

private fun Porcion.nombre() = when (this) {
    Porcion.NADA -> "Nada"
    Porcion.POCO -> "Poco"
    Porcion.TODO -> "Todo"
    Porcion.PIDIO_MAS -> "Pidió más"
}

private fun TipoIncidente.nombre() = when (this) {
    TipoIncidente.GOLPE -> "Golpe"
    TipoIncidente.CAIDA -> "Caída"
    TipoIncidente.MORDIDA -> "Mordida"
    TipoIncidente.RASGUNO -> "Rasguño"
    TipoIncidente.REACCION_ALERGICA -> "Reacción alérgica"
    TipoIncidente.FIEBRE -> "Fiebre"
    TipoIncidente.MALESTAR -> "Malestar"
    TipoIncidente.OTRO -> "Otro"
}

private fun EstadoAnimo.nombre() = when (this) {
    EstadoAnimo.FELIZ -> "Feliz"
    EstadoAnimo.TRISTE -> "Triste"
    EstadoAnimo.ENOJADO -> "Enojado"
}

private fun String.capital() = replaceFirstChar { it.titlecase(mx) }

object ArmadoBitacora {

    fun columna(fecha: LocalDate): String = fecha.format(diaColumna).capital()

    fun semanal(nino: Nino, dias: List<DiaBitacora>, ctx: Contexto): ContenidoBitacora {
        val lunes = dias.first().fecha
        val viernes = dias.last().fecha
        return armar(
            nino, dias, ctx,
            titulo = "Bitácora de filtro diario · Semana del ${lunes.format(diaLargo)} al ${viernes.format(diaLargo)}",
        )
    }

    fun diaria(nino: Nino, dia: DiaBitacora, ctx: Contexto): ContenidoBitacora =
        armar(nino, listOf(dia), ctx, titulo = "Bitácora de filtro diario · ${dia.fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", mx)).capital()}")

    private fun armar(nino: Nino, dias: List<DiaBitacora>, ctx: Contexto, titulo: String): ContenidoBitacora {
        // cada valor depende de si el nino asistio; las filas se arman igual para un dia o para cinco
        fun fila(etiqueta: String, valor: (DiaBitacora, Asistencia, ResumenDia) -> String) = Fila(
            etiqueta,
            dias.map { d ->
                val a = d.asistencia
                when {
                    a == null && d.fecha.isBefore(ctx.hoy) -> NO_ASISTIO
                    a == null -> PENDIENTE
                    else -> valor(d, a, ResumenDia.de(a, d.eventos, d.incidentes)).ifBlank { SIN_REGISTRO }
                }
            },
        )
        fun persona(id: String?) = nino.persona(id)?.let { "${it.nombre} (${it.parentesco.lowercase()})" } ?: SIN_REGISTRO
        fun personal(id: String?) = ctx.personal[id] ?: SIN_REGISTRO
        fun incidentes(d: DiaBitacora, salud: Boolean) = d.incidentes.filter { (it.tipo in tiposSalud) == salud }.sortedBy { it.cuando }

        val ingreso = Seccion(
            "II. Registro de ingreso",
            listOf(
                fila("Hora de entrada") { _, a, _ -> "${a.horaEntrada.format(hora)} hrs" },
                fila("Quién entrega") { _, a, _ -> persona(a.entregadoPorId) },
                fila("Personal que recibe") { _, a, _ -> personal(a.recibidoPorId) },
                fila("Ingresa con síntomas de enfermedad") { _, a, _ -> a.sintomas?.let { "Sí: $it" } ?: "No" },
                fila("Medicamento con receta y especificaciones") { _, a, _ ->
                    when (a.medicamentoConReceta) {
                        SiNoNoAplica.SI -> "Sí"
                        SiNoNoAplica.NO -> "No"
                        SiNoNoAplica.NO_APLICA -> "No aplica"
                    }
                },
                fila("Se presentó limpio") { _, a, _ -> siNo(a.llegoLimpio) },
                fila("Trajo su mochila completa") { _, a, _ -> siNo(a.mochilaCompleta) },
                fila("Buen estado físico") { _, a, _ -> if (a.buenEstadoEntrada) "Sí" else "No: ${a.lesionEntrada ?: SIN_REGISTRO}" },
            ),
        )
        val alimentacion = Seccion(
            "III. Registro de estadía · Alimentación",
            TiempoComida.entries.map { t -> fila(t.nombre()) { _, _, r -> r.comidas[t]?.nombre() ?: SIN_REGISTRO } },
        )
        val descanso = Seccion(
            "III. Descanso",
            listOf(
                fila("¿Durmió?") { _, _, r -> if (r.siestas.isEmpty()) "No" else "Sí" },
                fila("Tiempo de siesta") { _, _, r ->
                    if (r.siestas.isEmpty()) SIN_REGISTRO
                    else r.siestas.joinToString("; ") { s ->
                        s.fin?.let { fin -> "${s.minutos} min (${s.inicio.format(hora)} a ${fin.format(hora)})" } ?: "Desde ${s.inicio.format(hora)}"
                    }
                },
            ),
        )
        val excretoras = Seccion(
            "III. Funciones excretoras",
            listOf(
                fila("Control de esfínteres") { d, _, _ ->
                    val ultimo = d.eventos.filterIsInstance<EventoBitacora.Evacuacion>().maxByOrNull { it.cuando }
                    when (ultimo?.control) {
                        ControlEsfinteres.AVISO -> "Avisó"
                        ControlEsfinteres.NO_AVISO -> "No avisó"
                        ControlEsfinteres.NO_APLICA -> "No aplica"
                        null -> SIN_REGISTRO
                    }
                },
                fila("Número de veces pipí") { _, _, r -> "${r.pipi}" },
                fila("Número de veces popó") { _, _, r -> "${r.popo}" },
            ),
        )
        val animo = Seccion(
            "III. Estado de ánimo",
            listOf(
                fila("Durante su estadía estuvo") { _, _, r -> r.ultimoAnimo?.estado?.nombre() ?: SIN_REGISTRO },
                fila("¿Lloró?") { _, _, r -> siNo(r.ultimoAnimo?.lloro) },
                fila("¿Peleó?") { _, _, r -> siNo(r.ultimoAnimo?.peleo) },
                fila("¿Participó en las actividades?") { _, _, r -> siNo(r.ultimoAnimo?.participo) },
            ),
        )
        val accidentes = Seccion(
            "III. Accidentes",
            listOf(
                fila("¿Tuvo algún accidente?") { d, _, _ -> if (incidentes(d, false).isEmpty()) "No" else "Sí" },
                fila("Descripción breve") { d, _, _ ->
                    incidentes(d, false).joinToString("; ") { i ->
                        "${i.cuando.format(hora)} ${i.tipo.nombre()}${i.parteDelCuerpo?.let { p -> " en $p" }.orEmpty()}: ${i.descripcion}"
                    }.ifBlank { "No aplica" }
                },
                fila("Folio de reporte") { d, _, _ -> incidentes(d, false).joinToString(", ") { it.folio }.ifBlank { "No aplica" } },
                fila("Atención proporcionada") { d, _, _ -> incidentes(d, false).joinToString("; ") { it.accionTomada }.ifBlank { "No aplica" } },
            ),
        )
        val salud = Seccion(
            "III. Salud",
            listOf(
                fila("¿Presentó algún problema de salud?") { d, _, _ -> if (incidentes(d, true).isEmpty()) "No" else "Sí" },
                fila("¿Cuál?") { d, _, _ ->
                    incidentes(d, true).joinToString("; ") { "${it.folio} ${it.tipo.nombre()}: ${it.descripcion}" }.ifBlank { "No aplica" }
                },
                fila("Atención proporcionada") { d, _, _ -> incidentes(d, true).joinToString("; ") { it.accionTomada }.ifBlank { "No aplica" } },
                fila("Observaciones generales") { _, _, r -> r.notas.joinToString(" ") { it.texto }.ifBlank { SIN_REGISTRO } },
            ),
        )
        val egreso = Seccion(
            "IV. Registro de egreso",
            listOf(
                fila("Hora de salida") { _, a, _ -> a.horaSalida?.let { "${it.format(hora)} hrs" } ?: SIN_REGISTRO },
                fila("Quién recibe") { _, a, _ -> if (a.horaSalida == null) SIN_REGISTRO else persona(a.recogidoPorId) },
                fila("Personal que entrega") { _, a, _ -> if (a.horaSalida == null) SIN_REGISTRO else personal(a.entregoSalidaId) },
                fila("Se entrega en buen estado físico") { _, a, _ ->
                    when (a.buenEstadoSalida) {
                        null -> SIN_REGISTRO
                        true -> "Sí"
                        false -> "No: ${a.lesionSalida ?: SIN_REGISTRO}"
                    }
                },
                fila("Se entregó limpio") { _, a, _ -> siNo(a.salioLimpio) },
                fila("Pertenencias completas") { _, a, _ -> siNo(a.pertenenciasCompletas) },
                fila("Observaciones") { _, a, _ -> a.observacionesSalida ?: SIN_REGISTRO },
            ),
        )
        return ContenidoBitacora(
            titulo = titulo,
            datosGenerales = listOf(
                "Estancia infantil" to ctx.estancia,
                "Nombre completo de la niña o niño" to nino.nombreCompleto,
                "Grupo" to ctx.sala,
                "Fecha de nacimiento" to nino.fechaNacimiento.format(diaLargo),
                "Asistente al cuidado" to ctx.asistente,
                "Alergias y padecimientos" to listOf(nino.alergias, nino.padecimientos).filter { it.isNotBlank() }.joinToString("; ").ifBlank { "Ninguno registrado" },
            ),
            columnas = dias.map { columna(it.fecha) },
            secciones = listOf(ingreso, alimentacion, descanso, excretoras, animo, accidentes, salud, egreso),
            pie = "${nino.nombreCompleto} · Sala ${ctx.sala}",
        )
    }
}
