package mx.tec.sonrisas.data.remoto

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.Activo
import mx.tec.sonrisas.data.JsonApi
import mx.tec.sonrisas.data.Login
import mx.tec.sonrisas.data.Perfil
import mx.tec.sonrisas.data.RespuestaLogin
import mx.tec.sonrisas.data.ResultadoInicio
import mx.tec.sonrisas.data.Sesion
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.data.SesionRepository
import java.io.File
import java.io.IOException

/**
 * la sesion sobrevive a cerrar la app: el token y el perfil se guardan en las preferencias privadas.
 * el servidor sigue decidiendo: si el token vence o retiran el acceso, cualquier respuesta 401 cierra la sesion.
 */
class RemotoSesionRepository(
    context: Context,
    private val servidor: String,
    private val scope: CoroutineScope,
    // archivos bajados del servidor; se borran al cerrar sesion
    private val cache: List<File>,
) : SesionRepository {

    private val prefs = context.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    @Volatile
    private var token: String? = prefs.getString("token", null)

    val api = Api(servidor, { token }, ::vencer)

    private val _estado = MutableStateFlow(restaurar())
    override val estado: StateFlow<SesionEstado> = _estado.asStateFlow()

    private fun restaurar(): SesionEstado {
        val perfil = prefs.getString("perfil", null)?.let { runCatching { JsonApi.decodeFromString(Perfil.serializer(), it) }.getOrNull() }
        if (token == null || perfil == null) return SesionEstado.Anonima
        return SesionEstado.Activa(Sesion(perfil.usuario, perfil.desbloqueoHuella))
    }

    init {
        // si la direccion cambia la sala o retira el acceso, la sesion abierta se actualiza o se cierra
        scope.launch {
            while (isActive) {
                if (token != null) runCatching { guardar(api.get<Perfil>("/yo")) }
                delay(60_000)
            }
        }
    }

    private fun guardar(perfil: Perfil, nuevoToken: String? = null) {
        val editor = prefs.edit().putString("perfil", JsonApi.encodeToString(Perfil.serializer(), perfil))
        nuevoToken?.let { editor.putString("token", it) }
        editor.apply()
        nuevoToken?.let { token = it }
        if (token != null) _estado.value = SesionEstado.Activa(Sesion(perfil.usuario, perfil.desbloqueoHuella))
    }

    private fun vencer() {
        token = null
        prefs.edit().clear().apply()
        cache.forEach { it.deleteRecursively() }
        _estado.value = SesionEstado.Anonima
    }

    override suspend fun iniciar(usuario: String, contrasena: String): ResultadoInicio = try {
        val r = api.enviar("POST", "/auth/login", Login(usuario.trim(), contrasena)).como<RespuestaLogin>()
        guardar(r.perfil, r.token)
        ResultadoInicio.EXITO
    } catch (e: SesionVencida) {
        ResultadoInicio.INCORRECTO
    } catch (e: AccesoDenegado) {
        ResultadoInicio.SIN_ACCESO
    } catch (e: DemasiadosIntentos) {
        ResultadoInicio.DEMASIADOS_INTENTOS
    } catch (e: IOException) {
        Log.w("Sonrisas", "no se pudo conectar con $servidor", e)
        ResultadoInicio.SIN_CONEXION
    } catch (e: IllegalArgumentException) {
        // la direccion del servidor apunta a algo que no es este servidor
        Log.w("Sonrisas", "respuesta inesperada de $servidor", e)
        ResultadoInicio.SIN_CONEXION
    }

    override suspend fun cerrar() {
        // aunque no haya red, el telefono olvida la sesion
        runCatching { api.post("/auth/logout") }
        vencer()
    }

    override fun cambiarDesbloqueoHuella(activo: Boolean) {
        val actual = (estado.value as? SesionEstado.Activa)?.sesion ?: return
        guardar(Perfil(actual.usuario, activo))
        scope.launch { runCatching { api.enviar("PUT", "/yo/huella", Activo(activo)) } }
    }
}
