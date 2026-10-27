package dev.ofarafal.enklawa.ui

import android.content.Context
import android.os.BatteryManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class AppSpec(
    val id: String,
    val label: String,
    val glyph: Glyph,
    val top: Color,
    val bottom: Color,
    val enabled: Boolean,
)

private val Apps = listOf(
    AppSpec("browser", "Przeglądarka", Glyph.Globe, Color(0xFF5BC8FF), Color(0xFF1565C0), true),
    AppSpec("notes", "Notatki", Glyph.Notes, Color(0xFFFFD866), Color(0xFFF08A00), false),
    AppSpec("files", "Pliki", Glyph.Folder, Color(0xFF8EE08F), Color(0xFF2E7D32), false),
    AppSpec("chat", "Komunikator", Glyph.Chat, Color(0xFFD08CE6), Color(0xFF6A1B9A), false),
    AppSpec("tor", "Tor", Glyph.Onion, Color(0xFFA48BE0), Color(0xFF311B92), false),
    AppSpec("vaults", "Magazyny", Glyph.Lock, Color(0xFFFF9E80), Color(0xFFD84315), false),
)
private val DrawerSpec = AppSpec("drawer", "Aplikacje", Glyph.Grid, Color(0xFF8A97A6), Color(0xFF37424F), true)
private val PowerSpec = AppSpec("power", "Wyjdź", Glyph.Power, Color(0xFFFF8A80), Color(0xFFC62828), true)

private val Teal = Color(0xFF7FDBCA)

@Composable
fun DesktopScreen(
    isolatedProfile: Boolean,
    amnesia: Boolean,
    onOpenBrowser: () -> Unit,
    onExit: () -> Unit,
) {
    val ctx = LocalContext.current
    var drawerOpen by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }

    val launch: (AppSpec) -> Unit = { app ->
        drawerOpen = false
        when {
            app.id == "browser" -> onOpenBrowser()
            else -> Toast.makeText(ctx, "${app.label} — w kolejnych wersjach", Toast.LENGTH_SHORT).show()
        }
    }
    val requestExit: () -> Unit = {
        if (amnesia) {
            confirmExit = true
        } else {
            onExit()
        }
    }

    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    Box(Modifier.fillMaxSize()) {
        Wallpaper()

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            StatusBar(amnesia)
            Spacer(Modifier.height(36.dp))
            ClockWidget(amnesia, isolatedProfile)
            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Apps.take(4).forEach { app -> AppIcon(app, onClick = { launch(app) }) }
            }
            Spacer(Modifier.height(28.dp))
            Dock(
                onBrowser = { launch(Apps[0]) },
                onDrawer = { drawerOpen = true },
                onPower = requestExit,
            )
            Spacer(Modifier.height(18.dp))
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) {
            Drawer(onClose = { drawerOpen = false }, onLaunch = launch)
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Zniszczyć świat?") },
            text = { Text("Klucz sesji zniknie, pliki i profil przeglądarki zostaną skasowane, aplikacja się zamknie.") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; onExit() }) {
                    Text("Zniszcz i wyjdź", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Zostań") } },
        )
    }
}

/* ---------- tło ---------- */

@Composable
private fun Wallpaper() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF05080D), Color(0xFF0A1822), Color(0xFF0D2B33))))
        val a = Offset(size.width * 0.85f, size.height * 0.22f)
        drawCircle(
            Brush.radialGradient(listOf(Color(0x662DD4BF), Color.Transparent), center = a, radius = size.width * 0.85f),
            radius = size.width * 0.85f, center = a,
        )
        val b = Offset(size.width * 0.05f, size.height * 0.85f)
        drawCircle(
            Brush.radialGradient(listOf(Color(0x557C4DFF), Color.Transparent), center = b, radius = size.width * 0.9f),
            radius = size.width * 0.9f, center = b,
        )
    }
}

/* ---------- pasek stanu, zegar ---------- */

@Composable
private fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(10_000)
        }
    }
    return now
}

@Composable
private fun rememberBattery(): Pair<Int, Boolean> {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf(0 to false) }
    LaunchedEffect(Unit) {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        while (true) {
            state = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) to bm.isCharging
            delay(30_000)
        }
    }
    return state
}

@Composable
private fun StatusBar(amnesia: Boolean) {
    val now = rememberNow()
    val (level, charging) = rememberBattery()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        Chip(if (amnesia) "AMNEZJA" else "ZWYKŁY", if (amnesia) Teal else Color(0xFFB0BEC5))
        Spacer(Modifier.width(10.dp))
        Text(
            (if (charging) "⚡" else "") + "$level%",
            color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
        )
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun ClockWidget(amnesia: Boolean, isolatedProfile: Boolean) {
    val now = rememberNow()
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl-PL")))
        .replaceFirstChar { it.uppercase() }
    Column(Modifier.padding(horizontal = 28.dp)) {
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Color.White, fontSize = 82.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-2).sp,
        )
        Text(date, color = Color.White.copy(alpha = 0.72f), fontSize = 17.sp)
        Spacer(Modifier.height(18.dp))
        Glass(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (amnesia) "Świat tymczasowy" else "Świat zwykły",
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (amnesia) "Klucz sesji tylko w RAM. Po wyjściu znika razem z plikami i profilem przeglądarki."
                    else "Przeglądarka zachowuje dane między wejściami. Bez szyfrowania.",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 18.sp,
                )
                if (!isolatedProfile) {
                    Text(
                        "Ten WebView nie obsługuje osobnych profili — używa profilu domyślnego.",
                        color = MaterialTheme.colorScheme.error, fontSize = 12.sp, lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

/* ---------- szkło, ikony, dock ---------- */

@Composable
private fun Glass(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(22.dp), content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.07f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f))), shape),
    ) { content() }
}

@Composable
private fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "press")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

@Composable
private fun AppIcon(app: AppSpec, onClick: () -> Unit, size: Dp = 62.dp, showLabel: Boolean = true) {
    val shape = RoundedCornerShape(size * 0.3f)
    Column(
        Modifier.width(size + 18.dp).pressable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(size)
                .alpha(if (app.enabled) 1f else 0.55f)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(app.top, app.bottom)))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f))), shape),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(app.glyph, Modifier.size(size * 0.62f))
        }
        if (showLabel) {
            Text(
                app.label,
                color = Color.White.copy(alpha = if (app.enabled) 0.95f else 0.6f),
                fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 7.dp),
            )
        }
    }
}

@Composable
private fun Dock(onBrowser: () -> Unit, onDrawer: () -> Unit, onPower: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Glass(shape = RoundedCornerShape(34.dp)) {
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(Apps[0], onClick = onBrowser, size = 54.dp, showLabel = false)
                AppIcon(DrawerSpec, onClick = onDrawer, size = 54.dp, showLabel = false)
                AppIcon(PowerSpec, onClick = onPower, size = 54.dp, showLabel = false)
            }
        }
    }
}

@Composable
private fun Drawer(onClose: () -> Unit, onLaunch: (AppSpec) -> Unit) {
    val scrim by animateColorAsState(Color(0xF2070B10), label = "scrim")
    Box(
        Modifier
            .fillMaxSize()
            .background(scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.3f)),
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "Aplikacje", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Spacer(Modifier.height(24.dp))
            Apps.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { app -> AppIcon(app, onClick = { onLaunch(app) }) }
                    repeat(4 - row.size) { Spacer(Modifier.width(80.dp)) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
