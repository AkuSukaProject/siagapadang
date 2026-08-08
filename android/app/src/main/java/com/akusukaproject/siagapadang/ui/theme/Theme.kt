package com.akusukaproject.siagapadang.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SiagaNavy = Color(0xFF003049)
val SiagaCream = Color(0xFFFDF3DF)
val SiagaWarning = Color(0xFFF7FF0C)
val SiagaRust = Color(0xFFC6654B)
val SiagaRouteBlue = Color(0xFF7A7FFF)
val SiagaNextGreen = Color(0xFF58D68D)

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
        content = content,
    )
}
