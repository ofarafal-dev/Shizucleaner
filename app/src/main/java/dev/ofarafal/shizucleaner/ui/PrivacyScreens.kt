package dev.ofarafal.shizucleaner.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ofarafal.shizucleaner.data.RuntimePermissions
import dev.ofarafal.shizucleaner.shizuku.ShizukuState

/** Zakładka „Prywatność”: zapora i audyt uprawnień. */
@Composable
fun PrivacyScreen(shizuku: ShizukuState) {
    var sub by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = sub) {
            Tab(selected = sub == 0, onClick = { sub = 0 }, text = { Text("Zapora") })
            Tab(selected = sub == 1, onClick = { sub = 1 }, text = { Text("Uprawnienia") })
        }
        if (sub == 0) FirewallScreen(shizuku) else PermissionsScreen(shizuku)
    }
}

/** Zakładka „Czyszczenie”: resztki po aplikacjach i pliki. */
@Composable
fun CleanScreen(shizuku: ShizukuState) {
    var sub by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = sub) {
            Tab(selected = sub == 0, onClick = { sub = 0 }, text = { Text("Resztki") })
            Tab(selected = sub == 1, onClick = { sub = 1 }, text = { Text("Pliki") })
        }
        if (sub == 0) LeftoversScreen(shizuku) else FilesScreen(shizuku)
    }
}

@Composable
fun FirewallScreen(shizuku: ShizukuState, vm: FirewallViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ready = shizuku == ShizukuState.READY
    val visible = ui.visible()

    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Blokuje dostęp do internetu wybranym aplikacjom bez VPN (cmd connectivity). " +
                "Android kasuje te reguły po restarcie, ale aplikacja nakłada je ponownie, gdy tylko Shizuku wstanie. " +
                "Zablokowane aplikacje działają dalej, tylko bez sieci.",
            Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = ui.includeSystem,
                onClick = { vm.setIncludeSystem(!ui.includeSystem) },
                label = { Text("Pokaż też systemowe") },
            )
            Text("Zablokowane: ${ui.blocked.size}", style = MaterialTheme.typography.labelMedium)
        }
        OutlinedTextField(
            value = ui.query,
            onValueChange = vm::setQuery,
            label = { Text("Szukaj (nazwa lub pakiet)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )
        ui.message?.let { Text(it, Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.error) }

        Box(Modifier.weight(1f)) {
            when {
                ui.loading && ui.apps.isEmpty() -> BusyBox("Wczytuję aplikacje…")
                visible.isEmpty() -> Hint("Brak aplikacji.")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(visible, key = { it.pkg }) { app ->
                        val isBlocked = app.pkg in ui.blocked
                        ListItem(
                            headlineContent = { Text(app.label) },
                            supportingContent = { Text(app.pkg, style = MaterialTheme.typography.bodySmall) },
                            trailingContent = {
                                Switch(
                                    checked = isBlocked,
                                    onCheckedChange = { vm.toggle(app, it) },
                                    enabled = ready && ui.busyPkg == null,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionsScreen(shizuku: ShizukuState, vm: PermissionsViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ready = shizuku == ShizukuState.READY
    val visible = ui.visible()

    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RuntimePermissions.categories.forEach { c ->
                FilterChip(selected = ui.category.id == c.id, onClick = { vm.setCategory(c) }, label = { Text(c.label) })
            }
        }
        Row(
            Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = ui.includeSystem,
                onClick = { vm.setIncludeSystem(!ui.includeSystem) },
                label = { Text("Pokaż też systemowe") },
            )
            Text("${visible.size} aplikacji", style = MaterialTheme.typography.labelMedium)
        }
        OutlinedTextField(
            value = ui.query,
            onValueChange = vm::setQuery,
            label = { Text("Szukaj (nazwa lub pakiet)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Row(Modifier.padding(horizontal = 4.dp)) {
            TextButton(onClick = vm::selectAllVisible, enabled = visible.isNotEmpty()) { Text("Zaznacz wszystkie") }
            TextButton(onClick = vm::clearSelection, enabled = ui.selected.isNotEmpty()) { Text("Odznacz") }
        }

        Box(Modifier.weight(1f)) {
            when {
                ui.loading && ui.apps.isEmpty() -> BusyBox("Sprawdzam uprawnienia…")
                visible.isEmpty() -> Hint("Żadna aplikacja nie ma przyznanego tego uprawnienia.")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(visible, key = { it.pkg }) { app ->
                        SelectRow(
                            title = app.label,
                            subtitle = app.pkg + "\n" + app.granted.joinToString(", ") { it.substringAfterLast('.') },
                            trailing = if (app.isSystem) "systemowa" else null,
                            checked = app.pkg in ui.selected,
                            onToggle = { vm.toggle(app.pkg) },
                        )
                    }
                }
            }
        }

        ActionBar(
            label = "Cofnij „${ui.category.label}” (${ui.selected.size})",
            enabled = ready && ui.progress == null && ui.selected.isNotEmpty(),
            progress = ui.progress,
            onClick = { vm.askConfirm(true) },
        )
    }

    if (ui.confirm) {
        AlertDialog(
            onDismissRequest = { vm.askConfirm(false) },
            title = { Text("Cofnąć uprawnienie w ${ui.selected.size} aplikacjach?") },
            text = {
                Text(
                    "Kategoria: ${ui.category.label}. Aplikacje mogą zostać zrestartowane i przy następnym użyciu " +
                        "poprosić o zgodę ponownie. Uprawnienia można przywrócić w ustawieniach systemowych aplikacji.",
                )
            },
            confirmButton = { TextButton(onClick = vm::revokeSelected) { Text("Cofnij") } },
            dismissButton = { TextButton(onClick = { vm.askConfirm(false) }) { Text("Anuluj") } },
        )
    }

    ui.report?.let { ReportDialog("Gotowe", it, vm::dismissReport) }
}
