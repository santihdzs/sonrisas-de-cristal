package mx.tec.sonrisas.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * DEMO: las cuentas de prueba entran siempre, haya o no servidor.
 * primero se intenta el servidor; si no contesta o no conoce la cuenta y el usuario y la contraseña son de los
 * datos de prueba, la sesion sigue con los datos en memoria (data/fake). la app no muestra la diferencia.
 */
class SesionConDemo(
    // null = la app no tiene servidor configurado
    private val servidor: SesionRepository?,
    private val demo: SesionRepository,
    scope: CoroutineScope,
    // en release no hay datos de prueba: todo lo que se ve y se guarda pasa por el servidor
    private val permitirDemo: Boolean = true,
) : SesionRepository {

    private val _enDemo = MutableStateFlow(servidor == null && permitirDemo)

    /** AppContainer lo lee para dar los repositorios de la demo o los del servidor */
    val enDemo: Boolean get() = _enDemo.value

    @OptIn(ExperimentalCoroutinesApi::class)
    override val estado: StateFlow<SesionEstado> =
        _enDemo.flatMapLatest { if (it) demo.estado else servidor!!.estado }
            .stateIn(scope, SharingStarted.Eagerly, (servidor ?: demo).estado.value)

    private val actual: SesionRepository get() = if (enDemo) demo else servidor!!

    override suspend fun iniciar(usuario: String, contrasena: String): ResultadoInicio {
        val remoto = servidor?.iniciar(usuario, contrasena)
        if (remoto == ResultadoInicio.EXITO) {
            _enDemo.value = false
            return remoto
        }
        // si el servidor si contesto con algo distinto (acceso retirado, demasiados intentos), manda el servidor
        if (remoto != null && remoto != ResultadoInicio.SIN_CONEXION && remoto != ResultadoInicio.INCORRECTO) return remoto
        if (!permitirDemo) return remoto ?: ResultadoInicio.SIN_CONEXION
        val local = demo.iniciar(usuario, contrasena)
        if (local == ResultadoInicio.EXITO) _enDemo.value = true
        // una cuenta real con el servidor caido debe ver "sin conexion", no "contraseña incorrecta"
        return if (local == ResultadoInicio.INCORRECTO && remoto != null) remoto else local
    }

    override suspend fun cerrar() = actual.cerrar()

    override fun cambiarDesbloqueoHuella(activo: Boolean) = actual.cambiarDesbloqueoHuella(activo)
}
