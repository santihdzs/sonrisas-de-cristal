package mx.tec.sonrisas.ui.screens.bitacora

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.bitacora.Orientacion
import mx.tec.sonrisas.domain.lunesDeLaSemana
import mx.tec.sonrisas.ui.components.BotonPrincipal
import mx.tec.sonrisas.ui.components.CampoFecha
import mx.tec.sonrisas.ui.components.EtiquetaCampo
import mx.tec.sonrisas.ui.components.HojaModal
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.SelectorOpciones
import mx.tec.sonrisas.ui.components.compartirArchivo
import mx.tec.sonrisas.ui.components.formatoDiaMes
import mx.tec.sonrisas.ui.theme.Espacio
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** entrega a android el pdf ya generado; el sistema solo lo copia a la impresora */
private class AdaptadorPdf(private val archivo: File, private val nombre: String) : PrintDocumentAdapter() {
    override fun onLayout(
        anteriores: PrintAttributes?,
        nuevos: PrintAttributes?,
        cancelacion: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancelacion?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("$nombre.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build()
        callback.onLayoutFinished(info, anteriores != nuevos)
    }

    override fun onWrite(paginas: Array<out PageRange>?, destino: ParcelFileDescriptor, cancelacion: CancellationSignal?, callback: WriteResultCallback) {
        try {
            FileInputStream(archivo).use { entrada -> FileOutputStream(destino.fileDescriptor).use { entrada.copyTo(it) } }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        }
    }
}

fun Context.imprimirPdf(archivo: File, nombre: String, orientacion: Orientacion) {
    val manager = getSystemService(Context.PRINT_SERVICE) as PrintManager
    val carta = if (orientacion == Orientacion.HORIZONTAL) PrintAttributes.MediaSize.NA_LETTER.asLandscape() else PrintAttributes.MediaSize.NA_LETTER.asPortrait()
    manager.print(nombre, AdaptadorPdf(archivo, nombre), PrintAttributes.Builder().setMediaSize(carta).build())
}

/** se coloca una vez en el destino; aparece cuando la pantalla llama a vm.abrir() */
@Composable
fun ImpresionBitacora(vm: BitacoraViewModel) {
    val h = vm.hoja ?: return
    val context = LocalContext.current
    val esSala = h.objetivo is Objetivo.Sala
    val semanal = esSala || h.formato == FormatoBitacora.SEMANAL
    val alListo: (PdfListo) -> Unit = { pdf ->
        when (pdf.salida) {
            Salida.IMPRIMIR -> context.imprimirPdf(pdf.archivo, pdf.nombre, pdf.orientacion)
            Salida.COMPARTIR -> context.compartirArchivo(pdf.archivo.path, "application/pdf", pdf.nombre)
        }
    }
    HojaModal(
        titulo = "Imprimir bitácora",
        subtitulo = when (val o = h.objetivo) {
            is Objetivo.UnNino -> o.nombre
            is Objetivo.Sala -> "Sala ${o.nombre}: cada niño empieza en su propia hoja"
        },
        onCerrar = vm::cerrar,
        boton = {
            BotonPrincipal("Imprimir", { vm.generar(Salida.IMPRIMIR, alListo) }, habilitado = h.generando == null, guardando = h.generando == Salida.IMPRIMIR)
            OutlinedButton(
                onClick = { vm.generar(Salida.COMPARTIR, alListo) },
                enabled = h.generando == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                if (h.generando == Salida.COMPARTIR) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icono(R.drawable.ic_share, null)
                Text("Compartir PDF", modifier = Modifier.padding(start = Espacio.space2))
            }
        },
    ) {
        if (!esSala) {
            EtiquetaCampo("Formato")
            SelectorOpciones(FormatoBitacora.entries, h.formato, { vm.cambiar(h.copy(formato = it)) }, { it.etiqueta })
        }
        CampoFecha(if (semanal) "Cualquier día de la semana" else "Día", h.fecha, { it?.let { f -> vm.cambiar(h.copy(fecha = f)) } })
        if (semanal) {
            val lunes = h.fecha.lunesDeLaSemana()
            Text("Se imprime del lunes ${lunes.formatoDiaMes()} al viernes ${lunes.plusDays(4).formatoDiaMes()}.", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "Sigue el orden del formato de filtro diario. Los días sin asistencia dicen \"No asistió\" y los campos vacíos, \"Sin registro\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        h.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}
