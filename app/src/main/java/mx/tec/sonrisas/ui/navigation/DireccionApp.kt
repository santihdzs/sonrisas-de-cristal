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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.AppViewModelProvider
import mx.tec.sonrisas.ui.components.CargandoView
import mx.tec.sonrisas.ui.components.ErrorView
import mx.tec.sonrisas.ui.screens.comun.AvisosViewModel
import mx.tec.sonrisas.ui.screens.mensajes.ConversacionesViewModel
import mx.tec.sonrisas.ui.screens.direccion.ActividadScreen
import mx.tec.sonrisas.ui.screens.direccion.ActividadViewModel
import mx.tec.sonrisas.ui.screens.direccion.ExpedienteScreen
import mx.tec.sonrisas.ui.screens.direccion.ExpedienteViewModel
import mx.tec.sonrisas.ui.screens.direccion.HorariosScreen
import mx.tec.sonrisas.ui.screens.direccion.HorariosViewModel
import mx.tec.sonrisas.ui.screens.direccion.InicioScreen
import mx.tec.sonrisas.ui.screens.direccion.InicioViewModel
import mx.tec.sonrisas.ui.screens.direccion.InscripcionScreen
import mx.tec.sonrisas.ui.screens.direccion.InscripcionViewModel
import mx.tec.sonrisas.ui.screens.direccion.MasScreen
import mx.tec.sonrisas.ui.screens.direccion.NinosScreen
import mx.tec.sonrisas.ui.screens.direccion.NinosViewModel
import mx.tec.sonrisas.ui.screens.direccion.PublicarAvisoScreen
import mx.tec.sonrisas.ui.screens.direccion.PublicarAvisoViewModel
import mx.tec.sonrisas.ui.screens.direccion.ReporteScreen
import mx.tec.sonrisas.ui.screens.direccion.ReporteViewModel
import mx.tec.sonrisas.ui.screens.direccion.HojaSalaForm
import mx.tec.sonrisas.ui.screens.direccion.SalasScreen
import mx.tec.sonrisas.ui.screens.direccion.SalasViewModel
import mx.tec.sonrisas.ui.screens.direccion.HojaPersonalForm
import mx.tec.sonrisas.ui.screens.direccion.PersonalScreen
import mx.tec.sonrisas.ui.screens.direccion.PersonalViewModel
import mx.tec.sonrisas.ui.components.DialogoCredenciales
import mx.tec.sonrisas.ui.state.CredencialesViewModel
import mx.tec.sonrisas.ui.state.UiState

@Composable
fun DireccionApp(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val mensajero = rememberMensajero(snackbar)
    val avisosVm: AvisosViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val credenciales: CredencialesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val noLeidos by avisosVm.noLeidos.collectAsStateWithLifecycle()
    val mensajesVm: ConversacionesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val mensajesNoLeidos by mensajesVm.noLeidos.collectAsStateWithLifecycle()
    val pestanas = listOf(
        Pestana(Route.INICIO, "Inicio", R.drawable.ic_home, R.drawable.ic_home_fill),
        Pestana(Route.NINOS, "Niños", R.drawable.ic_child_care, R.drawable.ic_child_care_fill),
        Pestana(Route.MENSAJES, "Mensajes", R.drawable.ic_chat, R.drawable.ic_chat_fill, insignia = mensajesNoLeidos),
        Pestana(Route.AVISOS, "Avisos", R.drawable.ic_campaign, R.drawable.ic_campaign_fill, insignia = noLeidos),
        Pestana(Route.MAS, "Más", R.drawable.ic_more_horiz, R.drawable.ic_more_horiz_fill),
    )
    ScaffoldRol(navController, pestanas, snackbar, titulo = ::tituloDireccion) { padding ->
        NavHost(navController, startDestination = Route.INICIO, modifier = Modifier.padding(padding)) {
            composable(Route.INICIO) {
                val vm: InicioViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> InicioScreen(
                        datos = e.datos,
                        onIncidente = { navController.navigate(Route.incidenteDetalle(it.id)) },
                        onInscribir = { navController.navigate(Route.inscripcion()) },
                        onPublicarAviso = { navController.navigate(Route.PUBLICAR_AVISO) },
                        onVerAvisos = { navController.irAPestana(Route.AVISOS) },
                        onObjetos = { navController.navigate(Route.objetos(it.id)) },
                    )
                }
            }
            composable(Route.NINOS) {
                val vm: NinosViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> NinosScreen(
                        datos = e.datos,
                        busqueda = vm.busqueda,
                        salaId = vm.salaId,
                        bajas = vm.bajas,
                        onBuscar = vm::buscar,
                        onSala = vm::elegirSala,
                        onBajas = vm::verBajas,
                        onNino = { navController.navigate(Route.expediente(it.id)) },
                        onInscribir = { navController.navigate(Route.inscripcion()) },
                    )
                }
            }
            composable(Route.EXPEDIENTE) {
                val vm: ExpedienteViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> ExpedienteScreen(
                        datos = e.datos,
                        trabajando = vm.trabajando,
                        onEditar = { e.datos?.let { navController.navigate(Route.inscripcion(it.nino.id)) } },
                        onBaja = { vm.darDeBaja(mensajero::mostrar) },
                        onReactivar = { vm.reactivar(mensajero::mostrar) },
                        onEliminar = {
                            vm.eliminar {
                                mensajero.mostrar(it)
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }
            composable(
                Route.INSCRIPCION,
                arguments = listOf(navArgument(Route.ARG_NINO) { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                val vm: InscripcionViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> InscripcionScreen(
                        datos = e.datos,
                        form = vm.form,
                        onCambiar = vm::cambiar,
                        onElegirRegistrado = vm::elegirRegistrado,
                        onGuardar = {
                            vm.guardar { mensaje, id, nuevas ->
                                mensajero.mostrar(mensaje)
                                credenciales.mostrar(nuevas)
                                navController.popBackStack()
                                if (!e.datos.editando) navController.navigate(Route.expediente(id))
                            }
                        },
                    )
                }
            }
            composable(Route.PUBLICAR_AVISO) {
                val vm: PublicarAvisoViewModel = viewModel(factory = AppViewModelProvider.Factory)
                PublicarAvisoScreen(
                    form = vm.form,
                    onCambiar = vm::cambiar,
                    onPublicar = {
                        vm.publicar {
                            mensajero.mostrar(it)
                            navController.popBackStack()
                        }
                    },
                )
            }
            composable(Route.MAS) {
                MasScreen(
                    onAsistencia = { navController.navigate(Route.ASISTENCIA) },
                    onHorarios = { navController.navigate(Route.HORARIOS) },
                    onActividad = { navController.navigate(Route.ACTIVIDAD) },
                    onSalas = { navController.navigate(Route.SALAS) },
                    onPersonal = { navController.navigate(Route.PERSONAL) },
                    onPerfil = { navController.navigate(Route.PERFIL) },
                )
            }
            destinoReporte()
            composable(Route.SALAS) {
                val vm: SalasViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> {
                        SalasScreen(e.datos, onNueva = vm::nueva, onEditar = vm::editar)
                        vm.hoja?.let { h ->
                            HojaSalaForm(
                                hoja = h,
                                errores = vm.errores(h),
                                ocupadas = e.datos.todas.filter { it.id != h.id }.map { it.estrella }.toSet(),
                                onCambiar = vm::cambiar,
                                onGuardar = { vm.guardar(mensajero::mostrar) },
                                onCerrar = vm::cerrar,
                            )
                        }
                    }
                }
            }
            composable(Route.PERSONAL) {
                val vm: PersonalViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> {
                        PersonalScreen(e.datos, onNueva = vm::nueva, onEducadora = vm::abrir)
                        vm.hoja?.let { h ->
                            HojaPersonalForm(
                                hoja = h,
                                salas = e.datos.salas,
                                onCambiar = vm::cambiar,
                                onRegistrar = {
                                    vm.registrar { mensaje, nuevas ->
                                        mensajero.mostrar(mensaje)
                                        credenciales.mostrar(listOf(nuevas))
                                    }
                                },
                                onAsignar = { vm.asignar(mensajero::mostrar) },
                                onRetirar = { vm.retirarAcceso(mensajero::mostrar) },
                                onCerrar = vm::cerrar,
                            )
                        }
                    }
                }
            }
            composable(Route.HORARIOS) {
                val vm: HorariosViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> HorariosScreen(
                        datos = e.datos,
                        hoja = vm.hoja,
                        onSala = vm::elegirSala,
                        onNuevoBloque = vm::nuevoBloque,
                        onEditarBloque = vm::editarBloque,
                        onBorrarBloque = { vm.borrarBloque(it, mensajero::mostrar) },
                        onNuevaEspecial = vm::nuevaEspecial,
                        onBorrarEspecial = { vm.borrarEspecial(it, mensajero::mostrar) },
                        onCambiarHoja = vm::cambiar,
                        onGuardar = { vm.guardar(mensajero::mostrar) },
                        onCerrar = vm::cerrar,
                    )
                }
            }
            composable(Route.ACTIVIDAD) {
                val vm: ActividadViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                when (val e = estado) {
                    UiState.Cargando -> CargandoView()
                    is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
                    is UiState.Exito -> ActividadScreen(e.datos, vm.filtro, vm::cambiarFiltro)
                }
            }
            destinoAvisos(alSalir = avisosVm::marcarLeidos, onPublicar = { navController.navigate(Route.PUBLICAR_AVISO) })
            destinoIncidente()
            destinoObjetos(mensajero)
            destinoMensajes(navController, mensajero)
            destinosComunes(navController, onCerrarSesion)
        }
    }
    DialogoCredenciales(credenciales.pendientes, onCerrar = credenciales::cerrar)
}

/** lo usan la direccion y la fundacion; cada una recibe solo lo que le toca */
fun NavGraphBuilder.destinoReporte() {
    composable(Route.ASISTENCIA) {
        val vm: ReporteViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val estado by vm.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        when (val e = estado) {
            UiState.Cargando -> CargandoView()
            is UiState.Error -> ErrorView(e.mensaje, vm::reintentar)
            is UiState.Exito -> ReporteScreen(
                datos = e.datos,
                onAnterior = vm::mesAnterior,
                onSiguiente = vm::mesSiguiente,
                onSala = vm::elegirSala,
                onEstancia = vm::elegirEstancia,
                onCompartir = {
                    val d = e.datos
                    val estancia = d.estancias.firstOrNull { it.id == d.estanciaId }?.nombre.orEmpty()
                    val sala = d.salas.firstOrNull { it.id == d.salaId }?.let { " · Sala ${it.nombre}" }.orEmpty()
                    compartirTexto(context, "Reporte de asistencia", d.reporte.comoTexto(estancia + sala))
                },
            )
        }
    }
}

private fun tituloDireccion(ruta: String?): String = tituloComun(ruta) ?: when (ruta) {
    Route.INICIO -> "Inicio"
    Route.NINOS -> "Niños"
    Route.MAS -> "Más"
    Route.EXPEDIENTE -> "Expediente"
    Route.INSCRIPCION -> "Expediente del niño"
    Route.PUBLICAR_AVISO -> "Publicar aviso"
    Route.ASISTENCIA -> "Reporte de asistencia"
    Route.HORARIOS -> "Horarios"
    Route.ACTIVIDAD -> "Registro de actividad"
    Route.SALAS -> "Salas"
    Route.PERSONAL -> "Personal"
    else -> ""
}
