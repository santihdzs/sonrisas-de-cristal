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
fun FamiliarApp(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val pestanas = listOf(
        Pestana(Route.HOY, "Hoy", R.drawable.ic_today, R.drawable.ic_today_fill),
        Pestana(Route.SEMANA, "Semana", R.drawable.ic_date_range, R.drawable.ic_date_range_fill),
        Pestana(Route.FOTOS_FAMILIA, "Fotos", R.drawable.ic_photo_library, R.drawable.ic_photo_library_fill),
        Pestana(Route.AVISOS, "Avisos", R.drawable.ic_campaign, R.drawable.ic_campaign_fill),
    )
    ScaffoldRol(navController, pestanas, snackbar, titulo = { ruta -> pestanas.firstOrNull { it.ruta == ruta }?.etiqueta ?: "" }) { padding ->
        NavHost(navController, startDestination = Route.HOY, modifier = Modifier.padding(padding)) {
            composable(Route.HOY) { Pendiente("Hoy") }
            composable(Route.SEMANA) { Pendiente("Semana") }
            composable(Route.FOTOS_FAMILIA) { Pendiente("Fotos") }
            composable(Route.AVISOS) { Pendiente("Avisos") }
            destinosComunes(navController, onCerrarSesion)
        }
    }
}
