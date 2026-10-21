package mx.tec.sonrisas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// el proyecto no trae Inter como recurso, asi que se usa la fuente del sistema (sin descargas)
private val Fuente = FontFamily.Default

private fun estilo(tamano: Int, linea: Int, peso: FontWeight, espaciado: Double = 0.0) = TextStyle(
    fontFamily = Fuente,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = linea.sp,
    letterSpacing = espaciado.sp,
)

private val base = Typography()

val SonrisasTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Fuente),
    displayMedium = base.displayMedium.copy(fontFamily = Fuente),
    displaySmall = estilo(32, 40, FontWeight.Bold),
    headlineLarge = estilo(30, 38, FontWeight.Bold),
    // titulo de pantalla
    headlineMedium = estilo(28, 36, FontWeight.Bold),
    headlineSmall = estilo(24, 32, FontWeight.Bold),
    titleLarge = estilo(20, 28, FontWeight.SemiBold),
    titleMedium = estilo(16, 24, FontWeight.SemiBold),
    titleSmall = estilo(14, 20, FontWeight.SemiBold),
    bodyLarge = estilo(16, 24, FontWeight.Normal),
    bodyMedium = estilo(14, 20, FontWeight.Normal),
    bodySmall = estilo(12, 16, FontWeight.Normal),
    labelLarge = estilo(14, 20, FontWeight.SemiBold),
    labelMedium = estilo(12, 16, FontWeight.Medium),
    labelSmall = estilo(11, 16, FontWeight.Medium),
)

/** etiqueta de seccion: mayusculas pequenas, semibold, gris, con un poco de espaciado */
val EstiloEtiquetaSeccion = estilo(12, 16, FontWeight.SemiBold, espaciado = 1.0)
