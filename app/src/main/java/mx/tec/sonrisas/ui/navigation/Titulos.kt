package mx.tec.sonrisas.ui.navigation

fun tituloComun(ruta: String?): String? = when (ruta) {
    Route.PERFIL -> "Mi perfil"
    Route.ACERCA -> "Acerca de"
    Route.AVISOS -> "Avisos"
    Route.FOTO -> "Foto"
    Route.INCIDENTE_DETALLE -> "Incidente"
    Route.OBJETOS -> "Objetos perdidos"
    else -> null
}
