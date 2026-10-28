package dev.ofarafal.enklawa.ui

import android.widget.Toast
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ofarafal.enklawa.browser.UrlInput
import dev.ofarafal.enklawa.os.AppCatalog
import dev.ofarafal.enklawa.os.InstalledApp
import dev.ofarafal.enklawa.os.WebApps

/** Aplikacje-strony: zainstalowane (z ikoną i nazwą), polecane i własny adres. Każda otwiera się we własnym oknie. */
@Composable
fun WebAppsApp(env: AppEnv) {
    val ctx = LocalContext.current
    var custom by remember { mutableStateOf("") }
    val zipPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val files = ctx.contentResolver.openInputStream(uri)!!.use { dev.ofarafal.enklawa.miniapp.MiniApps.readZip(it) }
                dev.ofarafal.enklawa.miniapp.MiniAppStore.install(env.vault, files)
            }.onSuccess { Toast.makeText(ctx, "Zainstalowano ${it.name}", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(ctx, "Nie udało się: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }
    val note = when {
        env.vaultName != null -> "Zainstalowane aplikacje zostają w sejfie „${env.vaultName}”."
        env.amnesia -> "Zainstalowane aplikacje znikną po wyjściu (amnezja)."
        else -> "Zainstalowane aplikacje znikną po wyjściu ze świata (zapamiętuje je tylko sejf)."
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Strony jako aplikacje. Dane logowania zostają w profilu przeglądarki tego świata. $note " +
                "Ikona pochodzi ze strony i pobiera się przez ten sam kanał co przeglądarka.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (AppStore.apps.isNotEmpty()) {
            Text("Zainstalowane", color = Xp.Link)
            AppStore.apps.forEach { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C2530))
                        .clickable { env.open(a.id) }
                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    appById(a.id)?.let { TileIcon(it, 34.dp) }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(a.name, color = MaterialTheme.colorScheme.onSurface)
                        Text(a.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    TextButton(onClick = {
                        AppStore.uninstall(env.vault, a.url)
                        Toast.makeText(ctx, "Odinstalowano ${a.name}", Toast.LENGTH_SHORT).show()
                    }) { Text("Odinstaluj", color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        Text("Mini-aplikacje", color = Xp.Link, modifier = Modifier.padding(top = 4.dp))
        Text(
            "Własne narzędzia z paczek .zip (manifest.json + HTML/JS). Działają bez internetu i bez dostępu do czegokolwiek, dopóki im tego nie pozwolisz.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        dev.ofarafal.enklawa.miniapp.MiniAppStore.apps.forEach { m ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C2530))
                    .clickable { env.open("mini:" + m.id) }.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                appById("mini:" + m.id)?.let { TileIcon(it, 34.dp) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(m.name, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        if (m.permissions.isEmpty()) "bez uprawnień" else m.permissions.joinToString { it.key },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = {
                    dev.ofarafal.enklawa.miniapp.MiniAppStore.uninstall(env.vault, m.id)
                    Toast.makeText(ctx, "Odinstalowano ${m.name}", Toast.LENGTH_SHORT).show()
                }) { Text("Odinstaluj", color = MaterialTheme.colorScheme.error) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }, modifier = Modifier.weight(1f)) { Text("Z pliku .zip") }
            OutlinedButton(onClick = {
                runCatching {
                    dev.ofarafal.enklawa.miniapp.MiniAppStore.install(env.vault, dev.ofarafal.enklawa.miniapp.Examples.passwordGenerator)
                    dev.ofarafal.enklawa.miniapp.MiniAppStore.install(env.vault, dev.ofarafal.enklawa.miniapp.Examples.todo)
                }.onSuccess { Toast.makeText(ctx, "Zainstalowano przykłady", Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(ctx, "Błąd: ${it.message}", Toast.LENGTH_LONG).show() }
            }, modifier = Modifier.weight(1f)) { Text("Przykłady") }
        }

        Text("Polecane", color = Xp.Link, modifier = Modifier.padding(top = 4.dp))
        WebApps.presets.forEach { p ->
            val isInstalled = AppStore.find(p.url) != null
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C2530))
                    .clickable { env.open(WebApps.idFor(p.url)) }
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(Glyph.Globe, Modifier.size(28.dp), tint = Xp.Link)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(p.name, color = MaterialTheme.colorScheme.onSurface)
                    Text(p.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isInstalled) {
                    Text("zainstalowana", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                } else {
                    TextButton(onClick = {
                        AppStore.install(env.vault, InstalledApp(p.name, p.url), null)
                        Toast.makeText(ctx, "Zainstalowano ${p.name}", Toast.LENGTH_SHORT).show()
                    }) { Text("Zainstaluj") }
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = custom.isNotBlank(),
                onClick = {
                    val url = UrlInput.resolve(custom)
                    if (url.startsWith("http")) env.open(WebApps.idFor(url))
                },
                modifier = Modifier.weight(1f),
            ) { Text("Otwórz") }
            OutlinedButton(
                enabled = custom.isNotBlank(),
                onClick = {
                    val origin = AppCatalog.originOf(UrlInput.resolve(custom))
                    if (origin == null) {
                        Toast.makeText(ctx, "Podaj adres http lub https", Toast.LENGTH_SHORT).show()
                    } else {
                        AppStore.install(env.vault, InstalledApp(AppCatalog.displayName(null, origin), origin), null)
                        Toast.makeText(ctx, "Zainstalowano", Toast.LENGTH_SHORT).show()
                        custom = ""
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Zainstaluj") }
        }
        Text(
            "Wskazówka: w przeglądarce przycisk „aplikacje” przy pasku adresu instaluje bieżącą stronę razem z jej ikoną.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
