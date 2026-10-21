package mx.tec.sonrisas.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import mx.tec.sonrisas.SonrisasApplication
import mx.tec.sonrisas.data.AppContainer
import mx.tec.sonrisas.ui.screens.comun.AvisosViewModel
import mx.tec.sonrisas.ui.screens.comun.LoginViewModel
import mx.tec.sonrisas.ui.screens.direccion.ActividadViewModel
import mx.tec.sonrisas.ui.screens.direccion.EstanciaViewModel
import mx.tec.sonrisas.ui.screens.direccion.ExpedienteViewModel
import mx.tec.sonrisas.ui.screens.direccion.FundacionViewModel
import mx.tec.sonrisas.ui.screens.direccion.HorariosViewModel
import mx.tec.sonrisas.ui.screens.direccion.InicioViewModel
import mx.tec.sonrisas.ui.screens.direccion.InscripcionViewModel
import mx.tec.sonrisas.ui.screens.direccion.NinosViewModel
import mx.tec.sonrisas.ui.screens.direccion.PublicarAvisoViewModel
import mx.tec.sonrisas.ui.screens.direccion.ReporteViewModel
import mx.tec.sonrisas.ui.screens.direccion.SalasViewModel
import mx.tec.sonrisas.ui.screens.direccion.PersonalViewModel
import mx.tec.sonrisas.ui.screens.educadora.AsistenciaViewModel
import mx.tec.sonrisas.ui.screens.educadora.FichaViewModel
import mx.tec.sonrisas.ui.screens.educadora.IncidenteViewModel
import mx.tec.sonrisas.ui.screens.educadora.RegistroGrupalViewModel
import mx.tec.sonrisas.ui.screens.educadora.SalaViewModel
import mx.tec.sonrisas.ui.screens.comun.PerfilViewModel
import mx.tec.sonrisas.ui.screens.familia.FamiliaViewModel
import mx.tec.sonrisas.ui.screens.familia.IncidenteDetalleViewModel
import mx.tec.sonrisas.ui.screens.fotos.AgregarFotoViewModel
import mx.tec.sonrisas.ui.screens.fotos.FotoDetalleViewModel
import mx.tec.sonrisas.ui.screens.fotos.FotosSalaViewModel
import mx.tec.sonrisas.ui.screens.sala.ObjetosViewModel
import mx.tec.sonrisas.ui.screens.bitacora.BitacoraViewModel
import mx.tec.sonrisas.ui.screens.sala.MaterialViewModel
import mx.tec.sonrisas.ui.screens.mensajes.ChatViewModel
import mx.tec.sonrisas.ui.screens.mensajes.ConversacionesViewModel
import mx.tec.sonrisas.ui.screens.mensajes.NuevoMensajeViewModel
import mx.tec.sonrisas.ui.state.CredencialesViewModel
import mx.tec.sonrisas.ui.state.SesionViewModel

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val c = contenedor()
            SesionViewModel(c.sesionRepository) { c.estanciasRepository }
        }
        initializer { LoginViewModel(contenedor().sesionRepository) }
        initializer {
            val c = contenedor()
            PerfilViewModel(c.sesionRepository, c.estanciasRepository, c.ninosRepository)
        }
        initializer { AvisosViewModel(contenedor().avisosRepository) }
        initializer {
            val c = contenedor()
            SalaViewModel(c.sesionRepository, c.estanciasRepository, c.ninosRepository, c.jornadaRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            AsistenciaViewModel(c.jornadaRepository, c.incidentesRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            FichaViewModel(
                createSavedStateHandle(), c.sesionRepository, c.estanciasRepository, c.ninosRepository,
                c.jornadaRepository, c.incidentesRepository, c.reloj,
            )
        }
        initializer {
            val c = contenedor()
            RegistroGrupalViewModel(c.sesionRepository, c.estanciasRepository, c.ninosRepository, c.jornadaRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            IncidenteViewModel(createSavedStateHandle(), c.estanciasRepository, c.ninosRepository, c.incidentesRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            FotosSalaViewModel(c.sesionRepository, c.estanciasRepository, c.fotosRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            AgregarFotoViewModel(c.sesionRepository, c.estanciasRepository, c.ninosRepository, c.jornadaRepository, c.fotosRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            FotoDetalleViewModel(createSavedStateHandle(), c.sesionRepository, c.estanciasRepository, c.ninosRepository, c.fotosRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            FamiliaViewModel(
                c.estanciasRepository, c.ninosRepository, c.jornadaRepository, c.incidentesRepository,
                c.fotosRepository, c.horariosRepository, c.objetosRepository, c.reloj,
            )
        }
        initializer {
            val c = contenedor()
            IncidenteDetalleViewModel(createSavedStateHandle(), c.estanciasRepository, c.ninosRepository, c.incidentesRepository)
        }
        initializer {
            val c = contenedor()
            InicioViewModel(c.estanciasRepository, c.ninosRepository, c.jornadaRepository, c.incidentesRepository, c.avisosRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            NinosViewModel(c.estanciasRepository, c.ninosRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            ExpedienteViewModel(createSavedStateHandle(), c.estanciasRepository, c.ninosRepository, c.cuentasRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            InscripcionViewModel(createSavedStateHandle(), c.estanciasRepository, c.ninosRepository, c.cuentasRepository, c.reloj)
        }
        initializer { PublicarAvisoViewModel(contenedor().avisosRepository) }
        initializer {
            val c = contenedor()
            ReporteViewModel(c.sesionRepository, c.estanciasRepository, c.jornadaRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            HorariosViewModel(c.estanciasRepository, c.horariosRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            ActividadViewModel(c.actividadRepository, c.ninosRepository, c.reloj)
        }
        initializer { FundacionViewModel(contenedor().estanciasRepository) }
        initializer { CredencialesViewModel() }
        initializer { ConversacionesViewModel(contenedor().mensajesRepository, contenedor().reloj) }
        initializer { NuevoMensajeViewModel(contenedor().mensajesRepository) }
        initializer {
            val c = contenedor()
            BitacoraViewModel(c.estanciasRepository, c.ninosRepository, c.jornadaRepository, c.incidentesRepository, c.carpetaBitacoras, c.reloj)
        }
        initializer {
            val c = contenedor()
            MaterialViewModel(createSavedStateHandle(), c.sesionRepository, c.estanciasRepository, c.materialRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            ChatViewModel(createSavedStateHandle(), c.mensajesRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            ObjetosViewModel(createSavedStateHandle(), c.sesionRepository, c.estanciasRepository, c.objetosRepository, c.reloj)
        }
        initializer {
            val c = contenedor()
            EstanciaViewModel(createSavedStateHandle(), c.estanciasRepository, c.cuentasRepository)
        }
        initializer {
            val c = contenedor()
            SalasViewModel(c.estanciasRepository, c.ninosRepository)
        }
        initializer {
            val c = contenedor()
            PersonalViewModel(c.estanciasRepository, c.cuentasRepository)
        }
    }
}

fun CreationExtras.contenedor(): AppContainer = (this[APPLICATION_KEY] as SonrisasApplication).container
