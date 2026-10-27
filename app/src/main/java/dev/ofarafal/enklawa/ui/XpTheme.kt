package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** Paleta w duchu Luna z Windows XP. Wszystkie grafiki są własne (rysowane w kodzie). */
object Xp {
    val TitleActive = listOf(Color(0xFF0A5BEE), Color(0xFF3D8DFB), Color(0xFF0A4FD6))
    val TitleInactive = listOf(Color(0xFF7A96DF), Color(0xFF9DB4EC), Color(0xFF7A96DF))
    val Frame = Color(0xFF0831D9)
    val FrameInactive = Color(0xFF6582D4)
    val Body = Color(0xFFECE9D8)
    val BodyBorder = Color(0xFFACA899)
    val Link = Color(0xFF1B4FC4)
    val Taskbar = listOf(Color(0xFF3F86F0), Color(0xFF2461DA), Color(0xFF1B4FC4))
    val StartGreen = listOf(Color(0xFF55BB55), Color(0xFF2E8B2E), Color(0xFF3A9E3A))
    val Tray = listOf(Color(0xFF1AA0F2), Color(0xFF1479D8))
    val MenuHeader = listOf(Color(0xFF1C5BD8), Color(0xFF3E8AF0))
    val MenuRight = Color(0xFFD3E5FA)
}

/** Motyw jasny dla treści okien (beżowe tło, czarny tekst), niezależny od ciemnego motywu powłoki. */
@Composable
fun XpLightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF245EDC),
            onPrimary = Color.White,
            background = Xp.Body,
            onBackground = Color.Black,
            surface = Xp.Body,
            onSurface = Color.Black,
            surfaceVariant = Color.White,
            onSurfaceVariant = Color(0xFF555555),
            error = Color(0xFFC62828),
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
                0f to Color(0xFF2B66D3),
                0.55f to Color(0xFF8FC1F2),
                1f to Color(0xFFBFE0FA),
            ),
        )
        fun cloud(cx: Float, cy: Float, s: Float) {
            val c = Color.White.copy(alpha = 0.85f)
            drawOval(c, Offset(cx - s, cy - s * 0.28f), Size(s * 2f, s * 0.56f))
            drawOval(c, Offset(cx - s * 0.6f, cy - s * 0.5f), Size(s * 1.1f, s * 0.7f))
            drawOval(c, Offset(cx, cy - s * 0.42f), Size(s * 0.9f, s * 0.6f))
        }
        cloud(w * 0.25f, h * 0.2f, w * 0.18f)
        cloud(w * 0.72f, h * 0.12f, w * 0.14f)
        cloud(w * 0.55f, h * 0.34f, w * 0.1f)

        val back = Path().apply {
            moveTo(0f, h * 0.62f)
            cubicTo(w * 0.25f, h * 0.5f, w * 0.6f, h * 0.58f, w, h * 0.5f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(back, Brush.verticalGradient(listOf(Color(0xFF6CC33A), Color(0xFF2F8A1B)), startY = h * 0.5f, endY = h))
        val front = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(w * 0.3f, h * 0.64f, w * 0.7f, h * 0.84f, w, h * 0.7f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(front, Brush.verticalGradient(listOf(Color(0xFF4FAE26), Color(0xFF1F6E12)), startY = h * 0.64f, endY = h))
    }
}
