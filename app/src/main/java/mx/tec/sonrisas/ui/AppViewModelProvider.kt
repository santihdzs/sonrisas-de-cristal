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
import mx.tec.sonrisas.ui.screens.educadora.AsistenciaViewModel
import mx.tec.sonrisas.ui.screens.educadora.FichaViewModel
import mx.tec.sonrisas.ui.screens.educadora.IncidenteViewModel
import mx.tec.sonrisas.ui.screens.educadora.RegistroGrupalViewModel
import mx.tec.sonrisas.ui.screens.educadora.SalaViewModel
import mx.tec.sonrisas.ui.screens.comun.PerfilViewModel
import mx.tec.sonrisas.ui.state.SesionViewModel

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { SesionViewModel(contenedor().sesionRepository) }
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
    }
}

fun CreationExtras.contenedor(): AppContainer = (this[APPLICATION_KEY] as SonrisasApplication).container
