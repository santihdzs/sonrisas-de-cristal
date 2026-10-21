package mx.tec.sonrisas.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.AppViewModelProvider
import mx.tec.sonrisas.ui.components.CargandoView
import mx.tec.sonrisas.ui.components.DialogoCredenciales
import mx.tec.sonrisas.ui.components.ErrorView
import mx.tec.sonrisas.ui.screens.direccion.EstanciaFormScreen
import mx.tec.sonrisas.ui.screens.direccion.EstanciaViewModel
import mx.tec.sonrisas.ui.screens.direccion.FundacionScreen
import mx.tec.sonrisas.ui.screens.direccion.FundacionViewModel
import mx.tec.sonrisas.ui.screens.direccion.HojaNuevaCuenta
import mx.tec.sonrisas.ui.state.CredencialesViewModel
import mx.tec.sonrisas.ui.state.UiState

@Composable
fun FundacionApp(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val mensajero = rememberMensajero(snackbar)
    val credenciales: CredencialesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    // una sola pestana, asi que no se muestra barra inferior
    val pestanas = listOf(Pestana(Route.FUNDACION, "Resumen", R.drawable.ic_apartment, R.drawable.ic_apartment_fill))
    ScaffoldRol(
        navController, pestanas, snackbar, titulo = ::tituloFundacion,
        // patron B: titulo grande en el resumen, igual que las pestañas de la direccion
        rutasConTituloGrande = setOf(Route.FUNDACION, Route.PERFIL),
    ) { padding ->
        NavHost(navController, startDestination = Route.FUNDACION, modifier = Modifier.padding(padding)) {
            composable(Route.FUNDACION) {
                val vm: FundacionViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> FundacionScreen(
                        e.datos,
                        onReporte = { navController.navigate(Route.ASISTENCIA) },
                        onRegistrar = { navController.navigate(Route.estancia()) },
                        onEstancia = { navController.navigate(Route.estancia(it.id)) },
                    )
                }
            }
            composable(
                Route.ESTANCIA_FORM,
                arguments = listOf(navArgument(Route.ARG_ESTANCIA) { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                val vm: EstanciaViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> EstanciaFormScreen(
                        datos = e.datos,
                        form = vm.form,
                        onCambiar = vm::cambiar,
                        onGuardar = {
                            vm.guardar { mensaje, nuevas ->
                                mensajero.mostrar(mensaje)
                                credenciales.mostrar(nuevas)
                                navController.popBackStack()
                            }
                        },
                        onNuevaCuenta = vm::abrirNuevaCuenta,
                    )
                }
                vm.nuevaCuenta?.let { f ->
                    HojaNuevaCuenta(
                        titulo = "Crear cuenta de dirección",
                        form = f,
                        onCambiar = vm::cambiarNuevaCuenta,
                        onCrear = { vm.crearCuenta(credenciales::mostrar) },
                        onCerrar = vm::cerrarNuevaCuenta,
                    )
                }
            }
            destinoReporte()
            destinosComunes(navController, onCerrarSesion)
        }
    }
    DialogoCredenciales(credenciales.pendientes, onCerrar = credenciales::cerrar)
}

private fun tituloFundacion(ruta: String?): String = tituloComun(ruta) ?: when (ruta) {
    Route.FUNDACION -> "Resumen de la fundación"
    Route.ASISTENCIA -> "Reporte de asistencia"
    Route.ESTANCIA_FORM -> "Estancia"
    else -> ""
}
