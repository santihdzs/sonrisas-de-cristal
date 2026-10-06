package mx.tec.sonrisas.ui.navigation

object Route {
    const val ARG_NINO = "ninoId"
    const val ARG_FOTO = "fotoId"
    const val ARG_INCIDENTE = "incidenteId"
    const val ARG_ESTANCIA = "estanciaId"
    const val ARG_SALA = "salaId"
    const val ARG_CONVERSACION = "conversacionId"

    const val PERFIL = "perfil"
    const val ACERCA = "acerca"
    const val AVISOS = "avisos"
    const val FOTO = "foto/{$ARG_FOTO}"
    const val INCIDENTE_DETALLE = "incidente_detalle/{$ARG_INCIDENTE}"
    const val OBJETOS = "objetos/{$ARG_SALA}"
    const val MENSAJES = "mensajes"
    const val CHAT = "chat/{$ARG_CONVERSACION}"

    // educadora
    const val SALA = "sala"
    const val FICHA = "ficha/{$ARG_NINO}"
    const val REGISTRO_GRUPAL = "registro_grupal"
    const val INCIDENTE = "incidente/{$ARG_NINO}"
    const val FOTOS_SALA = "fotos_sala"
    const val AGREGAR_FOTO = "agregar_foto"

    // familiar
    const val FAMILIA = "familia"
    const val HOY = "hoy"
    const val SEMANA = "semana"
    const val FOTOS_FAMILIA = "fotos_familia"

    // direccion
    const val INICIO = "inicio"
    const val NINOS = "ninos"
    const val MAS = "mas"
    const val EXPEDIENTE = "expediente/{$ARG_NINO}"
    const val INSCRIPCION = "inscripcion?$ARG_NINO={$ARG_NINO}"
    const val PUBLICAR_AVISO = "publicar_aviso"
    const val ASISTENCIA = "asistencia"
    const val HORARIOS = "horarios"
    const val ACTIVIDAD = "actividad"
    const val SALAS = "salas"
    const val PERSONAL = "personal"

    // fundacion
    const val FUNDACION = "fundacion"
    const val ESTANCIA_FORM = "estancia?$ARG_ESTANCIA={$ARG_ESTANCIA}"

    fun foto(id: String) = "foto/$id"
    fun objetos(salaId: String) = "objetos/$salaId"
    fun chat(conversacionId: String) = "chat/$conversacionId"
    fun incidenteDetalle(id: String) = "incidente_detalle/$id"
    fun ficha(ninoId: String) = "ficha/$ninoId"
    fun incidente(ninoId: String) = "incidente/$ninoId"
    fun expediente(ninoId: String) = "expediente/$ninoId"
    fun estancia(id: String? = null) = if (id == null) "estancia" else "estancia?$ARG_ESTANCIA=$id"
    fun inscripcion(ninoId: String? = null) = if (ninoId == null) "inscripcion" else "inscripcion?$ARG_NINO=$ninoId"
}
