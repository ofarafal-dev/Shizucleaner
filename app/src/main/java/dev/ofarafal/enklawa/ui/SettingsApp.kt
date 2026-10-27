package dev.ofarafal.enklawa.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ofarafal.enklawa.Prefs
import dev.ofarafal.enklawa.browser.BrowserProfile

/** „Ustawienia”: aktualizacje, prywatność, informacje o aplikacji. */
@Composable
fun SettingsApp(env: AppEnv) {
    val ctx = LocalContext.current
    var autoCheck by remember { mutableStateOf(Prefs.autoUpdateCheck(ctx)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                Switch(checked = autoCheck, onCheckedChange = { autoCheck = it; Prefs.setAutoUpdateCheck(ctx, it) })
            }
        }

        Section("Prywatność") {
            Text(
                if (env.amnesia) "Tryb amnezji: po wyjściu klucz sesji znika, a pliki świata i profil przeglądarki są kasowane."
                else if (env.vaultName != null) "Sejf „${env.vaultName}”: pliki i notatki są szyfrowane kluczem z hasła i zostają na telefonie. Przeglądarka działa na profilu tymczasowym i po wyjściu jest czyszczona; jej dane nie trafiają do sejfu."
                else "Tryb zwykły: przeglądarka zachowuje dane (ciasteczka, logowania) bez szyfrowania. Pliki świata znikają po wyjściu.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Android hosta jest niezaufany: logi systemowe, schowek i klawiatura mogą coś zapamiętać. " +
                    "To prywatność na co dzień, nie ochrona przed zaawansowanym przeciwnikiem ani malware na telefonie.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!env.amnesia && env.vaultName == null) {
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
            Text("Microcosm — osobny świat w aplikacji.", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Zero analityki. Jedyne połączenie poza przeglądarką to sprawdzanie i pobieranie aktualizacji z GitHub Releases (suma SHA-256, podpis sprawdza Android).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF1C2530)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = Xp.Link)
        content()
    }
}
