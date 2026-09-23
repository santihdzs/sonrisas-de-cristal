package mx.tec.sonrisas.data.fake

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.Usuario
import java.time.LocalDateTime
import java.util.UUID

/** unica fuente de verdad en memoria; todos los repositorios falsos leen y escriben aqui */
class AlmacenMemoria(datos: DatosDePrueba) {
    val estancias = MutableStateFlow(datos.estancias)
    val salas = MutableStateFlow(datos.salas)
    val usuarios = MutableStateFlow(datos.usuarios)
    val tutores = MutableStateFlow(datos.tutores)
    val ninos = MutableStateFlow(datos.ninos)
    val asistencias = MutableStateFlow(datos.asistencias)
    val eventos = MutableStateFlow(datos.eventos)
    val incidentes = MutableStateFlow(datos.incidentes)
    val fotos = MutableStateFlow(datos.fotos)
    val avisos = MutableStateFlow(datos.avisos)
    val bloques = MutableStateFlow(datos.bloques)
    val especiales = MutableStateFlow(datos.especiales)
    val registros = MutableStateFlow(datos.registros)
    val avisosLeidos = MutableStateFlow(datos.avisosLeidos)
    val desbloqueoHuella = MutableStateFlow<Set<String>>(emptySet())
    var siguienteFolio = datos.siguienteFolio

    fun registrar(
        usuario: Usuario?,
        cuando: LocalDateTime,
        accion: AccionRegistro,
        detalle: String,
        ninoId: String? = null,
        estanciaId: String? = usuario?.estanciaId,
    ) {
        val registro = RegistroActividad(
            id = UUID.randomUUID().toString(),
            cuando = cuando,
            personaId = usuario?.id ?: "sistema",
            personaNombre = usuario?.nombre ?: "Sistema",
            accion = accion,
            detalle = detalle,
            ninoId = ninoId,
            estanciaId = estanciaId,
        )
        registros.update { listOf(registro) + it }
    }
}
