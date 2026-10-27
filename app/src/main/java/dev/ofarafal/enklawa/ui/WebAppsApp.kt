package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.ofarafal.enklawa.browser.UrlInput
import dev.ofarafal.enklawa.os.WebApps

/** Strony jako aplikacje: każda we własnym oknie. Wszystkie dzielą profil przeglądarki tego świata (ciasteczka, logowania). */
@Composable
fun WebAppsApp(env: AppEnv) {
    var custom by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Otwórz stronę jako osobną aplikację. Dane logowania zostają w profilu tego świata" +
                (if (env.amnesia || env.vaultName != null) " i znikną po wyjściu." else " (tryb zwykły: zachowane, bez szyfrowania)."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WebApps.presets.forEach { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C2530))
                    .clickable { env.open(WebApps.idFor(p.url)) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(Glyph.Globe, Modifier.size(28.dp), tint = Xp.Link)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(p.name, color = MaterialTheme.colorScheme.onSurface)
                    Text(p.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text("Własna strona", color = Xp.Link, modifier = Modifier.padding(top = 8.dp))
        PrivateEditText(
            text = custom,
            onTextChange = { custom = it },
            singleLine = true,
            hint = "adres, np. example.org",
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = custom.isNotBlank(),
            onClick = {
                val url = UrlInput.resolve(custom)
                if (url.startsWith("http")) env.open(WebApps.idFor(url))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Otwórz jako aplikację") }
    }
}
