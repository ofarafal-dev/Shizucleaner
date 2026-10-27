package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ofarafal.enklawa.tor.TorManager

private val TorGreen = Color(0xFF6EE7A8)
private val TorAmber = Color(0xFFFFD166)
private val TorRed = Color(0xFFFF6B6B)
private val TorGray = Color(0xFF8A8A8A)

/** Kolor wskaźnika: zielony = połączono, bursztynowy = łączenie, czerwony = brak Tora (ruch zablokowany), szary = Tor wyłączony. */
fun torIndicatorColor(): Color = when {
    !TorManager.routingWanted -> TorGray
    TorManager.isReady -> TorGreen
    TorManager.state is TorManager.State.Failed || TorManager.state is TorManager.State.Off -> TorRed
    else -> TorAmber
}

fun torStatusLabel(): String = when {
    !TorManager.routingWanted -> "Bez Tora: ruch idzie bezpośrednio"
    TorManager.isReady -> "Tor: połączono"
    TorManager.state is TorManager.State.Failed -> "Tor: błąd, ruch zablokowany"
    TorManager.state is TorManager.State.Off -> "Tor: wyłączony, ruch zablokowany"
    else -> "Tor: łączenie…"
}

/** Pasek stanu nad stroną: widoczny, gdy Tor nie jest połączony albo gdy przekierowanie jest wyłączone. */
@Composable
fun TorStatusStrip() {
    if (TorManager.routingWanted && TorManager.isReady) return
    val ctx = LocalContext.current
    val color = torIndicatorColor()
    Row(
        Modifier.fillMaxWidth().background(color.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(Glyph.Onion, Modifier.size(16.dp), tint = color)
        Text(torStatusLabel(), color = color, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(start = 6.dp))
        if (!TorManager.routingWanted) {
            TextButton(onClick = { TorManager.setRouting(ctx, true) }) { Text("Włącz Tora") }
        } else if (!TorManager.isActive) {
            TextButton(onClick = { TorManager.start(ctx) }) { Text("Ponów") }
        }
    }
}

/** Ekran zamiast przeglądarki, dopóki Tor nie jest gotowy: żadne połączenie nie wychodzi bez Tora. */
@Composable
fun TorGate() {
    val ctx = LocalContext.current
    val color = torIndicatorColor()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlyphIcon(Glyph.Onion, Modifier.size(56.dp), tint = color)
        Text(torStatusLabel(), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, textAlign = TextAlign.Center)
        Text(
            "Przeglądarka ruszy, gdy Tor połączy się z siecią. Do tego czasu nic nie wychodzi z telefonu bezpośrednio.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!TorManager.isActive) {
            Button(onClick = { TorManager.start(ctx) }) { Text("Spróbuj ponownie") }
        }
        OutlinedButton(onClick = { TorManager.setRouting(ctx, false) }) { Text("Przejdź bez Tora (niezalecane)") }
    }
}
