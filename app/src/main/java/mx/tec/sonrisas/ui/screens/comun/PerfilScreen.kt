package mx.tec.sonrisas.ui.screens.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.ui.components.AvatarNino
import mx.tec.sonrisas.ui.components.EstrellaSala
import mx.tec.sonrisas.ui.components.Icono
import mx.tec.sonrisas.ui.components.TarjetaSeccion
import mx.tec.sonrisas.ui.components.etiqueta
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import java.time.LocalDate

@Composable
fun PerfilScreen(
    datos: PerfilDatos,
    onCambiarHuella: (Boolean) -> Unit,
    onAcercaDe: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
            Icono(iconoRol(datos.rol), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(datos.nombre, style = MaterialTheme.typography.headlineSmall)
                Text("${datos.rol.etiqueta} · ${datos.usuario}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (datos.rol != Rol.FAMILIAR) {
            TarjetaSeccion(titulo = "Dónde trabajas", icono = R.drawable.ic_apartment) {
                Text(datos.estancia ?: "Todas las estancias de la fundación", style = MaterialTheme.typography.bodyLarge)
                datos.sala?.let { EstrellaSala("Sala ${it.nombre}", it.estrella) }
            }
        } else {
            TarjetaSeccion(titulo = if (datos.hijos.size == 1) "Tu hijo o hija" else "Tus hijos", icono = R.drawable.ic_child_care) {
                datos.hijos.forEach { (nino, sala) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                        AvatarNino(nino.nombre, nino.apellidos, sala?.estrella ?: ColorEstrella.AZUL, tamano = 40.dp)
                        Column {
                            Text(nino.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                            sala?.let { Text("Sala ${it.nombre}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                datos.estancia?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        if (datos.puedeUsarHuella) {
            TarjetaSeccion {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(value = datos.huella, role = Role.Switch, onValueChange = onCambiarHuella),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.space3),
                ) {
                    Icono(R.drawable.ic_fingerprint, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f)) {
                        Text("Desbloqueo con huella", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "La app se bloquea al salir de ella o tras 5 minutos sin uso. Útil si la tableta de la sala es compartida.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = datos.huella, onCheckedChange = null)
                }
            }
        }
        TarjetaSeccion(onClick = onAcercaDe) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.space3)) {
                Icono(R.drawable.ic_info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Acerca de", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icono(R.drawable.ic_chevron_right, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = Espacio.space2))
        OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icono(R.drawable.ic_logout, null)
            Text("Cerrar sesión", modifier = Modifier.padding(start = Espacio.space2))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PerfilPreview() {
    val sala = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
    SonrisasTheme {
        PerfilScreen(
            datos = PerfilDatos("Mariana López Hernández", "educadora", Rol.EDUCADORA, "Estancia San Bernabé", sala, emptyList(), true),
            onCambiarHuella = {}, onAcercaDe = {}, onCerrarSesion = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PerfilFamiliaPreview() {
    val sala = Sala("s", "e", "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
    val nino = Nino("n", "Sofía", "Ramírez Treviño", LocalDate.of(2024, 4, 1), "e", "s", emptyList(), emptyList())
    SonrisasTheme {
        PerfilScreen(
            datos = PerfilDatos("Laura Treviño Garza", "familia", Rol.FAMILIAR, "Estancia San Bernabé", null, listOf(nino to sala), false),
            onCambiarHuella = {}, onAcercaDe = {}, onCerrarSesion = {},
        )
    }
}
