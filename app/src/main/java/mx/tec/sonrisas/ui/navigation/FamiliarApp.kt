package mx.tec.sonrisas.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.AppViewModelProvider
import mx.tec.sonrisas.ui.components.CargandoView
import mx.tec.sonrisas.ui.components.ErrorView
import mx.tec.sonrisas.ui.screens.comun.AvisosViewModel
import mx.tec.sonrisas.ui.screens.bitacora.BitacoraViewModel
import mx.tec.sonrisas.ui.screens.bitacora.ImpresionBitacora
import mx.tec.sonrisas.ui.screens.bitacora.Objetivo
import mx.tec.sonrisas.ui.screens.mensajes.ConversacionesViewModel
import mx.tec.sonrisas.ui.screens.familia.FamiliaViewModel
import mx.tec.sonrisas.ui.screens.familia.HoyScreen
import mx.tec.sonrisas.ui.screens.familia.SelectorHijos
import mx.tec.sonrisas.ui.screens.familia.SemanaScreen
import mx.tec.sonrisas.ui.screens.familia.SinHijos
import mx.tec.sonrisas.ui.screens.fotos.GaleriaFotos
import mx.tec.sonrisas.ui.state.UiState

@Composable
fun FamiliarApp(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val mensajero = rememberMensajero(snackbar)
    val avisosVm: AvisosViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val noLeidos by avisosVm.noLeidos.collectAsStateWithLifecycle()
    val mensajesVm: ConversacionesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val mensajesNoLeidos by mensajesVm.noLeidos.collectAsStateWithLifecycle()
    val pestanas = listOf(
        Pestana(Route.HOY, "Hoy", R.drawable.ic_today, R.drawable.ic_today_fill),
        Pestana(Route.SEMANA, "Semana", R.drawable.ic_date_range, R.drawable.ic_date_range_fill),
        Pestana(Route.FOTOS_FAMILIA, "Fotos", R.drawable.ic_photo_library, R.drawable.ic_photo_library_fill),
        Pestana(Route.MENSAJES, "Mensajes", R.drawable.ic_chat, R.drawable.ic_chat_fill, insignia = mensajesNoLeidos),
        Pestana(Route.AVISOS, "Avisos", R.drawable.ic_campaign, R.drawable.ic_campaign_fill, insignia = noLeidos),
    )
    ScaffoldRol(navController, pestanas, snackbar, titulo = ::tituloFamiliar) { padding ->
        NavHost(navController, startDestination = Route.FAMILIA, modifier = Modifier.padding(padding)) {
            // hoy, semana y fotos comparten el viewmodel del grafo para que el hijo elegido sea el mismo
            navigation(startDestination = Route.HOY, route = Route.FAMILIA) {
                composable(Route.HOY) { entrada ->
                    val vm = familiaViewModel(navController, entrada)
                    val estado by vm.hoyState.collectAsStateWithLifecycle()
                    when (val e = estado) {
                        UiState.Cargando -> CargandoView()
                        is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                        is UiState.Exito -> e.datos?.let { datos ->
                            HoyScreen(
                                datos = datos,
                                onSeleccion = vm::seleccionar,
                                onIncidente = { navController.navigate(Route.incidenteDetalle(it.id)) },
                                onObjetos = { navController.navigate(Route.objetos(it.id)) },
                                onMaterial = { navController.navigate(Route.material(it.id)) },
                            )
                        } ?: SinHijos()
                    }
                }
                composable(Route.SEMANA) { entrada ->
                    val vm = familiaViewModel(navController, entrada)
                    val bitacoraVm: BitacoraViewModel = viewModel(factory = AppViewModelProvider.Factory)
                    val estado by vm.semanaState.collectAsStateWithLifecycle()
                    val context = LocalContext.current
                    when (val e = estado) {
                        UiState.Cargando -> CargandoView()
                        is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                        is UiState.Exito -> e.datos?.let { datos ->
                            SemanaScreen(
                                datos = datos,
                                onSeleccion = vm::seleccionar,
                                onCompartir = {
                                    compartirTexto(context, "Resumen semanal de ${datos.hijos.seleccionado.nombre}", datos.semana.comoTexto())
                                },
                                onImprimir = { datos.hijos.seleccionado.let { n -> bitacoraVm.abrir(Objetivo.UnNino(n.id, n.nombreCompleto)) } },
                            )
                        } ?: SinHijos()
                    }
                    ImpresionBitacora(bitacoraVm)
                }
                composable(Route.FOTOS_FAMILIA) { entrada ->
                    val vm = familiaViewModel(navController, entrada)
                    val estado by vm.fotosState.collectAsStateWithLifecycle()
                    when (val e = estado) {
                        UiState.Cargando -> CargandoView()
                        is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                        is UiState.Exito -> e.datos?.let { datos ->
                            GaleriaFotos(
                                datos = datos.galeria,
                                onFoto = { navController.navigate(Route.foto(it.id)) },
                                vacioMensaje = "Aquí verás las fotos de las actividades donde aparece ${datos.hijos.seleccionado.nombre}.",
                                encabezado = { SelectorHijos(datos.hijos, vm::seleccionar) },
                            )
                        } ?: SinHijos()
                    }
                }
            }
            destinoAvisos(alSalir = avisosVm::marcarLeidos)
            destinoFoto(navController, mensajero)
            destinoIncidente()
            destinoObjetos(mensajero)
            destinoMaterial(mensajero)
            destinoMensajes(navController, mensajero)
            destinosComunes(navController, onCerrarSesion)
        }
    }
}

@Composable
private fun familiaViewModel(navController: NavHostController, entrada: NavBackStackEntry): FamiliaViewModel {
    val padre = remember(entrada) { navController.getBackStackEntry(Route.FAMILIA) }
    return viewModel(padre, factory = AppViewModelProvider.Factory)
}

private fun tituloFamiliar(ruta: String?): String = tituloComun(ruta) ?: when (ruta) {
    Route.HOY -> "Hoy"
    Route.SEMANA -> "Resumen de la semana"
    Route.FOTOS_FAMILIA -> "Fotos"
    else -> ""
}
