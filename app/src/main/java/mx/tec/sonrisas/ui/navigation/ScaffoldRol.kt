package mx.tec.sonrisas.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.components.Icono

/** un destino puede poner aqui su titulo cuando depende de los datos, como el nombre de una conversacion */
const val TITULO_DINAMICO = "titulo"

private val ANCHO_MAXIMO = 840.dp

data class Pestana(
    val ruta: String,
    val etiqueta: String,
    @DrawableRes val icono: Int,
    @DrawableRes val iconoLleno: Int,
    val insignia: Int = 0,
)

/** muestra snackbars desde cualquier destino del navhost */
class Mensajero(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun mostrar(texto: String, accion: String? = null, alAccion: () -> Unit = {}) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val resultado = host.showSnackbar(
                message = texto,
                actionLabel = accion,
                withDismissAction = accion == null,
                duration = if (accion != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (resultado == SnackbarResult.ActionPerformed) alAccion()
        }
    }
}

@Composable
fun rememberMensajero(host: SnackbarHostState): Mensajero {
    val scope = rememberCoroutineScope()
    return remember(host) { Mensajero(host, scope) }
}

fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldRol(
    navController: NavHostController,
    pestanas: List<Pestana>,
    snackbar: SnackbarHostState,
    titulo: (ruta: String?) -> String,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val entrada by navController.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val ruta = destino?.route
    val esPestana = pestanas.any { it.ruta == ruta }
    val flujoTitulo = remember(entrada) { entrada?.savedStateHandle?.getStateFlow<String?>(TITULO_DINAMICO, null) ?: MutableStateFlow(null) }
    val tituloDinamico by flujoTitulo.collectAsState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(tituloDinamico ?: titulo(ruta), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (!esPestana && navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icono(R.drawable.ic_arrow_back, "Regresar")
                        }
                    }
                },
                actions = {
                    if (ruta != Route.PERFIL && ruta != Route.ACERCA) {
                        IconButton(onClick = { navController.navigate(Route.PERFIL) { launchSingleTop = true } }) {
                            Icono(R.drawable.ic_account_circle, "Mi perfil")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            if (esPestana && pestanas.size > 1) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    pestanas.forEach { p ->
                        val seleccionada = destino?.hierarchy?.any { it.route == p.ruta } == true
                        NavigationBarItem(
                            selected = seleccionada,
                            onClick = { navController.irAPestana(p.ruta) },
                            icon = {
                                BadgedBox(badge = {
                                    if (p.insignia > 0) {
                                        Badge(Modifier.semantics { contentDescription = "${p.insignia} sin leer" }) { Text("${p.insignia}") }
                                    }
                                }) {
                                    Icono(if (seleccionada) p.iconoLleno else p.icono, null)
                                }
                            },
                            label = {
                                // con cinco pestanas y letra al 200 % las etiquetas se cortan; se limitan a 130 %
                                val densidad = LocalDensity.current
                                CompositionLocalProvider(LocalDensity provides Density(densidad.density, minOf(densidad.fontScale, 1.3f))) {
                                    Text(p.etiqueta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        // en una tableta compartida en horizontal, las tarjetas no se estiran de lado a lado
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.fillMaxHeight().widthIn(max = ANCHO_MAXIMO).fillMaxWidth()) { content(padding) }
        }
    }
}
