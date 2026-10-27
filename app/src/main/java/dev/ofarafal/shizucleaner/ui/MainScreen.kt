package dev.ofarafal.shizucleaner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ofarafal.shizucleaner.BuildConfig
import dev.ofarafal.shizucleaner.update.CheckResult

private enum class Tab(val label: String, val glyph: String) {
    DEBLOAT("Debloat", "🗑"),
    APPS("Aplikacje", "📦"),
    CLEAN("Czyszczenie", "🧹"),
    PRIVACY("Prywatność", "🛡"),
    SETTINGS("Ustawienia", "⚙"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val shizuku by vm.shizukuState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.DEBLOAT) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ShizuCleaner") },
                actions = { TextButton(onClick = { vm.openUpdate(true) }) { Text("Aktualizacje") } },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Text(t.glyph) },
                        label = { Text(t.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ShizukuBanner(shizuku, onRequest = vm::requestPermission, onRetry = vm::onResume)

            (ui.update.result as? CheckResult.Available)?.takeIf { !ui.update.dialog }?.let { available ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { vm.openUpdate(true) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Text(
                        "Dostępna aktualizacja ${available.info.versionName}. Kliknij, aby zainstalować.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            when (tab) {
                Tab.DEBLOAT -> DebloatScreen(vm, shizuku)
                Tab.APPS -> AppsScreen(shizuku)
                Tab.CLEAN -> CleanScreen(shizuku)
                Tab.PRIVACY -> PrivacyScreen(shizuku)
                Tab.SETTINGS -> SettingsScreen(onOpenUpdates = { vm.openUpdate(true) })
            }
        }
    }

    if (ui.update.dialog) {
        UpdateDialog(
            state = ui.update,
            onDismiss = { vm.openUpdate(false) },
            onCheck = { vm.checkUpdate() },
            onInstall = vm::startUpdate,
            onAutoCheck = vm::setAutoCheck,
        )
    }
}

@Composable
private fun UpdateDialog(
    state: UpdateUi,
    onDismiss: () -> Unit,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onAutoCheck: (Boolean) -> Unit,
) {
    val result = state.result
    val busy = state.checking || state.downloading != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aktualizacje") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Zainstalowana wersja: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)

                Text(
                    when {
                        state.checking -> "Sprawdzam…"
                        result is CheckResult.Available -> "Dostępna: ${result.info.versionName}"
                        result is CheckResult.UpToDate -> "Masz najnowszą wersję."
                        result is CheckResult.Error -> result.message
                        else -> "Nie sprawdzono jeszcze."
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result is CheckResult.Error) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                )

                if (result is CheckResult.Available && result.info.notes.isNotBlank()) {
                    Text(result.info.notes.trim(), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                }

                state.downloading?.let {
                    LinearProgressIndicator(
                        progress = { it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }

                state.message?.let {
                    Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Sprawdzaj przy starcie", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = state.autoCheck, onCheckedChange = onAutoCheck)
                }
                Text(
                    "Zapytanie do api.github.com o najnowsze wydanie.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            if (result is CheckResult.Available) {
                TextButton(onClick = onInstall, enabled = !busy) { Text("Pobierz i zainstaluj") }
            } else {
                TextButton(onClick = onCheck, enabled = !busy) { Text("Sprawdź") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } },
    )
}
