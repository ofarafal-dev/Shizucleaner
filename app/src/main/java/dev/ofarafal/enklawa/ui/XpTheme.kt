package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** Paleta w duchu Luna z Windows XP. Wszystkie grafiki są własne (rysowane w kodzie). */
object Xp {
    val TitleActive = listOf(Color(0xFF273142), Color(0xFF1D2531))
    val TitleInactive = listOf(Color(0xFF1A202A), Color(0xFF161B24))
    val Frame = Color(0xFF3A465A)
    val FrameInactive = Color(0xFF262E3B)
    val Body = Color(0xFF10151C)
    val BodyBorder = Color(0xFF2A3342)
    val Link = Color(0xFF7FDBCA)
    val Taskbar = listOf(Color(0xF2171D27), Color(0xF2111620))
    val StartGreen = listOf(Color(0xFF7FDBCA), Color(0xFF3AA6C8))
    val Tray = listOf(Color(0x00000000), Color(0x00000000))
    val MenuHeader = listOf(Color(0xFF223049), Color(0xFF1A2434))
    val MenuRight = Color(0xFF161D28)
    val Panel = Color(0xFF131A24)
    val Accent = Color(0xFF7FDBCA)
}

/** Motyw treści okien: ciemny, spójny z powłoką. */
@Composable
fun XpLightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7FDBCA),
            onPrimary = Color(0xFF00201B),
            background = Xp.Body,
            onBackground = Color(0xFFE4E8EC),
            surface = Xp.Body,
            onSurface = Color(0xFFE4E8EC),
            surfaceVariant = Color(0xFF1C2530),
            onSurfaceVariant = Color(0xFFA9B4BF),
            error = Color(0xFFFF8A80),
        ),
        content = content,
    )
}

/** Tapeta: łąka i niebo z chmurami, narysowana w kodzie (własna, nie jest zdjęciem z żadnego systemu). */
@Composable
fun Wallpaper(modifier: Modifier = Modifier.fillMaxSize()) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFF0B1220),
                0.6f to Color(0xFF0F1A2E),
                1f to Color(0xFF07090F),
            ),
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x557FDBCA), Color(0x007FDBCA)),
                center = Offset(w * 0.2f, h * 0.18f), radius = w * 0.75f,
            ),
            radius = w * 0.75f, center = Offset(w * 0.2f, h * 0.18f),
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x449A7BFF), Color(0x009A7BFF)),
                center = Offset(w * 0.9f, h * 0.7f), radius = w * 0.9f,
            ),
            radius = w * 0.9f, center = Offset(w * 0.9f, h * 0.7f),
        )
        val wave = Path().apply {
            moveTo(0f, h * 0.82f)
            cubicTo(w * 0.3f, h * 0.74f, w * 0.65f, h * 0.9f, w, h * 0.78f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(wave, Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x00FFFFFF)), startY = h * 0.75f, endY = h))
    }
}
