package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ofarafal.enklawa.Prefs

@Composable
fun EntryScreen(onEnter: (amnesia: Boolean) -> Unit) {
    val ctx = LocalContext.current
    var amnesia by remember { mutableStateOf(false) } // domyślnie wyłączona

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Enklawa", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "Osobny świat w aplikacji. Wchodzisz, masz własny pulpit i przeglądarkę; wychodzisz i wracasz do zwykłego telefonu.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tryb amnezji", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (amnesia) "Włączony: po wyjściu klucz sesji znika, pliki i profil przeglądarki są kasowane."
                    else "Wyłączony: przeglądarka zachowuje dane (ciasteczka, logowania) między wejściami, bez szyfrowania.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = amnesia, onCheckedChange = { amnesia = it })
        }
        Button(onClick = { onEnter(amnesia) }, modifier = Modifier.fillMaxWidth()) {
            Text("Wejdź")
        }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Z pamięcią i hasłem — szyfrowane magazyny (wkrótce)")
        }

        Spacer(Modifier.height(8.dp))
        Text("Czego ta ochrona NIE daje", style = MaterialTheme.typography.titleMedium)
        Text(
            "• To prywatność na co dzień, nie ochrona przed zaawansowanym przeciwnikiem ani malware na telefonie.\n" +
                "• Android hosta jest niezaufany: logi systemowe, schowek i klawiatura mogą coś zapamiętać. Aplikacja prosi klawiaturę, by się nie uczyła, ale nie może tego wymusić.\n" +
                "• Klucz sesji (RAM) szyfruje pliki świata. Dane przeglądarki trzyma systemowy WebView poza naszym kluczem; po wyjściu usuwamy cały profil, lecz nie jest to kasowanie kryptograficzne.\n" +
                "• Adresy, które odwiedzasz, widzi Twoja sieć i odwiedzane strony. Tor będzie w kolejnej wersji.\n" +
                "• Nadpisywanie danych na pamięci flash nie daje gwarancji, dlatego opieramy się na zniszczeniu klucza.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        UpdatePanel(autoCheck = Prefs.autoUpdateCheck(ctx), modifier = Modifier.fillMaxWidth())
    }
}
