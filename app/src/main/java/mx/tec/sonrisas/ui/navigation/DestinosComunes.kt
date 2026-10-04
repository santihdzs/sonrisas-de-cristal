package mx.tec.sonrisas.ui.navigation

import android.content.Context
import android.content.Intent
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
import mx.tec.sonrisas.ui.screens.familia.IncidenteDetalleScreen
import mx.tec.sonrisas.ui.screens.familia.IncidenteDetalleViewModel
import mx.tec.sonrisas.ui.screens.fotos.FotoDetalleScreen
import mx.tec.sonrisas.ui.screens.fotos.FotoDetalleViewModel
import mx.tec.sonrisas.ui.screens.sala.HojaObjetoForm
import mx.tec.sonrisas.ui.screens.sala.ObjetosScreen
import mx.tec.sonrisas.ui.screens.sala.ObjetosViewModel
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

fun NavGraphBuilder.destinoFoto(navController: NavHostController, mensajero: Mensajero) {
    composable(Route.FOTO) {
        val vm: FotoDetalleViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val estado by vm.uiState.collectAsStateWithLifecycle()
        when (val e = estado) {
            UiState.Cargando -> CargandoView()
            is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
            is UiState.Exito -> FotoDetalleScreen(
                datos = if (vm.borrada) null else e.datos,
                borrando = vm.borrando,
                onBorrar = {
                    vm.borrar {
                        mensajero.mostrar(it)
                        navController.popBackStack()
                    }
                },
            )
        }
    }
}

fun NavGraphBuilder.destinoIncidente() {
    composable(Route.INCIDENTE_DETALLE) {
        val vm: IncidenteDetalleViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val estado by vm.uiState.collectAsStateWithLifecycle()
        when (val e = estado) {
            UiState.Cargando -> CargandoView()
            is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
            is UiState.Exito -> IncidenteDetalleScreen(e.datos)
        }
    }
}

/** al salir se marcan como vistos los objetos que la familia alcanzo a ver */
fun NavGraphBuilder.destinoObjetos(mensajero: Mensajero) {
    composable(Route.OBJETOS) {
        val vm: ObjetosViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val estado by vm.uiState.collectAsStateWithLifecycle()
        val vistos = (estado as? UiState.Exito)?.datos?.nuevos.orEmpty()
        val ultimos by rememberUpdatedState(vistos)
        DisposableEffect(Unit) { onDispose { vm.marcarVistos(ultimos) } }
        when (val e = estado) {
            UiState.Cargando -> CargandoView()
            is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
            is UiState.Exito -> {
                ObjetosScreen(
                    datos = e.datos,
                    filtro = vm.filtro,
                    onFiltro = vm::cambiarFiltro,
                    onPublicar = vm::abrirPublicar,
                    onReclamar = vm::abrirReclamo,
                    onEntregar = { vm.entregar(it, mensajero::mostrar) },
                    onRechazar = { vm.rechazar(it, mensajero::mostrar) },
                )
                vm.hoja?.let { h ->
                    HojaObjetoForm(
                        hoja = h,
                        hoy = e.datos.hoy,
                        onCambiar = vm::cambiar,
                        onPublicar = { vm.publicar(mensajero::mostrar) },
                        onReclamar = { vm.reclamar(mensajero::mostrar) },
                        onCerrar = vm::cerrar,
                    )
                }
            }
        }
    }
}

/** abre la hoja de compartir de android con texto plano (RF-12, RF-20) */
fun compartirTexto(context: Context, titulo: String, texto: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, titulo)
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(envio, titulo))
}
