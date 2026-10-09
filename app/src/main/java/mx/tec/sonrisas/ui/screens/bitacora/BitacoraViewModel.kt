package mx.tec.sonrisas.ui.screens.bitacora

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.tec.sonrisas.bitacora.ArmadoBitacora
import mx.tec.sonrisas.bitacora.Contexto
import mx.tec.sonrisas.bitacora.DiaBitacora
import mx.tec.sonrisas.bitacora.DisenoBitacora
import mx.tec.sonrisas.bitacora.MedidorAndroid
import mx.tec.sonrisas.bitacora.Orientacion
import mx.tec.sonrisas.bitacora.Pagina
import mx.tec.sonrisas.bitacora.PdfBitacora
import mx.tec.sonrisas.data.EstanciasRepository
import mx.tec.sonrisas.data.IncidentesRepository
import mx.tec.sonrisas.data.JornadaRepository
import mx.tec.sonrisas.data.NinosRepository
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.lunesDeLaSemana
import java.io.File
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed interface Objetivo {
    data class UnNino(val ninoId: String, val nombre: String) : Objetivo
    data class Sala(val salaId: String, val nombre: String) : Objetivo
}

enum class FormatoBitacora(val etiqueta: String) {
    SEMANAL("Semana, carta horizontal"),
    DIARIA("Un día, carta vertical"),
}

enum class Salida { IMPRIMIR, COMPARTIR }

data class HojaBitacora(
    val objetivo: Objetivo,
    val formato: FormatoBitacora = FormatoBitacora.SEMANAL,
    val fecha: LocalDate,
    val generando: Salida? = null,
    val error: String? = null,
)

data class PdfListo(val archivo: File, val nombre: String, val orientacion: Orientacion, val salida: Salida)

class BitacoraViewModel(
    private val estanciasRepository: EstanciasRepository,
    private val ninosRepository: NinosRepository,
    private val jornadaRepository: JornadaRepository,
    private val incidentesRepository: IncidentesRepository,
    private val carpeta: File,
    private val reloj: Reloj,
) : ViewModel() {

    var hoja by mutableStateOf<HojaBitacora?>(null)
        private set

    fun abrir(objetivo: Objetivo) {
        hoja = HojaBitacora(objetivo, fecha = reloj.hoy())
    }

    fun cambiar(nueva: HojaBitacora) {
        hoja = nueva.copy(error = null)
    }

    fun cerrar() {
        hoja = null
    }

    fun generar(salida: Salida, onListo: (PdfListo) -> Unit) {
        val h = hoja ?: return
        if (h.generando != null) return
        val formato = if (h.objetivo is Objetivo.Sala) FormatoBitacora.SEMANAL else h.formato
        if (h.fecha.isAfter(reloj.hoy())) {
            hoja = h.copy(error = "Elige hoy o una fecha anterior.")
            return
        }
        hoja = h.copy(generando = salida)
        viewModelScope.launch {
            try {
                val listo = withContext(Dispatchers.Default) { crear(h.objetivo, formato, h.fecha, salida) }
                hoja = null
                onListo(listo)
            } catch (e: SinNinos) {
                hoja = h.copy(generando = null, error = "No hay niños activos para imprimir en esta sala.")
            } catch (e: Exception) {
                hoja = h.copy(generando = null, error = "No pudimos generar la bitácora. Inténtalo de nuevo.")
            }
        }
    }

    private class SinNinos : Exception()

    private suspend fun crear(objetivo: Objetivo, formato: FormatoBitacora, fecha: LocalDate, salida: Salida): PdfListo {
        val hoy = reloj.hoy()
        // todo sale de los repositorios, que ya filtran por sesion: nadie imprime a un nino que no puede ver
        val todos = ninosRepository.ninos().first()
        val ninos: List<Nino> = when (objetivo) {
            is Objetivo.UnNino -> listOfNotNull(todos.firstOrNull { it.id == objetivo.ninoId })
            is Objetivo.Sala -> todos.filter { it.salaId == objetivo.salaId && it.activo }.sortedBy { it.nombreCompleto }
        }
        if (ninos.isEmpty()) throw SinNinos()
        val salas = estanciasRepository.salas().first().associateBy { it.id }
        val estancias = estanciasRepository.estancias().first().associateBy { it.id }
        val personal = estanciasRepository.personal().first()
        val nombres = personal.associate { it.id to it.nombre }
        val fechas = if (formato == FormatoBitacora.SEMANAL) (0L..4L).map { fecha.lunesDeLaSemana().plusDays(it) } else listOf(fecha)
        val orientacion = if (formato == FormatoBitacora.SEMANAL) Orientacion.HORIZONTAL else Orientacion.VERTICAL
        val diseno = DisenoBitacora(orientacion, MedidorAndroid)
        val generado = reloj.ahora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm"))

        val paginas = mutableListOf<Pagina>()
        ninos.forEach { n ->
            val desde = fechas.first()
            val hasta = fechas.last()
            val asistencias = jornadaRepository.asistenciasDe(n.id, desde, hasta).first()
            val eventos = jornadaRepository.eventosDe(n.id, desde, hasta).first()
            val incidentes = incidentesRepository.incidentesDe(n.id, desde, hasta).first()
            val dias = fechas.map { f ->
                DiaBitacora(
                    f,
                    asistencias.firstOrNull { it.fecha == f },
                    eventos.filter { it.cuando.toLocalDate() == f },
                    incidentes.filter { it.cuando.toLocalDate() == f },
                )
            }
            val sala = salas[n.salaId]
            val ctx = Contexto(
                estancia = estancias[n.estanciaId]?.nombre.orEmpty(),
                sala = sala?.nombre.orEmpty(),
                asistente = personal.filter { it.rol == Rol.EDUCADORA && it.salaId == n.salaId && it.activo }.joinToString(", ") { it.nombre }.ifBlank { "Sin asignar" },
                personal = nombres,
                hoy = hoy,
            )
            val contenido = if (formato == FormatoBitacora.SEMANAL) ArmadoBitacora.semanal(n, dias, ctx) else ArmadoBitacora.diaria(n, dias.first(), ctx)
            // cada nino empieza en su propia pagina
            paginas += diseno.paginar(contenido, generado)
        }

        val base = when (objetivo) {
            is Objetivo.UnNino -> objetivo.nombre
            is Objetivo.Sala -> "sala ${objetivo.nombre}"
        }
        val periodo = if (formato == FormatoBitacora.SEMANAL) "semana ${fechas.first()}" else "${fechas.first()}"
        val nombre = "Bitácora $base $periodo"
        val archivo = File(carpeta, "${limpio(nombre)}.pdf")
        withContext(Dispatchers.IO) {
            carpeta.mkdirs()
            PdfBitacora.escribir(paginas, archivo)
        }
        val accion = if (salida == Salida.IMPRIMIR) "Imprimió" else "Compartió en PDF"
        val tipo = if (formato == FormatoBitacora.SEMANAL) "semanal" else "diaria"
        val de = if (objetivo is Objetivo.Sala) "de la sala ${objetivo.nombre} (${ninos.size} niños)" else "de ${ninos.first().nombreCompleto}"
        val desdeTexto = if (formato == FormatoBitacora.SEMANAL) "la semana del ${fechas.first().format(DateTimeFormatter.ofPattern("d/MM/yyyy"))}" else "el ${fechas.first().format(DateTimeFormatter.ofPattern("d/MM/yyyy"))}"
        jornadaRepository.registrarExportacion(ninos.map { it.id }, "$accion la bitácora $tipo $de, $desdeTexto")
        return PdfListo(archivo, nombre, orientacion, salida)
    }

    private fun limpio(nombre: String): String =
        Normalizer.normalize(nombre.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
