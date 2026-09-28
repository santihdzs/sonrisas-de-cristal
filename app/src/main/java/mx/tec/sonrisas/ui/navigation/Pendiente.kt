package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.Composable
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.components.EstadoVacio

@Composable
fun Pendiente(nombre: String) {
    EstadoVacio(R.drawable.ic_hourglass_empty, nombre, mensaje = "Pantalla en construcción.")
}
