package dev.ofarafal.shizucleaner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ofarafal.shizucleaner.data.AppEntry
import dev.ofarafal.shizucleaner.data.PkgAction
import dev.ofarafal.shizucleaner.data.Risk
import dev.ofarafal.shizucleaner.shizuku.ShizukuState

@Composable
fun DebloatScreen(vm: MainViewModel, shizuku: ShizukuState) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val visible = remember(ui) { ui.visible() }
    val restore = ui.screen == Screen.RESTORE

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = ui.screen.ordinal) {
            Screen.entries.forEach { s ->
                Tab(selected = ui.screen == s, onClick = { vm.setScreen(s) }, text = { Text(s.title) })
            }
        }

        OutlinedTextField(
            value = ui.query,
            onValueChange = vm::setQuery,
            label = { Text("Szukaj (nazwa lub pakiet)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )

        if (!restore) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(Risk.SAFE, Risk.CAUTION, Risk.UNKNOWN).forEach { r ->
                    FilterChip(
                        selected = r in ui.riskFilter,
                        onClick = { vm.toggleRisk(r) },
                        label = { Text("${r.label} (${ui.count(r)})") },
                    )
                }
            }
            Row(Modifier.padding(horizontal = 4.dp)) {
                TextButton(onClick = vm::selectSafe) { Text("Zaznacz bezpieczne") }
                TextButton(onClick = vm::clearSelection) { Text("Wyczyść") }
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                ui.loading && ui.apps.isEmpty() -> BusyBox("Wczytuję aplikacje…")
                visible.isEmpty() -> Hint(if (restore) "Nic do przywrócenia." else "Brak aplikacji dla wybranych filtrów.")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(visible, key = { it.pkg }) { app ->
                        AppRow(app, checked = app.pkg in ui.selected, onToggle = { vm.toggle(app.pkg) })
                    }
                }
            }
        }

        ActionBar(
            label = if (restore) "Przywróć (${ui.selected.size})" else "Usuń / wyłącz (${ui.selected.size})",
            enabled = shizuku == ShizukuState.READY && ui.progress == null && ui.selected.isNotEmpty(),
            progress = ui.progress,
            onClick = { vm.showConfirm(true) },
        )
    }

    if (ui.confirm) {
        ConfirmDialog(
            restore = restore,
            selected = ui.selectedEntries(),
            onDismiss = { vm.showConfirm(false) },
            onConfirm = vm::run,
        )
    }

    ui.results?.let { results ->
        val ok = results.count { it.ok }
        val failed = results.filter { !it.ok }
        ReportDialog(
            title = "Gotowe: $ok / ${results.size}",
            text = if (failed.isEmpty()) "Wszystko się udało." else failed.joinToString("\n\n") { "${it.label}: ${it.message}" },
            onDismiss = vm::dismissResults,
        )
    }
}

@Composable
private fun AppRow(app: AppEntry, checked: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        leadingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle() }) },
        headlineContent = { Text(app.label) },
        supportingContent = {
            Column {
                Text(app.pkg, style = MaterialTheme.typography.bodySmall)
                app.known?.note?.takeIf { it.isNotEmpty() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (!app.installed) Text("usunięta dla użytkownika", style = MaterialTheme.typography.bodySmall)
                else if (!app.enabled) Text("wyłączona", style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = { RiskBadge(app.risk) },
    )
}

@Composable
private fun RiskBadge(risk: Risk) {
    val color = when (risk) {
        Risk.SAFE -> Color(0xFF2E7D32)
        Risk.CAUTION -> Color(0xFFEF6C00)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(risk.label, color = color, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun ConfirmDialog(
    restore: Boolean,
    selected: List<AppEntry>,
    onDismiss: () -> Unit,
    onConfirm: (PkgAction) -> Unit,
) {
    var mode by remember { mutableStateOf(PkgAction.UNINSTALL) }
    val caution = selected.count { it.risk == Risk.CAUTION }
    val unknown = selected.count { it.risk == Risk.UNKNOWN }
    val action = if (restore) PkgAction.RESTORE else mode

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (restore) "Przywrócić ${selected.size}?" else "Zastosować dla ${selected.size}?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (!restore) {
                    ModeOption(
                        "Usuń dla użytkownika 0",
                        "Dane zostają, APK zostaje w systemie — można przywrócić.",
                        mode == PkgAction.UNINSTALL,
                    ) { mode = PkgAction.UNINSTALL }
                    ModeOption(
                        "Wyłącz",
                        "Najłagodniejsze. Aplikacja znika i nie działa, zero ryzyka dla danych.",
                        mode == PkgAction.DISABLE,
                    ) { mode = PkgAction.DISABLE }
                    if (caution > 0 || unknown > 0) {
                        Text(
                            "W zaznaczeniu: $caution „ostrożnie”, $unknown nieznanych. " +
                                "Mogą być powiązane z innymi funkcjami telefonu — przy wątpliwościach wybierz Wyłącz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                } else {
                    Text("Aplikacje zostaną zainstalowane z powrotem dla użytkownika 0 i włączone.")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(action) }) { Text("Wykonaj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@Composable
private fun ModeOption(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}
