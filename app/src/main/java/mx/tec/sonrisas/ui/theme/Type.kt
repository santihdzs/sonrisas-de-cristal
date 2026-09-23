package mx.tec.sonrisas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import mx.tec.sonrisas.R

private val proveedor = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val figtree = GoogleFont("Figtree")
private val robotoFlex = GoogleFont("Roboto Flex")
private val pesos = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

// si figtree no se descarga, compose prueba roboto flex y luego la fuente del sistema
val Figtree = FontFamily(
    pesos.map { Font(googleFont = figtree, fontProvider = proveedor, weight = it) } +
        pesos.map { Font(googleFont = robotoFlex, fontProvider = proveedor, weight = it) }
)

private fun estilo(tamano: Int, linea: Int, peso: FontWeight) = TextStyle(
    fontFamily = Figtree,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = linea.sp,
)

private val base = Typography()

val SonrisasTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Figtree),
    displayMedium = base.displayMedium.copy(fontFamily = Figtree),
    displaySmall = estilo(32, 40, FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Figtree, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Figtree, fontWeight = FontWeight.Bold),
    headlineSmall = estilo(24, 32, FontWeight.Bold),
    titleLarge = estilo(20, 28, FontWeight.SemiBold),
    titleMedium = estilo(16, 24, FontWeight.SemiBold),
    titleSmall = estilo(14, 20, FontWeight.SemiBold),
    bodyLarge = estilo(16, 24, FontWeight.Normal),
    bodyMedium = estilo(14, 20, FontWeight.Normal),
    bodySmall = estilo(12, 16, FontWeight.Medium),
    labelLarge = estilo(14, 20, FontWeight.SemiBold),
    labelMedium = estilo(12, 16, FontWeight.Medium),
    labelSmall = estilo(11, 16, FontWeight.Medium),
)
