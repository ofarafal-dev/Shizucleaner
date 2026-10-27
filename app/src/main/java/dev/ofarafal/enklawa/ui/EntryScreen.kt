package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ofarafal.enklawa.Prefs
import dev.ofarafal.enklawa.crypto.SessionKey
import dev.ofarafal.enklawa.vault.UnlockResult
import dev.ofarafal.enklawa.vault.VaultInfo
import dev.ofarafal.enklawa.vault.VaultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EntryScreen(
    vaults: VaultStore,
    onEnter: (amnesia: Boolean) -> Unit,
    onVaultUnlocked: (VaultInfo, SessionKey) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var amnesia by remember { mutableStateOf(false) } // domyślnie wyłączona
    var list by remember { mutableStateOf(vaults.list()) }
    var unlocking by remember { mutableStateOf<VaultInfo?>(null) }
    var deleting by remember { mutableStateOf<VaultInfo?>(null) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var newPass2 by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1220), Color(0xFF132038), Color(0xFF0B1220)))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphIcon(Glyph.Logo, Modifier.size(52.dp))
                Text(
                    "Enklawa",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
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

            Spacer(Modifier.height(8.dp))
            Text("Sejfy (z pamięcią i hasłem)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Każdy sejf to osobny świat: pliki i notatki zostają na telefonie, zaszyfrowane kluczem z hasła (Argon2id) i kluczem z Android Keystore. " +
                    "Zapomnisz hasło — nie ma odzyskiwania. Reset fabryczny telefonu też niszczy sejfy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (list.isEmpty()) {
                Text("Brak sejfów.", style = MaterialTheme.typography.bodySmall)
            }
            list.forEach { v ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C2530))
                        .clickable { password = ""; error = null; unlocking = v }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlyphIcon(Glyph.Lock, Modifier.size(24.dp), tint = Xp.Accent)
                    Text(v.name, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    TextButton(onClick = { deleting = v }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
                }
            }

            Text("Nowy sejf", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            PrivateEditText(newName, { newName = it }, singleLine = true, hint = "nazwa sejfu", modifier = Modifier.fillMaxWidth())
            PrivateEditText(newPass, { newPass = it }, singleLine = true, hint = "hasło (min. ${VaultStore.MIN_PASSWORD} znaków)", password = true, modifier = Modifier.fillMaxWidth())
            PrivateEditText(newPass2, { newPass2 = it }, singleLine = true, hint = "powtórz hasło", password = true, modifier = Modifier.fillMaxWidth())
            createError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    createError = when {
                        newName.isBlank() -> "Podaj nazwę sejfu"
                        newPass.length < VaultStore.MIN_PASSWORD -> "Hasło ma mieć co najmniej ${VaultStore.MIN_PASSWORD} znaków"
                        newPass != newPass2 -> "Hasła się różnią"
                        else -> null
                    }
                    if (createError == null) {
                        busy = true
                        val name = newName
                        val pw = newPass.toCharArray()
                        scope.launch {
                            val r = withContext(Dispatchers.Default) { runCatching { vaults.create(name, pw) } }
                            pw.fill('\u0000')
                            busy = false
                            r.onSuccess {
                                newName = ""; newPass = ""; newPass2 = ""
                                list = vaults.list()
                            }.onFailure { createError = it.message ?: "Nie udało się utworzyć sejfu" }
                        }
                    }
                },
            ) { Text(if (busy) "Tworzenie…" else "Utwórz sejf") }

            Spacer(Modifier.height(8.dp))
            Text("Czego ta ochrona NIE daje", style = MaterialTheme.typography.titleMedium)
            Text(
                "• To prywatność na co dzień, nie ochrona przed zaawansowanym przeciwnikiem ani malware na telefonie.\n" +
                    "• Android hosta jest niezaufany: logi systemowe, schowek i klawiatura mogą coś zapamiętać. Aplikacja prosi klawiaturę, by się nie uczyła, ale nie może tego wymusić.\n" +
                    "• Klucz (RAM) szyfruje pliki świata. Dane przeglądarki trzyma systemowy WebView poza naszym kluczem; po wyjściu usuwamy cały profil, lecz nie jest to kasowanie kryptograficzne. W sejfie przeglądarka działa na profilu tymczasowym.\n" +
                    "• Adresy, które odwiedzasz, widzi Twoja sieć i odwiedzane strony. Tor jest w aplikacji Tor, ale to systemowy WebView, nie Tor Browser.\n" +
                    "• Nadpisywanie danych na pamięci flash nie daje gwarancji, dlatego opieramy się na zniszczeniu klucza.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )

            Spacer(Modifier.height(8.dp))
            UpdatePanel(autoCheck = Prefs.autoUpdateCheck(ctx), modifier = Modifier.fillMaxWidth())
        }
    }

    unlocking?.let { v ->
        AlertDialog(
            onDismissRequest = { if (!busy) unlocking = null },
            title = { Text("Sejf „${v.name}”") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrivateEditText(password, { password = it }, singleLine = true, hint = "hasło", password = true, modifier = Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    if (busy) Text("Odblokowywanie…", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && password.isNotEmpty(),
                    onClick = {
                        busy = true
                        error = null
                        val pw = password.toCharArray()
                        scope.launch {
                            val r = withContext(Dispatchers.Default) { vaults.unlock(v.id, pw) }
                            pw.fill('\u0000')
                            busy = false
                            when (r) {
                                is UnlockResult.Ok -> { password = ""; unlocking = null; onVaultUnlocked(v, r.key) }
                                UnlockResult.WrongPassword -> error = "Złe hasło"
                                UnlockResult.DeviceKeyLost -> error = "Klucz urządzenia zniknął — tego sejfu nie da się już otworzyć"
                                UnlockResult.Damaged -> error = "Nagłówek sejfu jest uszkodzony"
                            }
                        }
                    },
                ) { Text("Odblokuj") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { unlocking = null }) { Text("Anuluj") } },
        )
    }

    deleting?.let { v ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Usunąć sejf „${v.name}”?") },
            text = { Text("Pliki i klucz zostaną skasowane bezpowrotnie. Hasło nie jest potrzebne, więc upewnij się, że to ten sejf.") },
            confirmButton = {
                TextButton(onClick = { vaults.delete(v.id); list = vaults.list(); deleting = null }) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Anuluj") } },
        )
    }
}
