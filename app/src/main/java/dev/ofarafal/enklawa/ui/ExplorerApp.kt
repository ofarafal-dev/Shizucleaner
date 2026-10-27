package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Eksplorator magazynu sesji: pokazuje nazwy plików świata (zawartość jest zaszyfrowana kluczem z RAM). */
@Composable
fun ExplorerApp(env: AppEnv) {
    var tick by remember { mutableIntStateOf(0) }
    val names = remember(tick) { env.vault.names() }

    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Magazyn sesji (zaszyfrowany)",
                fontWeight = FontWeight.SemiBold,
                color = Xp.Link,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { tick++ }) { Text("Odśwież") }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(6.dp),
        ) {
            if (names.isEmpty()) {
                Text("Brak plików. Zapisz coś w Notatniku.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            names.forEach { n ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyph.Notes, Modifier.size(22.dp), tint = Color(0xFFF08A00))
                    Text(n, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    TextButton(onClick = { env.vault.delete(n); tick++ }) {
                        Text("Usuń", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Text(
            "Nazwy plików na dysku są losowe, a zawartość szyfruje klucz, który istnieje tylko w pamięci RAM.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
