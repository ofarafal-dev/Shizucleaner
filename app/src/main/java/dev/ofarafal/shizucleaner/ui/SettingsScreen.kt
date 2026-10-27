package dev.ofarafal.shizucleaner.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ofarafal.shizucleaner.BuildConfig

@Composable
fun SettingsScreen(onOpenUpdates: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Bezpieczne kasowanie", style = MaterialTheme.typography.titleMedium)
        Text(
            "Ile razy nadpisać zawartość pliku losowymi danymi przed skasowaniem (resztki, duże pliki, duplikaty, APK).",
            Modifier.padding(top = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Wyłączone", 1 to "1 przebieg", 3 to "3 przebiegi").forEach { (p, label) ->
                FilterChip(selected = ui.passes == p, onClick = { vm.setPasses(p) }, label = { Text(label) })
            }
        }
        Text(FLASH_NOTE, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)

        Text("Baza pakietów", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleMedium)
        Text(
            "Oceny ryzyka aplikacji systemowych i foldery po znanych aplikacjach. Pobiera aktualną listę z repozytorium " +
                "bez wydawania nowej wersji. Pakiety krytyczne są zabezpieczone w kodzie i baza ich nie odblokuje.",
            Modifier.padding(top = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = vm::updateDb, enabled = !ui.dbBusy) {
            Text(if (ui.dbBusy) "Pobieram…" else "Aktualizuj bazę")
        }
        ui.dbMessage?.let { Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall) }

        Text("Aplikacja", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleMedium)
        Text(
            "Wersja ${BuildConfig.VERSION_NAME}",
            Modifier.padding(top = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onOpenUpdates) { Text("Aktualizacje aplikacji") }
        Text(
            "Brak analityki. Internet służy tylko do pobierania aktualizacji aplikacji i bazy pakietów z GitHuba.",
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
