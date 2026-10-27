package dev.ofarafal.enklawa.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Color(0xFF7FDBCA),
    onPrimary = Color(0xFF00201B),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFE4E8EC),
    surface = Color(0xFF131A22),
    onSurface = Color(0xFFE4E8EC),
    surfaceVariant = Color(0xFF1C2530),
    onSurfaceVariant = Color(0xFFA9B4BF),
    error = Color(0xFFFF8A80),
)

@Composable
fun EnklawaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
