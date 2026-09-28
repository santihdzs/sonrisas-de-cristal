package mx.tec.sonrisas.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import mx.tec.sonrisas.SonrisasApplication
import mx.tec.sonrisas.data.AppContainer
import mx.tec.sonrisas.ui.screens.comun.LoginViewModel
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
    }
}

fun CreationExtras.contenedor(): AppContainer = (this[APPLICATION_KEY] as SonrisasApplication).container
