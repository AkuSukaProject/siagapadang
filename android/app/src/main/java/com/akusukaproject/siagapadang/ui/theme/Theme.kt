package com.akusukaproject.siagapadang.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.akusukaproject.siagapadang.R

// Warna identitas. Layar evakuasi memakai latar navy; layar masa tenang memakai latar krem.
val SiagaNavy = Color(0xFF01346D)
val SiagaCream = Color(0xFFFDF3DF)
val SiagaWarning = Color(0xFFF7FF0C)
val SiagaRust = Color(0xFFC6654B)
val SiagaRouteBlue = Color(0xFF007BFA)
val SiagaNextGreen = Color(0xFF58D68D)

// Token desain V3. Kontras teks di atas latarnya minimal 4.5:1 (NF-06).
val SiagaCalmBackground = Color(0xFFF5F2EA)
val SiagaSurface = Color(0xFFFFFFFF)
val SiagaLine = Color(0xFFE3DED2)
val SiagaTextSecondary = Color(0xFF4A5A70)
val SiagaOnNavyMuted = Color(0xFFC9D6E8)
val SiagaSafeGreen = Color(0xFF1E9E5A)
val SiagaRustDeep = Color(0xFFB4502F)
val SiagaTailGray = Color(0xFFEEF2F7)
val SiagaMapBackground = Color(0xFFECE7DC)

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
    // Black dipetakan ke ExtraBold karena berkas Black tidak dibawa.
    Font(R.font.inter_extrabold, FontWeight.Black),
)

private val SiagaTypography = Typography().let { base ->
    fun TextStyle.inter() = copy(fontFamily = InterFamily)
    Typography(
        displayLarge = base.displayLarge.inter(),
        displayMedium = base.displayMedium.inter(),
        displaySmall = base.displaySmall.inter(),
        headlineLarge = base.headlineLarge.inter(),
        headlineMedium = base.headlineMedium.inter(),
        headlineSmall = base.headlineSmall.inter(),
        titleLarge = base.titleLarge.inter(),
        titleMedium = base.titleMedium.inter(),
        titleSmall = base.titleSmall.inter(),
        bodyLarge = base.bodyLarge.inter(),
        bodyMedium = base.bodyMedium.inter(),
        bodySmall = base.bodySmall.inter(),
        labelLarge = base.labelLarge.inter(),
        labelMedium = base.labelMedium.inter(),
        labelSmall = base.labelSmall.inter(),
    )
}

private val SiagaPadangColors = darkColorScheme(
    primary = SiagaWarning,
    onPrimary = SiagaNavy,
    background = SiagaNavy,
    onBackground = Color.White,
    surface = SiagaNavy,
    onSurface = Color.White,
    error = Color(0xFFFF6B6B),
)

@Composable
fun SiagaPadangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SiagaPadangColors,
        typography = SiagaTypography,
        content = content,
    )
}
