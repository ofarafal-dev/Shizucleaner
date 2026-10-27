package dev.ofarafal.shizucleaner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ofarafal.shizucleaner.shizuku.ShizukuState

@Composable
fun AppsScreen(shizuku: ShizukuState, vm: AppsViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ready = shizuku == ShizukuState.READY
    val visible = ui.visible()
    val busy = ui.progress != null

    // Rozmiary czytamy dopiero, gdy Shizuku jest gotowe; po zmianie stanu odświeżamy listę.
    LaunchedEffect(ready) { vm.load(ready) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { vm.askConfirm(AppsAction.TRIM_CACHES) }, enabled = ready && !busy) {
                Text("Wyczyść cache wszystkich")
            }
            TextButton(onClick = { vm.load(ready) }, enabled = !ui.loading && !busy) { Text("Odśwież") }
        }

        OutlinedTextField(
            value = ui.query,
            onValueChange = vm::setQuery,
            label = { Text("Szukaj (nazwa lub pakiet)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        )

        Box(Modifier.weight(1f)) {
            when {
                ui.loading && ui.apps.isEmpty() -> BusyBox("Wczytuję aplikacje i rozmiary…")
                visible.isEmpty() -> Hint("Brak aplikacji.")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(visible, key = { it.pkg }) { app ->
                        val sizes = if (app.total > 0) {
                            "aplikacja ${formatSize(app.appBytes)} · dane ${formatSize(app.dataBytes)} · cache ${formatSize(app.cacheBytes)}"
                        } else {
                            null
                        }
                        SelectRow(
                            title = app.label,
                            subtitle = listOfNotNull(app.pkg, sizes).joinToString("\n"),
                            trailing = if (app.total > 0) formatSize(app.total) else null,
                            checked = app.pkg in ui.selected,
                            onToggle = { vm.toggle(app.pkg) },
                        )
                    }
                }
            }
        }

        val n = ui.selected.size
        Column(Modifier.fillMaxWidth()) {
            ActionBar(
                label = "Odinstaluj i wyczyść resztki ($n)",
                enabled = ready && !busy && n > 0,
                progress = ui.progress,
                onClick = { vm.askConfirm(AppsAction.UNINSTALL) },
            )
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                TextButton(onClick = { vm.askConfirm(AppsAction.CLEAR_DATA) }, enabled = ready && !busy && n > 0) {
                    Text("Wyczyść dane ($n)")
                }
                TextButton(onClick = vm::clearSelection, enabled = n > 0) { Text("Odznacz") }
            }
        }
    }

    when (ui.confirm) {
        AppsAction.UNINSTALL -> AlertDialog(
            onDismissRequest = { vm.askConfirm(null) },
            title = { Text("Odinstalować ${ui.selected.size}?") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Aplikacje zostaną usunięte razem z danymi: " + ui.selectedApps().take(5).joinToString { it.label } +
                            if (ui.selected.size > 5) " i inne." else ".",
                    )
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ui.wipeLeftovers, onCheckedChange = vm::setWipe)
                        Text(
                            "Usuń też resztki w pamięci (Android/data, obb, media, znane foldery) — ${passesText(vm.passes)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        "Operacji nie da się cofnąć. Kopie zapasowe czatów i media zapisane w pamięci przepadną razem z resztkami.",
                        Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (ui.wipeLeftovers && vm.passes > 0) {
                        Text(FLASH_NOTE, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.run(ready) }) { Text("Odinstaluj") } },
            dismissButton = { TextButton(onClick = { vm.askConfirm(null) }) { Text("Anuluj") } },
        )

        AppsAction.CLEAR_DATA -> AlertDialog(
            onDismissRequest = { vm.askConfirm(null) },
            title = { Text("Wyczyścić dane ${ui.selected.size} aplikacji?") },
            text = { Text("To jak „Wyczyść dane” w ustawieniach: wylogowanie, utrata ustawień i lokalnych danych tych aplikacji. Aplikacje zostają zainstalowane.") },
            confirmButton = { TextButton(onClick = { vm.run(ready) }) { Text("Wyczyść") } },
            dismissButton = { TextButton(onClick = { vm.askConfirm(null) }) { Text("Anuluj") } },
        )

        AppsAction.TRIM_CACHES -> AlertDialog(
            onDismissRequest = { vm.askConfirm(null) },
            title = { Text("Wyczyścić cache wszystkich aplikacji?") },
            text = { Text("Usuwa tylko pamięć podręczną (pliki tymczasowe). Dane i logowania zostają. Aplikacje mogą chwilę wolniej startować.") },
            confirmButton = { TextButton(onClick = { vm.run(ready) }) { Text("Wyczyść") } },
            dismissButton = { TextButton(onClick = { vm.askConfirm(null) }) { Text("Anuluj") } },
        )

        null -> Unit
    }

    ui.results?.let { results ->
        val failed = results.filter { !it.ok }
        ReportDialog(
            title = "Gotowe: ${results.count { it.ok }} / ${results.size}",
            text = if (failed.isEmpty()) "Wszystko się udało." else failed.joinToString("\n\n") { "${it.label}: ${it.message}" },
            onDismiss = vm::dismissResults,
        )
    }
}
