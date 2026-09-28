package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.tec.sonrisas.data.Sesion
import mx.tec.sonrisas.data.SesionEstado
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.ui.AppViewModelProvider
import mx.tec.sonrisas.ui.screens.comun.LoginScreen
import mx.tec.sonrisas.ui.screens.comun.LoginViewModel
import mx.tec.sonrisas.ui.screens.comun.SplashScreen
import mx.tec.sonrisas.ui.state.SesionViewModel

@Composable
fun SonrisasApp(sesionViewModel: SesionViewModel) {
    val estado by sesionViewModel.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        SesionEstado.Cargando -> SplashScreen()
        SesionEstado.Anonima -> {
            val vm: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LoginScreen(
                estado = vm.estado,
                cuentas = vm.cuentas,
                onUsuario = vm::cambiarUsuario,
                onContrasena = vm::cambiarContrasena,
                onMostrarContrasena = vm::alternarMostrar,
                onEntrar = vm::entrar,
                onCuentaDemo = vm::entrarComo,
            )
        }
        is SesionEstado.Activa -> SesionActiva(e.sesion, onCerrarSesion = sesionViewModel::cerrarSesion)
    }
}

private class AlmacenDeSesion : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

@Composable
private fun SesionActiva(sesion: Sesion, onCerrarSesion: () -> Unit) {
    // cada sesion tiene sus propios viewmodels; al salir se descartan y no queda nada del usuario anterior
    val almacen = remember(sesion.usuario.id) { AlmacenDeSesion() }
    DisposableEffect(almacen) { onDispose { almacen.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides almacen) {
        when (sesion.usuario.rol) {
            Rol.EDUCADORA -> EducadoraApp(onCerrarSesion)
            Rol.FAMILIAR -> FamiliarApp(onCerrarSesion)
            Rol.DIRECCION -> DireccionApp(onCerrarSesion)
            Rol.FUNDACION -> FundacionApp(onCerrarSesion)
        }
    }
}
