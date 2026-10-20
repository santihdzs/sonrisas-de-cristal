package mx.tec.sonrisas.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.ui.theme.ParEstrella
import mx.tec.sonrisas.ui.theme.SonrisasTheme

// acentos por tipo de evento (DISENO.md, seccion 2): naranja alimento, azul siesta,
// verde pañal, amarillo animo, gris azulado llegada y salida

enum class TipoTono { ALIMENTO, SIESTA, PANAL, ANIMO, LLEGADA_SALIDA, NOTA, INCIDENTE }

val EventoBitacora.tipoTono: TipoTono
    get() = when (this) {
        is EventoBitacora.Alimento -> TipoTono.ALIMENTO
        is EventoBitacora.Siesta -> TipoTono.SIESTA
        is EventoBitacora.Evacuacion -> TipoTono.PANAL
        is EventoBitacora.Animo -> TipoTono.ANIMO
        is EventoBitacora.Nota -> TipoTono.NOTA
    }

/** fondo suave, icono y texto del tono de cada tipo de evento */
@Composable
@ReadOnlyComposable
fun tono(tipo: TipoTono): ParEstrella {
    val c = SonrisasTheme.colors
    val esquema = MaterialTheme.colorScheme
    return when (tipo) {
        TipoTono.ALIMENTO -> c.alimento
        TipoTono.SIESTA -> c.siesta
        TipoTono.PANAL -> c.panal
        TipoTono.ANIMO -> c.animo
        TipoTono.LLEGADA_SALIDA -> c.llegadaSalida
        TipoTono.NOTA -> c.sinLlegar
        // un incidente se marca con el rojo apagado, no con el rojo vivo de las alertas
        TipoTono.INCIDENTE -> ParEstrella(esquema.error, esquema.errorContainer, esquema.onErrorContainer)
    }
}
