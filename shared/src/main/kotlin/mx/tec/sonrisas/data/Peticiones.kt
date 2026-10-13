@file:UseSerializers(FechaHoraSerializer::class, HoraSerializer::class)

package mx.tec.sonrisas.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import mx.tec.sonrisas.domain.FechaHoraSerializer
import mx.tec.sonrisas.domain.HoraSerializer
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.domain.Usuario
import java.time.LocalDateTime
import java.time.LocalTime

// cuerpos de las peticiones y respuestas del api que no son un modelo del dominio

@Serializable
data class Login(val usuario: String, val contrasena: String)

@Serializable
data class Perfil(val usuario: Usuario, val desbloqueoHuella: Boolean)

@Serializable
data class RespuestaLogin(val token: String, val perfil: Perfil)

@Serializable
data class Activo(val activo: Boolean)

@Serializable
data class Error(val mensaje: String)

@Serializable
data class Id(val id: String)

@Serializable
data class Ids(val ids: List<String>)

@Serializable
data class Texto(val texto: String?)

@Serializable
data class Numero(val valor: Int)

@Serializable
data class Hora(val hora: LocalTime)

@Serializable
data class GuardarNino(val nino: Nino, val tutores: List<Tutor>)

@Serializable
data class Exportacion(val ninoIds: List<String>, val detalle: String)

@Serializable
data class NuevoIncidente(
    val ninoId: String,
    val cuando: LocalDateTime,
    val tipo: TipoIncidente,
    val parteDelCuerpo: String?,
    val descripcion: String,
    val accionTomada: String,
)

@Serializable
data class NuevoAviso(val tipo: TipoAviso, val titulo: String, val cuerpo: String, val fechaEvento: LocalDateTime?)

@Serializable
data class EdicionMaterial(val titulo: String, val descripcion: String)

@Serializable
data class AbrirDirecta(val usuarioId: String, val salaId: String)

@Serializable
data class NuevaCuenta(val nombre: String, val estanciaId: String? = null, val salaId: String? = null)
