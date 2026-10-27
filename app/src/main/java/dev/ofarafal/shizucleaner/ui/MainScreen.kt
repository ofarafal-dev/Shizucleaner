package dev.ofarafal.shizucleaner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import dev.ofarafal.shizucleaner.BuildConfig
import dev.ofarafal.shizucleaner.data.AppEntry
import dev.ofarafal.shizucleaner.update.CheckResult
import dev.ofarafal.shizucleaner.data.PkgAction
import dev.ofarafal.shizucleaner.data.Risk
import dev.ofarafal.shizucleaner.shizuku.ShizukuState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val shizuku by vm.shizukuState.collectAsStateWithLifecycle()
    val visible = remember(ui) { ui.visible() }
    val restore = ui.screen == Screen.RESTORE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ShizuCleaner") },
                actions = { TextButton(onClick = { vm.openUpdate(true) }) { Text("Aktualizacje") } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.navigationBarsPadding().padding(12.dp)) {
                    ui.progress?.let { (done, total) ->
                        LinearProgressIndicator(
                            progress = { if (total == 0) 0f else done / total.toFloat() },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        )
                    }
                    Button(
                        onClick = { vm.showConfirm(true) },
                        enabled = shizuku == ShizukuState.READY && ui.progress == null && ui.selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val n = ui.selected.size
                        Text(if (restore) "Przywróć ($n)" else "Usuń / wyłącz ($n)")
                    }
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

            TabRow(selectedTabIndex = ui.screen.ordinal) {
                Screen.entries.forEach { s ->
                    Tab(
                        selected = ui.screen == s,
                        onClick = { vm.setScreen(s) },
                        text = { Text(s.title) },
                    )
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

            when {
                ui.loading && ui.apps.isEmpty() -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                visible.isEmpty() -> Text(
                    if (restore) "Nic do przywrócenia." else "Brak aplikacji dla wybranych filtrów.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )

                else -> LazyColumn(Modifier.weight(1f)) {
                    items(visible, key = { it.pkg }) { app ->
                        AppRow(app, checked = app.pkg in ui.selected, onToggle = { vm.toggle(app.pkg) })
                    }
                }
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
        AlertDialog(
            onDismissRequest = vm::dismissResults,
            confirmButton = { TextButton(onClick = vm::dismissResults) { Text("OK") } },
            title = { Text("Gotowe: $ok / ${results.size}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val failed = results.filter { !it.ok }
                    if (failed.isEmpty()) {
                        Text("Wszystko się udało.")
                    } else {
                        failed.forEach { Text("${it.label}: ${it.message}", Modifier.padding(bottom = 6.dp)) }
                    }
                }
            },
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
                    "Jedyne połączenie aplikacji z internetem: zapytanie do api.github.com o najnowsze wydanie.",
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

@Composable
private fun AppRow(app: AppEntry, checked: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        leadingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle() }) },
        headlineContent = { Text(app.label) },
        supportingContent = {
            Column {
                Text(app.pkg, style = MaterialTheme.typography.bodySmall)
                app.known?.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
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

private data class BannerSpec(val text: String, val buttonLabel: String?, val onClick: (() -> Unit)?)

@Composable
private fun ShizukuBanner(state: ShizukuState, onRequest: () -> Unit, onRetry: () -> Unit) {
    val spec = when (state) {
        ShizukuState.READY -> return
        ShizukuState.NOT_RUNNING -> BannerSpec(
            "Shizuku nie działa. Uruchom aplikację Shizuku (Wireless debugging lub adb) i wróć tutaj.",
            "Sprawdź ponownie", onRetry,
        )
        ShizukuState.UNSUPPORTED -> BannerSpec(
            "Wersja Shizuku jest za stara — zaktualizuj ją.", "Sprawdź ponownie", onRetry,
        )
        ShizukuState.NO_PERMISSION -> BannerSpec(
            "Brak uprawnienia Shizuku dla tej aplikacji. Jeśli prośba się nie pojawia, nadaj je w aplikacji Shizuku.",
            "Nadaj uprawnienie", onRequest,
        )
        ShizukuState.CONNECTING -> BannerSpec("Łączenie z usługą Shizuku…", null, null)
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(spec.text, style = MaterialTheme.typography.bodyMedium)
            if (spec.buttonLabel != null && spec.onClick != null) {
                TextButton(onClick = spec.onClick) { Text(spec.buttonLabel) }
            }
        }
    }
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
