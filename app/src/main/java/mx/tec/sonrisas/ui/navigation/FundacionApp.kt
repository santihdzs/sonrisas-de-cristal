package mx.tec.sonrisas.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.tec.sonrisas.R

@Composable
fun FundacionApp(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val pestanas = listOf(
        Pestana(Route.FUNDACION, "Resumen", R.drawable.ic_apartment, R.drawable.ic_apartment_fill),
    )
    ScaffoldRol(navController, pestanas, snackbar, titulo = { ruta -> pestanas.firstOrNull { it.ruta == ruta }?.etiqueta ?: "" }) { padding ->
        NavHost(navController, startDestination = Route.FUNDACION, modifier = Modifier.padding(padding)) {
            composable(Route.FUNDACION) { Pendiente("Resumen") }
            destinosComunes(navController, onCerrarSesion)
        }
    }
}
