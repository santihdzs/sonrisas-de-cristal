package mx.tec.sonrisas.ui.components

import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.theme.Elevacion
import mx.tec.sonrisas.ui.theme.Espacio
import mx.tec.sonrisas.ui.theme.Radio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

/**
 * patron A: bloque teal pegado a los bordes y arriba, detras de la barra de estado, con esquinas
 * inferiores de 32. Dibuja su propio margen de la barra de estado, asi que la pantalla no debe
 * agregar otro arriba. Todo lo de adentro sale en blanco.
 */
@Composable
fun EncabezadoTeal(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit = {},
) {
    IconosClarosEnBarraDeEstado()
    val forma = RoundedCornerShape(bottomStart = Radio.encabezado, bottomEnd = Radio.encabezado)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sombraSuave(Elevacion.tarjeta * 2, forma)
            .clip(forma)
            .background(degradadoTeal())
            .statusBarsPadding()
            .padding(start = Espacio.space5, end = Espacio.space3, top = Espacio.space1, bottom = Espacio.space4),
        verticalArrangement = Arrangement.spacedBy(Espacio.space2),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                accion?.invoke()
            }
            contenido()
        }
    }
}

/** mientras el encabezado teal esta en pantalla, la hora y la bateria se dibujan en blanco */
@Composable
private fun IconosClarosEnBarraDeEstado() {
    val ventana = LocalActivity.current?.window ?: return
    val vista = LocalView.current
    DisposableEffect(ventana, vista) {
        val controlador = WindowCompat.getInsetsController(ventana, vista)
        val antes = controlador.isAppearanceLightStatusBars
        controlador.isAppearanceLightStatusBars = false
        onDispose { controlador.isAppearanceLightStatusBars = antes }
    }
}

/** patron B: titulo de pestaña sobre el fondo claro, con subtitulo opcional y accion a la derecha */
@Composable
fun TituloPantalla(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    onRegresar: (() -> Unit)? = null,
    accion: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = if (onRegresar != null) Espacio.space1 else Espacio.space4, end = Espacio.space4, top = Espacio.space2, bottom = Espacio.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // flecha opcional para cuando el titulo grande se usa fuera de una pestaña (por ejemplo Mi perfil)
        if (onRegresar != null) {
            IconButton(onClick = onRegresar) { Icono(R.drawable.ic_arrow_back, "Regresar") }
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        accion?.invoke()
    }
}

/**
 * patron C: barra superior clara con flecha de regreso y titulo. Tambien la usa ScaffoldRol
 * mientras cada pestaña no tenga su propio encabezado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraDetalle(
    titulo: String,
    modifier: Modifier = Modifier,
    onRegresar: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                titulo,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            if (onRegresar != null) {
                IconButton(onClick = onRegresar) { Icono(R.drawable.ic_arrow_back, "Regresar") }
            }
        },
        actions = acciones,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

/**
 * boton circular con icono. Sobre teal es un circulo blanco translucido; sobre el fondo claro,
 * un circulo blanco con sombra suave.
 */
@Composable
fun BotonCircular(
    @DrawableRes icono: Int,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sobreTeal: Boolean = false,
) {
    val forma = CircleShape
    Surface(
        onClick = onClick,
        shape = forma,
        color = if (sobreTeal) Color.White.copy(alpha = 0.18f) else SonrisasTheme.colors.superficieElevada,
        contentColor = if (sobreTeal) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = (if (sobreTeal) modifier else modifier.sombraSuave(Elevacion.tarjeta, forma)).size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icono(icono, descripcion, Modifier.size(26.dp))
        }
    }
}

/** boton circular de perfil (arriba a la derecha) */
@Composable
fun BotonPerfil(onClick: () -> Unit, modifier: Modifier = Modifier, sobreTeal: Boolean = false) {
    BotonCircular(R.drawable.ic_account_circle, "Mi perfil", onClick, modifier, sobreTeal)
}

/**
 * chip translucido para usar dentro de un bloque teal, como la sala en el encabezado de Mi sala.
 * [inicio] es lo que va antes del texto (un icono o la estrella de la sala).
 */
@Composable
fun ChipTranslucido(texto: String, modifier: Modifier = Modifier, inicio: (@Composable () -> Unit)? = null) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f), contentColor = Color.White, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = Espacio.space3, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            inicio?.invoke()
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** barra de progreso delgada y redondeada; por defecto blanca, para el encabezado teal */
@Composable
fun ProgresoDelgado(
    fraccion: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    pista: Color = Color.White.copy(alpha = 0.25f),
) {
    val valor = fraccion.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(pista)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(valor, 0f..1f) },
    ) {
        Box(Modifier.fillMaxWidth(valor).fillMaxHeight().clip(CircleShape).background(color))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5F7)
@Composable
private fun EncabezadosPreview() {
    SonrisasTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.space4)) {
            EncabezadoTeal("Mi sala", accion = { BotonPerfil({}, sobreTeal = true) }) {
                ChipTranslucido("Sala de ejemplo", inicio = { Icono(R.drawable.ic_star_fill, null, Modifier.size(16.dp)) })
                Text("Lunes 5 de octubre", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                Text("9 de 14 presentes", style = MaterialTheme.typography.headlineSmall)
                ProgresoDelgado(9 / 14f)
            }
            TituloPantalla("Niños", subtitulo = "Estancia de ejemplo", accion = { BotonPerfil({}) })
            TarjetaTeal(Modifier.padding(horizontal = Espacio.space4)) {
                Text("34 niños activos", style = MaterialTheme.typography.titleLarge)
            }
            BarraDetalle("Horarios", onRegresar = {})
        }
    }
}
