package mx.tec.sonrisas.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Estancia(val id: String, val nombre: String, val colonia: String, val telefono: String = "")

enum class TipoSala { LACTANTES, MATERNALES, PREESCOLAR }

enum class ColorEstrella { AZUL, ROJO, NARANJA, VERDE, AMARILLO }

data class Sala(
    val id: String,
    val estanciaId: String,
    val nombre: String,
    val tipo: TipoSala,
    val estrella: ColorEstrella,
)

enum class Rol { EDUCADORA, FAMILIAR, DIRECCION, FUNDACION }

data class Usuario(
    val id: String,
    val usuario: String,
    val contrasena: String,
    val nombre: String,
    val rol: Rol,
    val estanciaId: String? = null,
    val salaId: String? = null,
    val tutorId: String? = null,
    // al retirar el acceso la cuenta no se borra, para que su historial siga en el registro de actividad
    val activo: Boolean = true,
)

/** se muestran una sola vez al crear la cuenta; despues ya no se pueden consultar */
data class Credenciales(val nombre: String, val usuario: String, val contrasena: String, val rol: Rol)

data class Tutor(val id: String, val nombre: String, val parentesco: String, val telefono: String)

data class PersonaAutorizada(val id: String, val nombre: String, val parentesco: String, val telefono: String)

sealed interface EstadoExpediente {
    data object Activo : EstadoExpediente
    data class BajaTemporal(val fechaBaja: LocalDate, val eliminarEl: LocalDate) : EstadoExpediente
}

data class Nino(
    val id: String,
    val nombre: String,
    val apellidos: String,
    val fechaNacimiento: LocalDate,
    val estanciaId: String,
    val salaId: String,
    val tutorIds: List<String>,
    val personasAutorizadas: List<PersonaAutorizada>,
    val alergias: String = "",
    val padecimientos: String = "",
    val medicamentos: String = "",
    val permisoImagenInterna: Boolean = true,
    val permisoImagenRedes: Boolean = true,
    val estado: EstadoExpediente = EstadoExpediente.Activo,
) {
    val nombreCompleto: String get() = "$nombre $apellidos"
    val activo: Boolean get() = estado is EstadoExpediente.Activo
    val tieneInfoMedica: Boolean
        get() = alergias.isNotBlank() || padecimientos.isNotBlank() || medicamentos.isNotBlank()

    fun persona(id: String?): PersonaAutorizada? = personasAutorizadas.firstOrNull { it.id == id }
}

enum class SiNoNoAplica { SI, NO, NO_APLICA }

data class Asistencia(
    val id: String,
    val ninoId: String,
    val fecha: LocalDate,
    val horaEntrada: LocalTime,
    val entregadoPorId: String,
    val recibidoPorId: String,
    val llegoLimpio: Boolean = true,
    val mochilaCompleta: Boolean = true,
    val buenEstadoEntrada: Boolean = true,
    val lesionEntrada: String? = null,
    val sintomas: String? = null,
    val medicamentoConReceta: SiNoNoAplica = SiNoNoAplica.NO_APLICA,
    val horaSalida: LocalTime? = null,
    val recogidoPorId: String? = null,
    val salioLimpio: Boolean? = null,
    val pertenenciasCompletas: Boolean? = null,
    val buenEstadoSalida: Boolean? = null,
    val lesionSalida: String? = null,
    val observacionesSalida: String? = null,
    // personal de la estancia que entrego al nino a la salida
    val entregoSalidaId: String? = null,
) {
    val salio: Boolean get() = horaSalida != null
}

enum class TiempoComida { DESAYUNO, COLACION, COMIDA }
enum class Porcion { NADA, POCO, TODO, PIDIO_MAS }
enum class TipoEvacuacion { PIPI, POPO, AMBOS }
enum class ControlEsfinteres { AVISO, NO_AVISO, NO_APLICA }
enum class EstadoAnimo { FELIZ, TRISTE, ENOJADO }

sealed interface EventoBitacora {
    val id: String
    val ninoId: String
    val cuando: LocalDateTime
    val registradoPorId: String

    data class Alimento(
        override val id: String,
        override val ninoId: String,
        override val cuando: LocalDateTime,
        override val registradoPorId: String,
        val tiempo: TiempoComida,
        val porcion: Porcion,
    ) : EventoBitacora

    data class Siesta(
        override val id: String,
        override val ninoId: String,
        override val cuando: LocalDateTime,
        override val registradoPorId: String,
        val inicio: LocalTime,
        val fin: LocalTime?,
    ) : EventoBitacora {
        val minutos: Int
            get() = if (fin == null) 0 else java.time.Duration.between(inicio, fin).toMinutes().toInt().coerceAtLeast(0)
    }

    data class Evacuacion(
        override val id: String,
        override val ninoId: String,
        override val cuando: LocalDateTime,
        override val registradoPorId: String,
        val tipo: TipoEvacuacion,
        val control: ControlEsfinteres,
    ) : EventoBitacora

    data class Animo(
        override val id: String,
        override val ninoId: String,
        override val cuando: LocalDateTime,
        override val registradoPorId: String,
        val estado: EstadoAnimo,
        val lloro: Boolean,
        val peleo: Boolean,
        val participo: Boolean,
    ) : EventoBitacora

    data class Nota(
        override val id: String,
        override val ninoId: String,
        override val cuando: LocalDateTime,
        override val registradoPorId: String,
        val texto: String,
    ) : EventoBitacora
}

enum class TipoIncidente { GOLPE, CAIDA, MORDIDA, RASGUNO, REACCION_ALERGICA, FIEBRE, MALESTAR, OTRO }

data class Incidente(
    val id: String,
    val folio: String,
    val ninoId: String,
    val cuando: LocalDateTime,
    val tipo: TipoIncidente,
    val parteDelCuerpo: String?,
    val descripcion: String,
    val accionTomada: String,
    val registradoPorId: String,
)

enum class UsoFoto { INTERNA, REDES }
enum class TemaFoto { PINTURA, JARDIN, MUSICA, LECTURA, CUMPLEANOS, JUEGO }

data class Foto(
    val id: String,
    val salaId: String,
    val cuando: LocalDateTime,
    val descripcion: String,
    val ninoIds: List<String>,
    val uso: UsoFoto,
    val tema: TemaFoto,
)

enum class TipoAviso { JUNTA, SUSPENSION, CAMPANA, GENERAL }

data class Aviso(
    val id: String,
    val estanciaId: String,
    val tipo: TipoAviso,
    val titulo: String,
    val cuerpo: String,
    val fechaEvento: LocalDateTime?,
    val publicadoEl: LocalDateTime,
    val autorId: String,
)

enum class EstadoObjeto { DISPONIBLE, RECLAMADO, ENTREGADO }

data class ObjetoPerdido(
    val id: String,
    val salaId: String,
    val descripcion: String,
    val dondeSeEncontro: String,
    val fecha: LocalDate,
    // ruta del archivo en el almacenamiento privado de la app
    val foto: String?,
    val publicadoPorId: String,
    val publicadoEl: LocalDateTime,
    val estado: EstadoObjeto = EstadoObjeto.DISPONIBLE,
    val reclamadoPorId: String? = null,
    // nombre y parentesco de quien reclamo; la capa de datos lo oculta a las demas familias
    val reclamadoPor: String? = null,
    val notaReclamo: String? = null,
    val reclamadoEl: LocalDateTime? = null,
    val entregadoEl: LocalDateTime? = null,
)

data class MaterialClase(
    val id: String,
    val salaId: String,
    val titulo: String,
    val descripcion: String,
    val nombreArchivo: String,
    val tipo: String,
    val tamano: Long,
    // ruta del archivo en el almacenamiento privado de la app
    val ruta: String,
    val subidoPorId: String,
    val subidoEl: LocalDateTime,
)

enum class TipoConversacion { SALA, DIRECTA }

/** el chat de sala es uno por sala; quienes participan se calcula en la capa de datos */
data class Conversacion(
    val id: String,
    val tipo: TipoConversacion,
    val salaId: String,
    val participantes: List<String> = emptyList(),
)

data class Mensaje(
    val id: String,
    val conversacionId: String,
    val autorId: String,
    val texto: String,
    val enviado: LocalDateTime,
)

data class BloqueHorario(
    val id: String,
    val salaId: String,
    val inicio: LocalTime,
    val fin: LocalTime,
    val actividad: String,
)

data class ActividadEspecial(
    val id: String,
    val salaId: String,
    val fecha: LocalDate,
    val inicio: LocalTime,
    val fin: LocalTime,
    val titulo: String,
)

enum class AccionRegistro { CAPTURA, EDICION, BORRADO, FOTO_CONSULTADA, EXPEDIENTE, AVISO, SESION, ESTRUCTURA, CUENTA, OBJETO, MATERIAL, IMPRESION }

data class RegistroActividad(
    val id: String,
    val cuando: LocalDateTime,
    val personaId: String,
    val personaNombre: String,
    val accion: AccionRegistro,
    val detalle: String,
    val ninoId: String? = null,
    val estanciaId: String? = null,
)

interface Reloj {
    fun ahora(): LocalDateTime
    fun hoy(): LocalDate = ahora().toLocalDate()
}

object RelojSistema : Reloj {
    override fun ahora(): LocalDateTime = LocalDateTime.now()
}
