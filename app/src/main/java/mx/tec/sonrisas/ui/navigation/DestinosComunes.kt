package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import mx.tec.sonrisas.BuildConfig
import mx.tec.sonrisas.ui.AppViewModelProvider
import mx.tec.sonrisas.ui.components.CargandoView
import mx.tec.sonrisas.ui.components.ErrorView
import mx.tec.sonrisas.ui.screens.comun.AcercaDeScreen
import mx.tec.sonrisas.ui.screens.comun.AvisosScreen
import mx.tec.sonrisas.ui.screens.comun.AvisosViewModel
import mx.tec.sonrisas.ui.screens.comun.PerfilScreen
import mx.tec.sonrisas.ui.screens.comun.PerfilViewModel
import mx.tec.sonrisas.ui.state.UiState

fun NavGraphBuilder.destinosComunes(navController: NavHostController, onCerrarSesion: () -> Unit) {
    composable(Route.PERFIL) {
        val vm: PerfilViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val estado by vm.uiState.collectAsStateWithLifecycle()
        when (val e = estado) {
            UiState.Cargando -> CargandoView()
            is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
            is UiState.Exito -> PerfilScreen(
                datos = e.datos,
                onCambiarHuella = vm::cambiarHuella,
                onAcercaDe = { navController.navigate(Route.ACERCA) },
                onCerrarSesion = onCerrarSesion,
            )
        }
    }
    composable(Route.ACERCA) {
        AcercaDeScreen(version = BuildConfig.VERSION_NAME)
    }
}

/** al salir de la lista se marcan como leidos los avisos que se vieron */
fun NavGraphBuilder.destinoAvisos(alSalir: (Collection<String>) -> Unit, onPublicar: (() -> Unit)? = null) {
    composable(Route.AVISOS) {
        val vm: AvisosViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val estado by vm.uiState.collectAsStateWithLifecycle()
        val vistos = (estado as? UiState.Exito)?.datos?.avisos?.map { it.id }.orEmpty()
        val ultimos by rememberUpdatedState(vistos)
        DisposableEffect(Unit) { onDispose { alSalir(ultimos) } }
        when (val e = estado) {
            UiState.Cargando -> CargandoView()
            is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
            is UiState.Exito -> AvisosScreen(e.datos, onPublicar = onPublicar)
        }
    }
}
