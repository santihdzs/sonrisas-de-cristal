package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import mx.tec.sonrisas.domain.Credenciales

/** vive a nivel de la app del rol para que el dialogo sobreviva a la navegacion y a la rotacion */
class CredencialesViewModel : ViewModel() {
    var pendientes by mutableStateOf<List<Credenciales>>(emptyList())
        private set

    fun mostrar(nuevas: List<Credenciales>) {
        if (nuevas.isNotEmpty()) pendientes = pendientes + nuevas
    }

    fun cerrar() {
        pendientes = emptyList()
    }
}
