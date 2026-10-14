package mx.tec.sonrisas.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.tec.sonrisas.SonrisasApplication
import mx.tec.sonrisas.data.Sesion
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.ui.AppViewModelProvider
import mx.tec.sonrisas.ui.screens.comun.BloqueoScreen
import mx.tec.sonrisas.ui.screens.comun.LoginScreen
import mx.tec.sonrisas.ui.screens.comun.LoginViewModel
import mx.tec.sonrisas.ui.screens.comun.SplashScreen
import mx.tec.sonrisas.ui.state.SesionViewModel

@Composable
fun SonrisasApp(sesionViewModel: SesionViewModel, onDesbloquear: () -> Unit) {
    val estado by sesionViewModel.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        SesionEstado.Cargando -> SplashScreen()
        SesionEstado.Anonima -> {
            val vm: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val context = LocalContext.current
            val contenedor = (context.applicationContext as SonrisasApplication).container
            LoginScreen(
                estado = vm.estado,
                onUsuario = vm::cambiarUsuario,
                onContrasena = vm::cambiarContrasena,
                onMostrarContrasena = vm::alternarMostrar,
                onEntrar = vm::entrar,
                servidor = contenedor.servidor,
                onServidor = { url ->
                    contenedor.cambiarServidor(url)
                    context.reiniciarApp()
                },
            )
        }
        is SesionEstado.Activa -> {
            val bloqueada = sesionViewModel.bloqueada
            // la app sigue compuesta debajo para no perder la navegacion, pero sin semantica mientras esta bloqueada
            Box(if (bloqueada) Modifier.clearAndSetSemantics { } else Modifier) {
                SesionActiva(e.sesion, sesionViewModel.tiendaPara(e.sesion.usuario.id), onCerrarSesion = sesionViewModel::cerrarSesion)
            }
            if (bloqueada) {
                val contexto by sesionViewModel.contextoBloqueo.collectAsStateWithLifecycle()
                BackHandler { }
                BloqueoScreen(
                    titulo = contexto?.titulo ?: "Sonrisas de Cristal",
                    estrella = contexto?.estrella,
                    nombreUsuario = contexto?.nombre ?: e.sesion.usuario.nombre,
                    mensaje = sesionViewModel.mensajeBloqueo,
                    onDesbloquear = onDesbloquear,
                    onCambiarUsuario = sesionViewModel::cerrarSesion,
                )
            }
        }
    }
}

private class AlmacenDeSesion(app: Application, override val viewModelStore: ViewModelStore) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = AppViewModelProvider.Factory
    override val defaultViewModelCreationExtras: CreationExtras =
        MutableCreationExtras().apply { set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, app) }
}

@Composable
private fun SesionActiva(sesion: Sesion, tienda: ViewModelStore, onCerrarSesion: () -> Unit) {
    // cada sesion tiene sus propios viewmodels; SesionViewModel los descarta al cambiar de usuario
    val app = LocalContext.current.applicationContext as Application
    val almacen = remember(tienda) { AlmacenDeSesion(app, tienda) }
    CompositionLocalProvider(LocalViewModelStoreOwner provides almacen) {
        when (sesion.usuario.rol) {
            Rol.EDUCADORA -> EducadoraApp(onCerrarSesion)
            Rol.FAMILIAR -> FamiliarApp(onCerrarSesion)
            Rol.DIRECCION -> DireccionApp(onCerrarSesion)
            Rol.FUNDACION -> FundacionApp(onCerrarSesion)
        }
    }
}

// los repositorios se eligen al crear el contenedor, asi que cambiar de servidor requiere un proceso nuevo
private fun Context.reiniciarApp() {
    val inicio = packageManager.getLaunchIntentForPackage(packageName)!!
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(inicio)
    Runtime.getRuntime().exit(0)
}
