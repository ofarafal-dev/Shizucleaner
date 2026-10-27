package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Notatki zapisują do zaszyfrowanego magazynu sesji (klucz w RAM). Pliki znikają razem ze światem. */
@Composable
fun NotepadApp(env: AppEnv) {
    var text by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("notatka.txt") }
    var menuOpen by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PrivateEditText(
                text = name,
                onTextChange = { name = it },
                singleLine = true,
                hint = "nazwa pliku",
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                val n = name.trim()
                if (n.isEmpty()) {
                    status = "Podaj nazwę pliku"
                } else {
                    env.vault.write(n, text.toByteArray(Charsets.UTF_8))
                    status = "Zapisano \"$n\" w zaszyfrowanym magazynie"
                }
            }) { Text("Zapisz") }
            Box {
                OutlinedButton(onClick = { menuOpen = true }) { Text("Otwórz") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    val names = env.vault.names().filterNot { dev.ofarafal.enklawa.os.AppCatalog.isInternalName(it) }
                    if (names.isEmpty()) {
                        DropdownMenuItem(text = { Text("Brak plików") }, onClick = { menuOpen = false })
                    }
                    names.forEach { n ->
                        DropdownMenuItem(
                            text = { Text(n) },
                            onClick = {
                                menuOpen = false
                                val bytes = runCatching { env.vault.read(n) }.getOrNull()
                                if (bytes == null) {
                                    status = "Nie udało się otworzyć \"$n\""
                                } else {
                                    text = String(bytes, Charsets.UTF_8)
                                    name = n
                                    status = "Otwarto \"$n\""
                                }
                            },
                        )
                    }
                }
            }
        }
        PrivateEditText(
            text = text,
            onTextChange = { text = it },
            hint = "Pisz tutaj…",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Text(
            status ?: (if (env.vaultName != null) "Pliki zostają w sejfie „${env.vaultName}” (zaszyfrowane)." else "Pliki zostają tylko w tej sesji i znikają po wyjściu ze świata."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().background(Xp.Body),
        )
    }
}
