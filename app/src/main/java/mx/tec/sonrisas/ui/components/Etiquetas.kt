package mx.tec.sonrisas.ui.components

import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.ControlEsfinteres
import mx.tec.sonrisas.domain.EstadoAnimo
import mx.tec.sonrisas.domain.Porcion
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.SiNoNoAplica
import mx.tec.sonrisas.domain.TemaFoto
import mx.tec.sonrisas.domain.TiempoComida
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.domain.TipoEvacuacion
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.domain.UsoFoto

val TiempoComida.etiqueta: String
    get() = when (this) {
        TiempoComida.DESAYUNO -> "Desayuno"
        TiempoComida.COLACION -> "Colación"
        TiempoComida.COMIDA -> "Comida"
    }

val Porcion.etiqueta: String
    get() = when (this) {
        Porcion.NADA -> "Nada"
        Porcion.POCO -> "Poco"
        Porcion.TODO -> "Todo"
        Porcion.PIDIO_MAS -> "Pidió más"
    }

val TipoEvacuacion.etiqueta: String
    get() = when (this) {
        TipoEvacuacion.PIPI -> "Pipí"
        TipoEvacuacion.POPO -> "Popó"
        TipoEvacuacion.AMBOS -> "Ambos"
    }

val ControlEsfinteres.etiqueta: String
    get() = when (this) {
        ControlEsfinteres.AVISO -> "Avisó"
        ControlEsfinteres.NO_AVISO -> "No avisó"
        ControlEsfinteres.NO_APLICA -> "No aplica"
    }

val EstadoAnimo.etiqueta: String
    get() = when (this) {
        EstadoAnimo.FELIZ -> "Feliz"
        EstadoAnimo.TRISTE -> "Triste"
        EstadoAnimo.ENOJADO -> "Enojado"
    }

val EstadoAnimo.icono: Int
    get() = when (this) {
        EstadoAnimo.FELIZ -> R.drawable.ic_sentiment_satisfied
        EstadoAnimo.TRISTE -> R.drawable.ic_sentiment_dissatisfied
        EstadoAnimo.ENOJADO -> R.drawable.ic_sentiment_very_dissatisfied
    }

val TipoIncidente.etiqueta: String
    get() = when (this) {
        TipoIncidente.GOLPE -> "Golpe"
        TipoIncidente.CAIDA -> "Caída"
        TipoIncidente.MORDIDA -> "Mordida"
        TipoIncidente.RASGUNO -> "Rasguño"
        TipoIncidente.REACCION_ALERGICA -> "Reacción alérgica"
        TipoIncidente.FIEBRE -> "Fiebre"
        TipoIncidente.MALESTAR -> "Malestar"
        TipoIncidente.OTRO -> "Otro"
    }

val TipoAviso.etiqueta: String
    get() = when (this) {
        TipoAviso.JUNTA -> "Junta"
        TipoAviso.SUSPENSION -> "Suspensión"
        TipoAviso.CAMPANA -> "Campaña"
        TipoAviso.GENERAL -> "General"
    }

val TipoAviso.icono: Int
    get() = when (this) {
        TipoAviso.JUNTA -> R.drawable.ic_groups
        TipoAviso.SUSPENSION -> R.drawable.ic_event_busy
        TipoAviso.CAMPANA -> R.drawable.ic_vaccines
        TipoAviso.GENERAL -> R.drawable.ic_campaign
    }

val TemaFoto.etiqueta: String
    get() = when (this) {
        TemaFoto.PINTURA -> "Pintura"
        TemaFoto.JARDIN -> "Jardín"
        TemaFoto.MUSICA -> "Música"
        TemaFoto.LECTURA -> "Lectura"
        TemaFoto.CUMPLEANOS -> "Cumpleaños"
        TemaFoto.JUEGO -> "Juego"
    }

val TemaFoto.icono: Int
    get() = when (this) {
        TemaFoto.PINTURA -> R.drawable.ic_palette
        TemaFoto.JARDIN -> R.drawable.ic_park
        TemaFoto.MUSICA -> R.drawable.ic_music_note
        TemaFoto.LECTURA -> R.drawable.ic_menu_book
        TemaFoto.CUMPLEANOS -> R.drawable.ic_cake
        TemaFoto.JUEGO -> R.drawable.ic_toys
    }

val UsoFoto.etiqueta: String
    get() = when (this) {
        UsoFoto.INTERNA -> "Solo para las familias"
        UsoFoto.REDES -> "También para redes de la fundación"
    }

val TipoSala.etiqueta: String
    get() = when (this) {
        TipoSala.LACTANTES -> "Lactantes"
        TipoSala.MATERNALES -> "Maternales"
        TipoSala.PREESCOLAR -> "Preescolar"
    }

val ColorEstrella.etiqueta: String
    get() = when (this) {
        ColorEstrella.AZUL -> "Azul"
        ColorEstrella.ROJO -> "Roja"
        ColorEstrella.NARANJA -> "Naranja"
        ColorEstrella.VERDE -> "Verde"
        ColorEstrella.AMARILLO -> "Amarilla"
    }

val Rol.etiqueta: String
    get() = when (this) {
        Rol.EDUCADORA -> "Educadora"
        Rol.FAMILIAR -> "Familiar"
        Rol.DIRECCION -> "Dirección"
        Rol.FUNDACION -> "Fundación"
    }

val SiNoNoAplica.etiqueta: String
    get() = when (this) {
        SiNoNoAplica.SI -> "Sí"
        SiNoNoAplica.NO -> "No"
        SiNoNoAplica.NO_APLICA -> "No aplica"
    }

val AccionRegistro.etiqueta: String
    get() = when (this) {
        AccionRegistro.CAPTURA -> "Captura"
        AccionRegistro.EDICION -> "Edición"
        AccionRegistro.BORRADO -> "Borrado"
        AccionRegistro.FOTO_CONSULTADA -> "Foto consultada"
        AccionRegistro.EXPEDIENTE -> "Expediente"
        AccionRegistro.AVISO -> "Aviso"
        AccionRegistro.SESION -> "Sesión"
        AccionRegistro.ESTRUCTURA -> "Estancias y salas"
        AccionRegistro.CUENTA -> "Cuentas"
    }

val AccionRegistro.icono: Int
    get() = when (this) {
        AccionRegistro.CAPTURA -> R.drawable.ic_edit_note
        AccionRegistro.EDICION -> R.drawable.ic_edit
        AccionRegistro.BORRADO -> R.drawable.ic_delete
        AccionRegistro.FOTO_CONSULTADA -> R.drawable.ic_image
        AccionRegistro.EXPEDIENTE -> R.drawable.ic_badge
        AccionRegistro.AVISO -> R.drawable.ic_campaign
        AccionRegistro.SESION -> R.drawable.ic_login
        AccionRegistro.ESTRUCTURA -> R.drawable.ic_meeting_room
        AccionRegistro.CUENTA -> R.drawable.ic_key
    }
