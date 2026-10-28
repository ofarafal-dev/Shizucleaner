@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.ofarafal.enklawa.ui

import android.content.Context
import android.os.BatteryManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ofarafal.enklawa.browser.BrowserProfile
import dev.ofarafal.enklawa.crypto.SessionVault
import dev.ofarafal.enklawa.os.Win
import dev.ofarafal.enklawa.os.WindowLogic
import dev.ofarafal.enklawa.os.WindowsState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val DesktopIconIds = listOf("browser", "webapps", "notepad", "explorer", "calc", "clock")
private val PinnedIds = listOf("browser", "webapps", "notepad", "explorer", "calc", "clock", "tor")
private val RightIds = listOf("computer", "explorer", "settings")
private val LabelShadow = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.8f), Offset(1.5f, 1.5f), 3f))

/**
 * Powłoka systemu w stylu Luna: pulpit z ikonami, okna z paskiem tytułu, pasek zadań z przyciskiem start,
 * menu Start i zasobnik. Okna otwierają się zmaksymalizowane (to telefon); przycisk w pasku tytułu je przywraca.
 */
@Composable
fun XpShell(amnesia: Boolean, vaultName: String?, profile: BrowserProfile, vault: SessionVault, onExit: () -> Unit) {
    val ctx = LocalContext.current
    var wm by remember { mutableStateOf(WindowsState()) }
    var startOpen by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var desktopIds by remember { mutableStateOf(DesktopIconIds) }
    var desktopMenu by remember { mutableStateOf(false) }
    var pendingOpen by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    androidx.compose.runtime.DisposableEffect(vault) {
        AppStore.load(vault)
        dev.ofarafal.enklawa.miniapp.MiniAppStore.load(vault)
        onDispose { AppStore.clear(); dev.ofarafal.enklawa.miniapp.MiniAppStore.clear() }
    }

    BackHandler(enabled = startOpen) { startOpen = false }
    BackHandler(enabled = !startOpen && wm.activeId != null) {
        wm.activeId?.let { wm = WindowLogic.close(wm, it) }
    }

    val turnOff: () -> Unit = {
        startOpen = false
        if (amnesia) {
            confirmExit = true
        } else {
            onExit()
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val areaW = maxWidth.value
            val areaH = maxHeight.value

            val open: (String) -> Unit = { id ->
                startOpen = false
                val app = appById(id)
                when {
                    app == null -> Unit
                    !app.available -> Toast.makeText(ctx, "${app.title} — w kolejnych wersjach", Toast.LENGTH_SHORT).show()
                    else -> wm = WindowLogic.open(wm, app.id, app.title, app.singleton, areaW, areaH)
                }
            }

            LaunchedEffect(pendingOpen) {
                pendingOpen?.let { id -> pendingOpen = null; open(id) }
            }

            Wallpaper(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        desktopMenu = true
                    })
                },
            )
            Box(Modifier.padding(start = 12.dp, top = 40.dp)) {
                DropdownMenu(expanded = desktopMenu, onDismissRequest = { desktopMenu = false }) {
                    DropdownMenuItem(text = { Text("Ustawienia") }, onClick = { desktopMenu = false; open("settings") })
                    DropdownMenuItem(text = { Text("Przywróć ikony pulpitu") }, onClick = { desktopMenu = false; desktopIds = DesktopIconIds })
                    DropdownMenuItem(text = { Text("Wyłącz świat") }, onClick = { desktopMenu = false; turnOff() })
                }
            }

            Column(
                Modifier.padding(start = 10.dp, top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val installedIds = AppStore.apps.map { it.id } + dev.ofarafal.enklawa.miniapp.MiniAppStore.apps.map { "mini:" + it.id }
                (desktopIds + installedIds).mapNotNull { appById(it) }.forEach { app ->
                    val inst = AppStore.apps.firstOrNull { it.id == app.id }
                    DesktopIcon(
                        app,
                        onClick = { open(app.id) },
                        onRemove = { desktopIds = desktopIds - app.id },
                        onUninstall = inst?.let { i -> { AppStore.uninstall(vault, i.url) } }
                            ?: app.id.takeIf { it.startsWith("mini:") }?.let { mid -> { dev.ofarafal.enklawa.miniapp.MiniAppStore.uninstall(vault, mid.removePrefix("mini:")) } },
                    )
                }
            }

            wm.windows.forEach { win ->
                key(win.id) {
                    val app = appById(win.appId)
                    if (app != null) {
                        val active = wm.activeId == win.id
                        XpWindow(
                            win = win,
                            app = app,
                            active = active,
                            env = AppEnv(amnesia, profile, vault, active && !win.minimized, open, vaultName),
                            onFocus = { if (wm.activeId != win.id) wm = WindowLogic.focus(wm, win.id) },
                            onMinimize = { wm = WindowLogic.minimize(wm, win.id) },
                            onMaximize = { wm = WindowLogic.toggleMaximize(wm, win.id) },
                            onClose = { wm = WindowLogic.close(wm, win.id) },
                            onDrag = { dx, dy -> wm = WindowLogic.move(wm, win.id, dx, dy, areaW, areaH) },
                        )
                    }
                }
            }

            if (startOpen) {
                StartMenu(
                    amnesia = amnesia,
                    vaultName = vaultName,
                    onAddToDesktop = { id -> if (id !in desktopIds) desktopIds = desktopIds + id },
                    onLaunch = open,
                    onTurnOff = turnOff,
                    onDismiss = { startOpen = false },
                )
            }
        }

        Taskbar(
            wm = wm,
            amnesia = amnesia || vaultName != null,
            onStart = { startOpen = !startOpen },
            onWindow = { id -> startOpen = false; wm = WindowLogic.taskbarClick(wm, id) },
            onMinimize = { id -> wm = WindowLogic.minimize(wm, id) },
            onClose = { id -> wm = WindowLogic.close(wm, id) },
            onOpenApp = { id -> pendingOpen = id },
            onTurnOff = turnOff,
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Wyłączyć świat?") },
            text = { Text("Klucz sesji zniknie, pliki i profil przeglądarki zostaną skasowane, aplikacja się zamknie.") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; onExit() }) {
                    Text("Wyłącz", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Anuluj") } },
        )
    }
}

/* ---------- ikony ---------- */

@Composable
private fun IconBox(app: AppDef, size: Dp, dim: Boolean = false) = TileIcon(app, size, dim)

@Composable
fun TileIcon(app: AppDef, size: Dp, dim: Boolean = false) {
    val shape = RoundedCornerShape(size * 0.26f)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(app.top, app.bottom)))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.05f))), shape),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = app.iconBitmap
        when {
            bmp != null -> androidx.compose.foundation.Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.size(size * 0.66f).clip(RoundedCornerShape(size * 0.1f)),
            )
            app.letter != null -> Text(
                app.letter,
                color = Color.White.copy(alpha = if (dim) 0.55f else 1f),
                fontSize = (size.value * 0.46f).sp,
                fontWeight = FontWeight.Bold,
            )
            else -> GlyphIcon(app.glyph, Modifier.size(size * 0.62f), tint = Color.White.copy(alpha = if (dim) 0.55f else 1f))
        }
    }
}

@Composable
private fun DesktopIcon(app: AppDef, onClick: () -> Unit, onRemove: () -> Unit, onUninstall: (() -> Unit)? = null) {
    var menu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Box {
        Column(
            Modifier
                .width(76.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menu = true },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBox(app, 48.dp)
            Text(
                app.title,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
                style = LabelShadow,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Otwórz") }, onClick = { menu = false; onClick() })
            if (onUninstall != null) {
                DropdownMenuItem(text = { Text("Odinstaluj") }, onClick = { menu = false; onUninstall() })
            } else {
                DropdownMenuItem(text = { Text("Usuń z pulpitu") }, onClick = { menu = false; onRemove() })
            }
        }
    }
}

/* ---------- okno ---------- */

@Composable
private fun XpWindow(
    win: Win,
    app: AppDef,
    active: Boolean,
    env: AppEnv,
    onFocus: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
) {
    val density = LocalDensity.current.density
    val focus = rememberUpdatedState(onFocus)
    val drag = rememberUpdatedState(onDrag)

    val geometry = if (win.maximized) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .offset { IntOffset((win.x * density).roundToInt(), (win.y * density).roundToInt()) }
            .size(win.w.dp, win.h.dp)
    }
    // Zminimalizowane okno zostaje w kompozycji (żeby np. strona w przeglądarce nie zginęła), ale poza ekranem.
    val hidden = if (win.minimized) Modifier.offset(x = 6000.dp) else Modifier
    val shape = RoundedCornerShape(14.dp)

    Column(
        hidden
            .then(geometry)
            .clip(shape)
            .background(if (active) Xp.Frame else Xp.FrameInactive)
            .border(1.dp, if (active) Xp.Frame else Xp.FrameInactive, shape)
            .pointerInput(Unit) {
                // Obserwujemy dotknięcie bez konsumowania, żeby okno wyszło na wierzch.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.changedToDown() }) focus.value()
                    }
                }
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Brush.verticalGradient(if (active) Xp.TitleActive else Xp.TitleInactive))
                .pointerInput(win.maximized) {
                    if (!win.maximized) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            drag.value(dragAmount.x / this.density, dragAmount.y / this.density)
                        }
                    }
                }
                .padding(start = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphIcon(app.glyph, Modifier.size(18.dp))
            Text(
                win.title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            CaptionButton(CaptionKind.Minimize, onMinimize)
            CaptionButton(if (win.maximized) CaptionKind.Restore else CaptionKind.Maximize, onMaximize)
            CaptionButton(CaptionKind.Close, onClose)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 1.dp, end = 1.dp, bottom = 1.dp)
                .background(Xp.Body),
        ) {
            XpLightTheme {
                Surface(Modifier.fillMaxSize(), color = Xp.Body) { app.content(env) }
            }
        }
    }
}

private enum class CaptionKind { Minimize, Maximize, Restore, Close }

@Composable
private fun CaptionButton(kind: CaptionKind, onClick: () -> Unit) {
    val bg = if (kind == CaptionKind.Close) Color(0xFFE5574B) else Color(0x33FFFFFF)
    Box(
        Modifier
            .padding(end = 6.dp)
            .size(26.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(11.dp)) {
            val s = size.minDimension
            val t = 2.2.dp.toPx()
            when (kind) {
                CaptionKind.Minimize -> drawLine(Color.White, Offset(s * 0.1f, s * 0.9f), Offset(s * 0.7f, s * 0.9f), strokeWidth = t)
                CaptionKind.Maximize -> {
                    drawRect(Color.White, Offset(t / 2, t / 2), androidx.compose.ui.geometry.Size(s - t, s - t), style = Stroke(t))
                    drawLine(Color.White, Offset(0f, t / 2), Offset(s, t / 2), strokeWidth = t * 1.4f)
                }
                CaptionKind.Restore -> {
                    drawRect(Color.White, Offset(s * 0.3f, t / 2), androidx.compose.ui.geometry.Size(s * 0.7f - t / 2, s * 0.65f), style = Stroke(t * 0.8f))
                    drawRect(Color.White, Offset(t / 2, s * 0.3f), androidx.compose.ui.geometry.Size(s * 0.7f - t / 2, s * 0.65f), style = Stroke(t * 0.8f))
                }
                CaptionKind.Close -> {
                    drawLine(Color.White, Offset(s * 0.1f, s * 0.1f), Offset(s * 0.9f, s * 0.9f), strokeWidth = t)
                    drawLine(Color.White, Offset(s * 0.9f, s * 0.1f), Offset(s * 0.1f, s * 0.9f), strokeWidth = t)
                }
            }
        }
    }
}

/* ---------- pasek zadań ---------- */

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
private fun Taskbar(
    wm: WindowsState,
    amnesia: Boolean,
    onStart: () -> Unit,
    onWindow: (Int) -> Unit,
    onMinimize: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onOpenApp: (String) -> Unit,
    onTurnOff: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var startMenu by remember { mutableStateOf(false) }
    val now = rememberNow()
    val (level, charging) = rememberBattery()
    val dock = RoundedCornerShape(26.dp)
    Box(Modifier.fillMaxWidth().background(Color(0xFF07090F)).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(dock)
                .background(Brush.verticalGradient(Xp.Taskbar))
                .border(1.dp, Color(0xFF2A3342), dock)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(Xp.StartGreen))
                        .combinedClickable(
                            onClick = onStart,
                            onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); startMenu = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) { GlyphIcon(Glyph.Logo, Modifier.size(24.dp), tint = Color(0xFF06231E)) }
                DropdownMenu(expanded = startMenu, onDismissRequest = { startMenu = false }) {
                    DropdownMenuItem(text = { Text("Ustawienia") }, onClick = { startMenu = false; onOpenApp("settings") })
                    DropdownMenuItem(text = { Text("Ten świat") }, onClick = { startMenu = false; onOpenApp("computer") })
                    DropdownMenuItem(text = { Text("Wyłącz świat") }, onClick = { startMenu = false; onTurnOff() })
                }
            }

            Row(
                Modifier.weight(1f).padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                wm.windows.forEach { win ->
                    val app = appById(win.appId)
                    val current = wm.activeId == win.id && !win.minimized
                    val shape = RoundedCornerShape(18.dp)
                    var winMenu by remember(win.id) { mutableStateOf(false) }
                    Box {
                    Row(
                        Modifier
                            .width(112.dp)
                            .height(36.dp)
                            .clip(shape)
                            .background(if (current) Color(0x337FDBCA) else Color(0x14FFFFFF))
                            .border(1.dp, if (current) Xp.Accent.copy(alpha = 0.7f) else Color.Transparent, shape)
                            .combinedClickable(
                                onClick = { onWindow(win.id) },
                                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); winMenu = true },
                            )
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (app != null) GlyphIcon(app.glyph, Modifier.size(16.dp))
                        Text(
                            win.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    DropdownMenu(expanded = winMenu, onDismissRequest = { winMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (win.minimized) "Przywróć" else "Pokaż") },
                            onClick = { winMenu = false; if (win.minimized || !current) onWindow(win.id) },
                        )
                        if (!win.minimized) {
                            DropdownMenuItem(text = { Text("Minimalizuj") }, onClick = { winMenu = false; onMinimize(win.id) })
                        }
                        DropdownMenuItem(text = { Text("Zamknij") }, onClick = { winMenu = false; onClose(win.id) })
                    }
                    }
                }
            }

            Row(
                Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlyphIcon(Glyph.Onion, Modifier.size(16.dp), tint = torIndicatorColor())
                GlyphIcon(Glyph.Lock, Modifier.size(16.dp), tint = if (amnesia) Color(0xFF9FFFE0) else Color.White.copy(alpha = 0.6f))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                    Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text((if (charging) "⚡" else "") + "$level%", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }
        }
    }
}

/* ---------- menu Start ---------- */

@Composable
private fun StartMenu(amnesia: Boolean, vaultName: String?, onAddToDesktop: (String) -> Unit, onLaunch: (String) -> Unit, onTurnOff: () -> Unit, onDismiss: () -> Unit) {
    var allPrograms by remember { mutableStateOf(false) }
    val noRipple = remember { MutableInteractionSource() }
    val menuShape = RoundedCornerShape(20.dp)

    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp)
                .fillMaxWidth(0.95f)
                .widthIn(max = 350.dp)
                .clip(menuShape)
                .background(Xp.Frame)
                .padding(1.dp)
                .clickable(interactionSource = noRipple, indication = null, onClick = {}),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 19.dp, topEnd = 19.dp))
                    .background(Brush.horizontalGradient(Xp.MenuHeader))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val avatar = RoundedCornerShape(7.dp)
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(avatar)
                        .background(Brush.verticalGradient(listOf(Color(0xFF9AD0FF), Color(0xFF2F6BD6))))
                        .border(2.dp, Color.White, avatar),
                    contentAlignment = Alignment.Center,
                ) { GlyphIcon(Glyph.Logo, Modifier.size(28.dp)) }
                Text(
                    if (amnesia) "Świat tymczasowy" else if (vaultName != null) "Sejf: $vaultName" else "Świat zwykły",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    style = LabelShadow,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }

            Row(Modifier.fillMaxWidth().height(330.dp)) {
                Column(Modifier.weight(1.15f).fillMaxHeight().background(Xp.Panel).padding(vertical = 6.dp)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        if (allPrograms) {
                            MenuLine("◂ Wstecz", null, bold = true, color = Xp.Link) { allPrograms = false }
                            AppRegistry.forEach { app ->
                                MenuLine(app.title, app, enabled = app.available, onAddToDesktop = { onAddToDesktop(app.id) }) { onLaunch(app.id) }
                            }
                            (AppStore.apps.map { it.id } + dev.ofarafal.enklawa.miniapp.MiniAppStore.apps.map { "mini:" + it.id }).mapNotNull { appById(it) }.forEach { app ->
                                MenuLine(app.title, app) { onLaunch(app.id) }
                            }
                        } else {
                            PinnedIds.mapNotNull { appById(it) }.forEach { app ->
                                MenuLine(app.title, app, bold = true, enabled = app.available, onAddToDesktop = { onAddToDesktop(app.id) }) { onLaunch(app.id) }
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2A3342)))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { allPrograms = !allPrograms }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Wszystkie programy", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE4E8EC), modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(20.dp).clip(CircleShape).background(Xp.Accent),
                            contentAlignment = Alignment.Center,
                        ) { Text("▸", color = Color(0xFF06231E), fontSize = 12.sp) }
                    }
                }
                Column(Modifier.weight(0.85f).fillMaxHeight().background(Xp.MenuRight).padding(vertical = 6.dp)) {
                    RightIds.mapNotNull { appById(it) }.forEach { app ->
                        MenuLine(app.title, app, bold = true, color = Color(0xFFE4E8EC), small = true) { onLaunch(app.id) }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(Xp.MenuHeader))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onTurnOff).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val power = RoundedCornerShape(6.dp)
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(power)
                            .background(Brush.verticalGradient(listOf(Color(0xFFEE8A70), Color(0xFFC8401A))))
                            .border(1.dp, Color.White.copy(alpha = 0.8f), power),
                        contentAlignment = Alignment.Center,
                    ) { GlyphIcon(Glyph.Power, Modifier.size(18.dp)) }
                    Text("Wyłącz", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun MenuLine(
    title: String,
    app: AppDef?,
    bold: Boolean = false,
    color: Color = Color(0xFFE4E8EC),
    small: Boolean = false,
    enabled: Boolean = true,
    onAddToDesktop: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (onAddToDesktop != null && enabled) {
                        { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menu = true }
                    } else null,
                )
                .padding(horizontal = 10.dp, vertical = if (small) 7.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (app != null) {
                IconBox(app, if (small) 24.dp else 30.dp, dim = !enabled)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                title,
                color = if (enabled) color else Color(0xFF8A8A8A),
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (small) 12.sp else 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Otwórz") }, onClick = { menu = false; onClick() })
            DropdownMenuItem(text = { Text("Dodaj do pulpitu") }, onClick = { menu = false; onAddToDesktop?.invoke() })
        }
    }
}
