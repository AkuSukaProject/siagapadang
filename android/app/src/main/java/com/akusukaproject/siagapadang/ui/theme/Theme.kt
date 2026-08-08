package com.akusukaproject.siagapadang.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SiagaPadangColors = darkColorScheme(
    primary = Color(0xFFFFC857),
    onPrimary = Color(0xFF152025),
    background = Color(0xFF071A20),
    onBackground = Color.White,
    surface = Color(0xFF0B252D),
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

