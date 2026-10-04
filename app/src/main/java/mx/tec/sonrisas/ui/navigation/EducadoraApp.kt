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
import mx.tec.sonrisas.ui.screens.comun.AvisosViewModel
import mx.tec.sonrisas.ui.screens.educadora.AsistenciaViewModel
import mx.tec.sonrisas.ui.screens.fotos.AgregarFotoScreen
import mx.tec.sonrisas.ui.screens.fotos.AgregarFotoViewModel
import mx.tec.sonrisas.ui.screens.fotos.FotosSalaViewModel
import mx.tec.sonrisas.ui.screens.fotos.GaleriaFotos
import mx.tec.sonrisas.ui.screens.educadora.FichaScreen
import mx.tec.sonrisas.ui.screens.educadora.FichaViewModel
import mx.tec.sonrisas.ui.screens.educadora.HojasAsistencia
import mx.tec.sonrisas.ui.screens.educadora.IncidenteScreen
import mx.tec.sonrisas.ui.screens.educadora.IncidenteViewModel
import mx.tec.sonrisas.ui.screens.educadora.RegistroGrupalScreen
import mx.tec.sonrisas.ui.screens.educadora.RegistroGrupalViewModel
import mx.tec.sonrisas.ui.screens.educadora.SalaScreen
import mx.tec.sonrisas.ui.screens.educadora.SalaViewModel
import mx.tec.sonrisas.ui.state.UiState

@Composable
fun EducadoraApp(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val mensajero = rememberMensajero(snackbar)
    val avisosVm: AvisosViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val noLeidos by avisosVm.noLeidos.collectAsStateWithLifecycle()
    val pestanas = listOf(
        Pestana(Route.SALA, "Sala", R.drawable.ic_groups, R.drawable.ic_groups_fill),
        Pestana(Route.FOTOS_SALA, "Fotos", R.drawable.ic_photo_library, R.drawable.ic_photo_library_fill),
        Pestana(Route.AVISOS, "Avisos", R.drawable.ic_campaign, R.drawable.ic_campaign_fill, insignia = noLeidos),
    )
    ScaffoldRol(navController, pestanas, snackbar, titulo = ::tituloEducadora) { padding ->
        NavHost(navController, startDestination = Route.SALA, modifier = Modifier.padding(padding)) {
            composable(Route.SALA) {
                val vm: SalaViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val asistenciaVm: AsistenciaViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> SalaScreen(
                        datos = e.datos,
                        filtro = vm.filtro,
                        onFiltro = vm::cambiarFiltro,
                        onNino = { navController.navigate(Route.ficha(it.id)) },
                        onLlegada = asistenciaVm::abrirLlegada,
                        onSalida = asistenciaVm::abrirSalida,
                        onRegistroGrupal = { navController.navigate(Route.REGISTRO_GRUPAL) },
                        onObjetos = { navController.navigate(Route.objetos(e.datos.sala.id)) },
                    )
                }
                HojasAsistencia(
                    hoja = asistenciaVm.hoja,
                    onCambiarLlegada = asistenciaVm::cambiarLlegada,
                    onCambiarSalida = asistenciaVm::cambiarSalida,
                    onGuardar = { asistenciaVm.guardar { mensajero.mostrar(it) } },
                    onCerrar = asistenciaVm::cerrar,
                )
            }
            composable(Route.FICHA) {
                val vm: FichaViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val asistenciaVm: AsistenciaViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                val ahora by vm.ahora.collectAsStateWithLifecycle()
                val alGuardar: (String, String) -> Unit = { mensaje, id ->
                    mensajero.mostrar(mensaje, "Deshacer") { vm.borrar(id) }
                }
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> FichaScreen(
                        datos = e.datos,
                        form = vm.form,
                        ahora = ahora,
                        onAbrir = vm::abrir,
                        onCambiar = vm::cambiar,
                        onCerrarHoja = vm::cerrar,
                        onGuardar = { vm.guardar(alGuardar) },
                        onEmpezarSiesta = { vm.empezarSiesta(alGuardar) },
                        onTerminarSiesta = { id -> vm.terminarSiesta(id) { mensajero.mostrar(it) } },
                        onBorrar = { id -> vm.borrar(id) { mensajero.mostrar(it) } },
                        onIncidente = { navController.navigate(Route.incidente(e.datos.nino.id)) },
                        onLlegada = { asistenciaVm.abrirLlegada(e.datos.nino) },
                        onSalida = { asistenciaVm.abrirSalida(e.datos.nino, it) },
                    )
                }
                HojasAsistencia(
                    hoja = asistenciaVm.hoja,
                    onCambiarLlegada = asistenciaVm::cambiarLlegada,
                    onCambiarSalida = asistenciaVm::cambiarSalida,
                    onGuardar = { asistenciaVm.guardar { mensajero.mostrar(it) } },
                    onCerrar = asistenciaVm::cerrar,
                )
            }
            composable(Route.REGISTRO_GRUPAL) {
                val vm: RegistroGrupalViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> RegistroGrupalScreen(
                        datos = e.datos,
                        form = vm.form,
                        onCambiar = vm::cambiar,
                        onGuardar = {
                            vm.guardar(e.datos.presentes) {
                                mensajero.mostrar(it)
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }
            composable(Route.INCIDENTE) {
                val vm: IncidenteViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> IncidenteScreen(
                        nino = e.datos.nino,
                        estrella = e.datos.sala.estrella,
                        form = vm.form,
                        onCambiar = vm::cambiar,
                        onGuardar = {
                            vm.guardar {
                                mensajero.mostrar(it)
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }
            composable(Route.FOTOS_SALA) {
                val vm: FotosSalaViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> GaleriaFotos(
                        datos = e.datos,
                        onFoto = { navController.navigate(Route.foto(it.id)) },
                        onAgregar = { navController.navigate(Route.AGREGAR_FOTO) },
                    )
                }
            }
            composable(Route.AGREGAR_FOTO) {
                val vm: AgregarFotoViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> AgregarFotoScreen(
                        datos = e.datos,
                        form = vm.form,
                        onCambiar = vm::cambiar,
                        onUso = { vm.cambiarUso(it, e.datos.ninos) },
                        onAlternar = vm::alternar,
                        onGuardar = {
                            vm.guardar {
                                mensajero.mostrar(it)
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }
            destinoFoto(navController, mensajero)
            destinoObjetos(mensajero)
            destinoAvisos(alSalir = avisosVm::marcarLeidos)
            destinosComunes(navController, onCerrarSesion)
        }
    }
}

private fun tituloEducadora(ruta: String?): String = tituloComun(ruta) ?: when (ruta) {
    Route.SALA -> "Mi sala"
    Route.FICHA -> "Ficha del día"
    Route.REGISTRO_GRUPAL -> "Registro grupal"
    Route.INCIDENTE -> "Registrar incidente"
    Route.FOTOS_SALA -> "Fotos de la sala"
    Route.AGREGAR_FOTO -> "Agregar foto"
    else -> ""
}
