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
fun EducadoraApp(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val pestanas = listOf(
        Pestana(Route.SALA, "Sala", R.drawable.ic_groups, R.drawable.ic_groups_fill),
        Pestana(Route.FOTOS_SALA, "Fotos", R.drawable.ic_photo_library, R.drawable.ic_photo_library_fill),
        Pestana(Route.AVISOS, "Avisos", R.drawable.ic_campaign, R.drawable.ic_campaign_fill),
    )
    ScaffoldRol(navController, pestanas, snackbar, titulo = { ruta -> pestanas.firstOrNull { it.ruta == ruta }?.etiqueta ?: "" }) { padding ->
        NavHost(navController, startDestination = Route.SALA, modifier = Modifier.padding(padding)) {
            composable(Route.SALA) { Pendiente("Sala") }
            composable(Route.FOTOS_SALA) { Pendiente("Fotos") }
            composable(Route.AVISOS) { Pendiente("Avisos") }
            destinosComunes(navController, onCerrarSesion)
        }
    }
}
