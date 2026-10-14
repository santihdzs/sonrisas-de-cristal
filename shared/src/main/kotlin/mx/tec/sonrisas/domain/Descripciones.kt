package mx.tec.sonrisas.domain

import java.time.format.DateTimeFormatter

// textos del registro de actividad (RF-14); los arma la capa de datos porque ahi se registra

private val hora = DateTimeFormatter.ofPattern("H:mm")

fun describirLlegada(n: Nino, a: Asistencia): String {
    val quien = n.persona(a.entregadoPorId)?.let { "; entregó ${it.nombre} (${it.parentesco.lowercase()})" } ?: ""
    return "Registró la llegada de ${n.nombreCompleto} a las ${a.horaEntrada.format(hora)}$quien"
}

fun describirSalida(n: Nino, a: Asistencia): String {
    val quien = n.persona(a.recogidoPorId)?.let { "; recogió ${it.nombre} (${it.parentesco.lowercase()})" } ?: ""
    return "Registró la salida de ${n.nombreCompleto} a las ${a.horaSalida?.format(hora)}$quien"
}

fun describirEvento(n: Nino, e: EventoBitacora): String = when (e) {
    is EventoBitacora.Alimento -> {
        val tiempo = when (e.tiempo) {
            TiempoComida.DESAYUNO -> "desayuno"
            TiempoComida.COLACION -> "colación"
            TiempoComida.COMIDA -> "comida"
        }
        val porcion = when (e.porcion) {
            Porcion.NADA -> "nada"
            Porcion.POCO -> "poco"
            Porcion.TODO -> "todo"
            Porcion.PIDIO_MAS -> "pidió más"
        }
        "Registró $tiempo de ${n.nombreCompleto}: $porcion"
    }
    is EventoBitacora.Siesta ->
        if (e.fin == null) "Registró que ${n.nombreCompleto} empezó a dormir a las ${e.inicio.format(hora)}"
        else "Registró siesta de ${n.nombreCompleto} de ${e.inicio.format(hora)} a ${e.fin.format(hora)}"
    is EventoBitacora.Evacuacion -> {
        val tipo = when (e.tipo) {
            TipoEvacuacion.PIPI -> "pipí"
            TipoEvacuacion.POPO -> "popó"
            TipoEvacuacion.AMBOS -> "pipí y popó"
        }
        val control = when (e.control) {
            ControlEsfinteres.AVISO -> ", avisó"
            ControlEsfinteres.NO_AVISO -> ", no avisó"
            ControlEsfinteres.NO_APLICA -> ""
        }
        "Registró baño de ${n.nombreCompleto}: $tipo$control"
    }
    is EventoBitacora.Animo -> {
        val estado = when (e.estado) {
            EstadoAnimo.FELIZ -> "feliz"
            EstadoAnimo.TRISTE -> "triste"
            EstadoAnimo.ENOJADO -> "enojado"
        }
        "Registró ánimo de ${n.nombreCompleto}: $estado"
    }
    is EventoBitacora.Nota -> "Escribió una nota para la familia de ${n.nombreCompleto}"
}

fun describirIncidente(n: Nino, i: Incidente): String =
    "Registró el incidente ${i.folio.ifBlank { "" }} de ${n.nombreCompleto}: ${i.descripcion}".replace("  ", " ")
