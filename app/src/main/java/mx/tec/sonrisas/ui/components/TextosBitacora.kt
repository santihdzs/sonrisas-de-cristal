package mx.tec.sonrisas.ui.components

import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ControlEsfinteres
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Incidente

val EventoBitacora.icono: Int
    get() = when (this) {
        is EventoBitacora.Alimento -> R.drawable.ic_restaurant
        is EventoBitacora.Siesta -> R.drawable.ic_bedtime
        is EventoBitacora.Evacuacion -> R.drawable.ic_wc
        is EventoBitacora.Animo -> estado.icono
        is EventoBitacora.Nota -> R.drawable.ic_edit_note
    }

fun EventoBitacora.titulo(): String = when (this) {
    is EventoBitacora.Alimento -> "${tiempo.etiqueta}: ${porcion.etiqueta.lowercase()}"
    is EventoBitacora.Siesta ->
        if (fin == null) "Durmiendo desde las ${inicio.formatoHora()}"
        else "Siesta de ${inicio.formatoHora()} a ${fin.formatoHora()} (${minutosTexto(minutos)})"
    is EventoBitacora.Evacuacion -> buildString {
        append("Baño: ${tipo.etiqueta.lowercase()}")
        if (control != ControlEsfinteres.NO_APLICA) append(", ${control.etiqueta.lowercase()}")
    }
    is EventoBitacora.Animo -> "Ánimo: ${estado.etiqueta.lowercase()}"
    is EventoBitacora.Nota -> "Nota para la familia"
}

fun EventoBitacora.detalle(): String? = when (this) {
    is EventoBitacora.Animo -> detallesAnimo()
    is EventoBitacora.Nota -> texto
    else -> null
}

fun EventoBitacora.Animo.detallesAnimo(): String = listOf(
    if (lloro) "Lloró" else "No lloró",
    if (peleo) "peleó" else "no peleó",
    if (participo) "participó en actividades" else "no participó en actividades",
).joinToString(", ")

fun Incidente.titulo(): String = "Incidente: ${tipo.etiqueta.lowercase()}"
