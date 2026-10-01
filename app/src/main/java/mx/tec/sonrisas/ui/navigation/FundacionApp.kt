package mx.tec.sonrisas.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.AppViewModelProvider
import mx.tec.sonrisas.ui.components.CargandoView
import mx.tec.sonrisas.ui.components.ErrorView
import mx.tec.sonrisas.ui.screens.direccion.FundacionScreen
import mx.tec.sonrisas.ui.screens.direccion.FundacionViewModel
import mx.tec.sonrisas.ui.state.UiState

@Composable
fun FundacionApp(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    // una sola pestana, asi que no se muestra barra inferior
    val pestanas = listOf(Pestana(Route.FUNDACION, "Resumen", R.drawable.ic_apartment, R.drawable.ic_apartment_fill))
    ScaffoldRol(navController, pestanas, snackbar, titulo = ::tituloFundacion) { padding ->
        NavHost(navController, startDestination = Route.FUNDACION, modifier = Modifier.padding(padding)) {
            composable(Route.FUNDACION) {
                val vm: FundacionViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> FundacionScreen(e.datos, onReporte = { navController.navigate(Route.ASISTENCIA) })
                }
            }
            destinoReporte()
            destinosComunes(navController, onCerrarSesion)
        }
    }
}

private fun tituloFundacion(ruta: String?): String = tituloComun(ruta) ?: when (ruta) {
    Route.FUNDACION -> "Resumen de la fundación"
    Route.ASISTENCIA -> "Reporte de asistencia"
    else -> ""
}
