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
fun DireccionApp(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val pestanas = listOf(
        Pestana(Route.INICIO, "Inicio", R.drawable.ic_home, R.drawable.ic_home_fill),
        Pestana(Route.NINOS, "Niños", R.drawable.ic_child_care, R.drawable.ic_child_care_fill),
        Pestana(Route.AVISOS, "Avisos", R.drawable.ic_campaign, R.drawable.ic_campaign_fill),
        Pestana(Route.MAS, "Más", R.drawable.ic_more_horiz, R.drawable.ic_more_horiz_fill),
    )
    ScaffoldRol(navController, pestanas, snackbar, titulo = { ruta -> pestanas.firstOrNull { it.ruta == ruta }?.etiqueta ?: "" }) { padding ->
        NavHost(navController, startDestination = Route.INICIO, modifier = Modifier.padding(padding)) {
            composable(Route.INICIO) { Pendiente("Inicio") }
            composable(Route.NINOS) { Pendiente("Niños") }
            composable(Route.AVISOS) { Pendiente("Avisos") }
            composable(Route.MAS) { Pendiente("Más") }
            destinosComunes(navController, onCerrarSesion)
        }
    }
}
