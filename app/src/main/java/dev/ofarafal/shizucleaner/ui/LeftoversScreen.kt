package dev.ofarafal.shizucleaner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ofarafal.shizucleaner.shizuku.ShizukuState

@Composable
fun LeftoversScreen(shizuku: ShizukuState, vm: LeftoversViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ready = shizuku == ShizukuState.READY
    val busy = ui.progress != null

    Column(Modifier.fillMaxSize()) {
        Text(
            "Foldery w Android/data, obb i media po aplikacjach, których już nie ma na telefonie, " +
                "oraz znane foldery (np. Telegram, WhatsApp). Aplikacje usunięte debloatem tylko dla użytkownika " +
                "są pomijane, bo ich dane zostają celowo.",
            Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = vm::scan, enabled = ready && !ui.scanning && !busy) {
                Text(if (ui.scanned) "Skanuj ponownie" else "Skanuj")
            }
            TextButton(onClick = vm::selectAll, enabled = ui.items.isNotEmpty()) { Text("Zaznacz wszystko") }
            TextButton(onClick = vm::clearSelection, enabled = ui.selected.isNotEmpty()) { Text("Odznacz") }
        }
        ui.error?.let { Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error) }

        Box(Modifier.weight(1f)) {
            when {
                ui.scanning -> BusyBox("Skanuję foldery aplikacji…")
                !ui.scanned -> Hint("Naciśnij „Skanuj”, żeby wyszukać resztki.")
                ui.items.isEmpty() -> Hint("Nie znaleziono resztek.")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(ui.items, key = { it.path }) { item ->
                        SelectRow(
                            title = item.pkg,
                            subtitle = "${item.kind.label} · ostatnia zmiana ${formatDate(item.mtime)}",
                            trailing = formatSize(item.size),
                            checked = item.path in ui.selected,
                            onToggle = { vm.toggle(item.path) },
                        )
                    }
                }
            }
        }

        val freed = ui.items.filter { it.path in ui.selected }.sumOf { it.size }
        ActionBar(
            label = "Usuń zaznaczone (${ui.selected.size}, ${formatSize(freed)})",
            enabled = ready && !busy && ui.selected.isNotEmpty(),
            progress = ui.progress,
            onClick = { vm.askConfirm(true) },
        )
    }

    if (ui.confirm) {
        ConfirmDeleteDialog(
            title = "Usunąć ${ui.selected.size} folderów?",
            body = "Zostaną skasowane foldery po aplikacjach, których nie ma na telefonie. " +
                "Mogą zawierać kopie zapasowe czatów i media — jeśli zamierzasz zainstalować aplikację ponownie, " +
                "najpierw sprawdź, czy tych danych nie potrzebujesz.",
            passes = vm.passes,
            confirmLabel = "Usuń",
            onConfirm = vm::deleteSelected,
            onDismiss = { vm.askConfirm(false) },
        )
    }

    ui.report?.let { ReportDialog("Gotowe", it, vm::dismissReport) }
}
