package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/** Własne ikony rysowane w kodzie (bez zależności od pakietu material-icons). */
enum class Glyph { Globe, Notes, Folder, Chat, Onion, Lock, Power, Grid, Back, Reload, Close, Settings, Logo, Computer }

@Composable
fun GlyphIcon(glyph: Glyph, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.085f
        val line = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val thin = Stroke(width = w * 0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (glyph) {
            Glyph.Globe -> {
                val c = Offset(s / 2, s / 2)
                val r = s * 0.36f
                drawCircle(tint, r, c, style = line)
                drawOval(tint, Offset(c.x - r * 0.45f, c.y - r), Size(r * 0.9f, r * 2f), style = thin)
                drawLine(tint, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = w * 0.8f, cap = StrokeCap.Round)
                val dx = r * 0.866f
                drawLine(tint, Offset(c.x - dx, c.y - r * 0.5f), Offset(c.x + dx, c.y - r * 0.5f), strokeWidth = w * 0.8f, cap = StrokeCap.Round)
                drawLine(tint, Offset(c.x - dx, c.y + r * 0.5f), Offset(c.x + dx, c.y + r * 0.5f), strokeWidth = w * 0.8f, cap = StrokeCap.Round)
            }

            Glyph.Notes -> {
                drawRoundRect(
                    tint, Offset(s * 0.25f, s * 0.16f), Size(s * 0.5f, s * 0.68f),
                    CornerRadius(s * 0.08f), style = line,
                )
                for (i in 0..2) {
                    val y = s * (0.36f + i * 0.15f)
                    drawLine(tint, Offset(s * 0.36f, y), Offset(s * 0.64f, y), strokeWidth = w * 0.8f, cap = StrokeCap.Round)
                }
            }

            Glyph.Folder -> {
                val p = Path().apply {
                    moveTo(s * 0.16f, s * 0.28f)
                    lineTo(s * 0.40f, s * 0.28f)
                    lineTo(s * 0.48f, s * 0.37f)
                    lineTo(s * 0.84f, s * 0.37f)
                    lineTo(s * 0.84f, s * 0.74f)
                    lineTo(s * 0.16f, s * 0.74f)
                    close()
                }
                drawPath(p, tint, style = line)
            }

            Glyph.Chat -> {
                drawRoundRect(
                    tint, Offset(s * 0.16f, s * 0.2f), Size(s * 0.68f, s * 0.46f),
                    CornerRadius(s * 0.12f), style = line,
                )
                val tail = Path().apply {
                    moveTo(s * 0.34f, s * 0.66f)
                    lineTo(s * 0.30f, s * 0.82f)
                    lineTo(s * 0.50f, s * 0.66f)
                }
                drawPath(tail, tint, style = line)
                for (i in 0..2) drawCircle(tint, s * 0.035f, Offset(s * (0.36f + i * 0.14f), s * 0.43f))
            }

            Glyph.Onion -> {
                val c = Offset(s / 2, s / 2)
                drawCircle(tint, s * 0.37f, c, style = line)
                drawCircle(tint, s * 0.25f, c, style = thin)
                drawCircle(tint, s * 0.13f, c, style = thin)
            }

            Glyph.Lock -> {
                drawRoundRect(
                    tint, Offset(s * 0.25f, s * 0.45f), Size(s * 0.5f, s * 0.36f),
                    CornerRadius(s * 0.07f), style = line,
                )
                drawArc(
                    tint, 180f, 180f, false, Offset(s * 0.34f, s * 0.19f), Size(s * 0.32f, s * 0.36f), style = line,
                )
                drawCircle(tint, s * 0.04f, Offset(s * 0.5f, s * 0.62f))
            }

            Glyph.Power -> {
                drawArc(
                    tint, -60f, 300f, false, Offset(s * 0.25f, s * 0.28f), Size(s * 0.5f, s * 0.5f), style = line,
                )
                drawLine(tint, Offset(s * 0.5f, s * 0.18f), Offset(s * 0.5f, s * 0.48f), strokeWidth = w, cap = StrokeCap.Round)
            }

            Glyph.Grid -> {
                for (row in 0..2) for (col in 0..2) {
                    drawCircle(tint, s * 0.058f, Offset(s * (0.28f + col * 0.22f), s * (0.28f + row * 0.22f)))
                }
            }

            Glyph.Back -> {
                val p = Path().apply {
                    moveTo(s * 0.6f, s * 0.22f)
                    lineTo(s * 0.34f, s * 0.5f)
                    lineTo(s * 0.6f, s * 0.78f)
                }
                drawPath(p, tint, style = line)
            }

            Glyph.Reload -> {
                drawArc(
                    tint, -40f, 280f, false, Offset(s * 0.24f, s * 0.24f), Size(s * 0.52f, s * 0.52f), style = line,
                )
                val head = Path().apply {
                    moveTo(s * 0.66f, s * 0.16f)
                    lineTo(s * 0.72f, s * 0.34f)
                    lineTo(s * 0.54f, s * 0.32f)
                }
                drawPath(head, tint, style = line)
            }

            Glyph.Settings -> {
                val c = Offset(s / 2, s / 2)
                drawCircle(tint, s * 0.14f, c, style = line)
                drawCircle(tint, s * 0.27f, c, style = thin)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val dx = Math.cos(a).toFloat()
                    val dy = Math.sin(a).toFloat()
                    drawLine(
                        tint,
                        Offset(c.x + dx * s * 0.27f, c.y + dy * s * 0.27f),
                        Offset(c.x + dx * s * 0.38f, c.y + dy * s * 0.38f),
                        strokeWidth = w * 1.2f, cap = StrokeCap.Round,
                    )
                }
            }

            Glyph.Logo -> {
                val c = Offset(s / 2, s / 2)
                drawCircle(tint, s * 0.36f, c, style = Stroke(width = s * 0.12f, cap = StrokeCap.Round))
                drawCircle(tint.copy(alpha = 0.95f), s * 0.15f, c)
            }

            Glyph.Computer -> {
                drawRoundRect(
                    tint, Offset(s * 0.15f, s * 0.2f), Size(s * 0.7f, s * 0.46f),
                    CornerRadius(s * 0.06f), style = line,
                )
                drawLine(tint, Offset(s * 0.5f, s * 0.66f), Offset(s * 0.5f, s * 0.78f), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(tint, Offset(s * 0.32f, s * 0.8f), Offset(s * 0.68f, s * 0.8f), strokeWidth = w, cap = StrokeCap.Round)
            }

            Glyph.Close -> {
                drawLine(tint, Offset(s * 0.28f, s * 0.28f), Offset(s * 0.72f, s * 0.72f), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(tint, Offset(s * 0.72f, s * 0.28f), Offset(s * 0.28f, s * 0.72f), strokeWidth = w, cap = StrokeCap.Round)
            }
        }
    }
}
