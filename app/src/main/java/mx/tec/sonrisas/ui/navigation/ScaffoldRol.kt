package mx.tec.sonrisas.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import mx.tec.sonrisas.ui.components.BarraDetalle
import mx.tec.sonrisas.ui.components.BarraInferiorFlotante
import mx.tec.sonrisas.ui.components.BotonPerfil
import mx.tec.sonrisas.ui.components.ItemBarraInferior
import mx.tec.sonrisas.ui.theme.Espacio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

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

@Composable
fun ScaffoldRol(
    navController: NavHostController,
    pestanas: List<Pestana>,
    snackbar: SnackbarHostState,
    titulo: (ruta: String?) -> String,
    modifier: Modifier = Modifier,
    /** pestañas que dibujan su propio encabezado teal detras de la barra de estado (patron A): no llevan barra superior */
    rutasConEncabezadoTeal: Set<String> = emptySet(),
    content: @Composable (PaddingValues) -> Unit,
) {
    val entrada by navController.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val ruta = destino?.route
    val esPestana = pestanas.any { it.ruta == ruta }
    val flujoTitulo = remember(entrada) { entrada?.savedStateHandle?.getStateFlow<String?>(TITULO_DINAMICO, null) ?: MutableStateFlow(null) }
    val tituloDinamico by flujoTitulo.collectAsState()
    val encabezadoPropio = ruta in rutasConEncabezadoTeal
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!encabezadoPropio) BarraDetalle(
                titulo = tituloDinamico ?: titulo(ruta),
                onRegresar = if (!esPestana && navController.previousBackStackEntry != null) ({ navController.navigateUp() }) else null,
                acciones = {
                    if (ruta != Route.PERFIL && ruta != Route.ACERCA) {
                        BotonPerfil(
                            onClick = { navController.navigate(Route.PERFIL) { launchSingleTop = true } },
                            modifier = Modifier.padding(end = Espacio.space3),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (esPestana && pestanas.size > 1) {
                BarraInferiorFlotante {
                    pestanas.forEach { p ->
                        ItemBarraInferior(
                            etiqueta = p.etiqueta,
                            icono = p.icono,
                            iconoSeleccionado = p.iconoLleno,
                            seleccionado = destino?.hierarchy?.any { it.route == p.ruta } == true,
                            onClick = { navController.irAPestana(p.ruta) },
                            insignia = p.insignia,
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { relleno ->
        // con encabezado propio, el contenido sube hasta arriba para que el teal quede detras de la barra de estado
        val direccion = LocalLayoutDirection.current
        val padding = if (encabezadoPropio) {
            PaddingValues(
                start = relleno.calculateStartPadding(direccion),
                end = relleno.calculateEndPadding(direccion),
                bottom = relleno.calculateBottomPadding(),
            )
        } else {
            relleno
        }
        // en una tableta compartida en horizontal, las tarjetas no se estiran de lado a lado
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.fillMaxHeight().widthIn(max = ANCHO_MAXIMO).fillMaxWidth()) { content(padding) }
        }
    }
}
