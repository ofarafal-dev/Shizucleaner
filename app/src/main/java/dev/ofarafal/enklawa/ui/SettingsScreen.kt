package dev.ofarafal.enklawa.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ofarafal.enklawa.Prefs
import dev.ofarafal.enklawa.browser.BrowserProfile

@Composable
fun SettingsScreen(amnesia: Boolean, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var autoCheck by remember { mutableStateOf(Prefs.autoUpdateCheck(ctx)) }

    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF05080D), Color(0xFF0A1822))))
            .safeDrawingPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { GlyphIcon(Glyph.Back, Modifier.size(26.dp)) }
            Text("Ustawienia", fontSize = 24.sp, fontWeight = FontWeight.Light, color = Color.White)
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Section("Aktualizacje") {
                UpdatePanel(autoCheck = false)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sprawdzaj przy starcie", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Zapytanie o nowe wydanie idzie do GitHuba z ekranu wejścia, poza światem.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = autoCheck,
                        onCheckedChange = { autoCheck = it; Prefs.setAutoUpdateCheck(ctx, it) },
                    )
                }
            }

            Section("Prywatność") {
                Text(
                    if (amnesia) "Tryb amnezji: klucz sesji tylko w RAM, po wyjściu kasowane są pliki świata i profil przeglądarki."
                    else "Tryb zwykły: przeglądarka zachowuje dane (ciasteczka, logowania) bez szyfrowania.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Android hosta jest niezaufany: logi systemowe, schowek i klawiatura mogą coś zapamiętać. " +
                        "To prywatność na co dzień, nie ochrona przed zaawansowanym przeciwnikiem ani malware na telefonie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!amnesia) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val ok = BrowserProfile.wipePersistent()
                            Toast.makeText(
                                ctx,
                                if (ok) "Dane przeglądarki usunięte" else "Nie udało się usunąć — zamknij przeglądarkę i spróbuj ponownie",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                    ) { Text("Wyczyść dane przeglądarki") }
                }
            }

            Section("O aplikacji") {
                Text("Enklawa — osobny świat w aplikacji.", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Zero analityki. Jedyne połączenie poza przeglądarką to sprawdzanie i pobieranie aktualizacji z GitHub Releases (suma SHA-256, podpis sprawdza Android).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f))))
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.6.sp)
        content()
    }
}
