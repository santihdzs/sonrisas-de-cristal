package mx.tec.sonrisas.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mx.tec.sonrisas.domain.Conversacion
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.Mensaje
import mx.tec.sonrisas.domain.Sala

// lo que comparten la app y el servidor: errores, limites y respuestas armadas por la capa de datos

/** el mismo formato en la app, el api y la base de datos */
val JsonApi = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class AccesoDenegado(mensaje: String = "No tienes permiso para hacer esto.") : IllegalStateException(mensaje)

const val LIMITE_ARCHIVO_MB = 10
const val LIMITE_BYTES = LIMITE_ARCHIVO_MB * 1024L * 1024L

class ArchivoMuyGrande : IllegalArgumentException("El archivo pesa más de $LIMITE_ARCHIVO_MB MB.")

@Serializable
data class ResumenEstancia(
    val estancia: Estancia,
    val inscritos: Int,
    val presentesHoy: Int,
    val promedioMes: Int,
)

const val LIMITE_MENSAJE = 1000

// pdf, imagenes y documentos de office; se valida aqui y no solo en el selector de archivos
val TIPOS_MATERIAL = arrayOf(
    "application/pdf",
    "image/*",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
)

fun tipoMaterialPermitido(tipo: String): Boolean =
    TIPOS_MATERIAL.any { if (it.endsWith("/*")) tipo.startsWith(it.removeSuffix("*")) else it == tipo }

/** "Laura · mamá de Sofía", "Mariana · educadora" */
@Serializable
data class MensajeChat(val mensaje: Mensaje, val autor: String, val propio: Boolean)

@Serializable
data class ResumenConversacion(
    val conversacion: Conversacion,
    val titulo: String,
    val subtitulo: String,
    val sala: Sala?,
    val ultimo: MensajeChat?,
    val noLeidos: Int,
)

/** con quien se puede abrir un chat directo: tutor con educadora de la sala de su hijo, y al reves */
@Serializable
data class Contacto(val usuarioId: String, val nombre: String, val etiqueta: String, val sala: Sala)
