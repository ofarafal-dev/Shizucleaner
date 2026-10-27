package dev.ofarafal.shizucleaner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ofarafal.shizucleaner.ShizuApp
import dev.ofarafal.shizucleaner.data.ActionResult
import dev.ofarafal.shizucleaner.data.AppEntry
import dev.ofarafal.shizucleaner.data.DebloatRepository
import dev.ofarafal.shizucleaner.data.PkgAction
import dev.ofarafal.shizucleaner.data.Risk
import dev.ofarafal.shizucleaner.update.CheckResult
import dev.ofarafal.shizucleaner.update.Updater
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen(val title: String) {
    DEBLOAT("Debloat"),
    RESTORE("Przywróć"),
}

data class UpdateUi(
    val dialog: Boolean = false,
    val checking: Boolean = false,
    val result: CheckResult? = null,
    val downloading: Float? = null,
    val message: String? = null,
    val autoCheck: Boolean = true,
)

data class UiState(
    val update: UpdateUi = UpdateUi(),
    val loading: Boolean = true,
    val apps: List<AppEntry> = emptyList(),
    val selected: Set<String> = emptySet(),
    val query: String = "",
    val riskFilter: Set<Risk> = setOf(Risk.SAFE, Risk.CAUTION),
    val screen: Screen = Screen.DEBLOAT,
    val confirm: Boolean = false,
    val progress: Pair<Int, Int>? = null,
    val results: List<ActionResult>? = null,
) {
    private fun debloatPool() =
        apps.filter { it.isSystem && it.installed && it.enabled && it.risk != Risk.CRITICAL }

    private fun restorePool() =
        apps.filter { it.isSystem && (!it.installed || !it.enabled) }

    fun count(risk: Risk): Int = debloatPool().count { it.risk == risk }

    fun visible(): List<AppEntry> {
        val q = query.trim()
        val base = when (screen) {
            Screen.DEBLOAT -> debloatPool().filter { it.risk in riskFilter }
            Screen.RESTORE -> restorePool()
        }
        return base
            .filter { q.isEmpty() || it.label.contains(q, ignoreCase = true) || it.pkg.contains(q, ignoreCase = true) }
            .sortedWith(compareBy<AppEntry>({ it.risk.ordinal }, { it.label.lowercase() }))
    }

    fun selectedEntries(): List<AppEntry> = apps.filter { it.pkg in selected }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val client = (app as ShizuApp).shizuku
    private val repo = DebloatRepository(app, client)

    val shizukuState = client.state

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private val prefs = app.getSharedPreferences("settings", 0)

    init {
        refresh()
        val auto = prefs.getBoolean(KEY_AUTO_CHECK, true)
        _ui.update { it.copy(update = it.update.copy(autoCheck = auto)) }
        if (auto) checkUpdate(silent = true)
    }

    fun openUpdate(show: Boolean) = _ui.update { it.copy(update = it.update.copy(dialog = show, message = null)) }

    fun setAutoCheck(on: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK, on).apply()
        _ui.update { it.copy(update = it.update.copy(autoCheck = on)) }
    }

    /** silent = start aplikacji: błędy sieci są ignorowane, a dostępna wersja pokazuje tylko baner. */
    fun checkUpdate(silent: Boolean = false) {
        if (_ui.value.update.checking) return
        viewModelScope.launch {
            _ui.update { it.copy(update = it.update.copy(checking = true, message = null)) }
            val result = Updater.check()
            _ui.update {
                val keep = if (silent && result is CheckResult.Error) null else result
                it.copy(update = it.update.copy(checking = false, result = keep))
            }
        }
    }

    fun startUpdate() {
        val info = (_ui.value.update.result as? CheckResult.Available)?.info ?: return
        val app = getApplication<Application>()
        if (!app.packageManager.canRequestPackageInstalls()) {
            app.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            _ui.update {
                it.copy(update = it.update.copy(message = "Zezwól na instalację z tego źródła, wróć i kliknij ponownie."))
            }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(update = it.update.copy(downloading = 0f, message = null)) }
            try {
                val apk = Updater.download(app, info) { p ->
                    _ui.update { s -> s.copy(update = s.update.copy(downloading = p)) }
                }
                Updater.install(app, apk)
                _ui.update { it.copy(update = it.update.copy(downloading = null, message = "Instalowanie…")) }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(update = it.update.copy(downloading = null, message = e.message ?: "Błąd aktualizacji"))
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            val apps = repo.loadApps()
            val known = apps.map { it.pkg }.toSet()
            _ui.update { it.copy(loading = false, apps = apps, selected = it.selected.intersect(known)) }
        }
    }

    fun onResume() = client.refresh()
    fun requestPermission() = client.requestPermission()

    fun setScreen(screen: Screen) =
        _ui.update { it.copy(screen = screen, selected = emptySet(), query = "") }

    fun setQuery(q: String) = _ui.update { it.copy(query = q) }

    fun toggleRisk(risk: Risk) = _ui.update {
        it.copy(riskFilter = if (risk in it.riskFilter) it.riskFilter - risk else it.riskFilter + risk)
    }

    fun toggle(pkg: String) = _ui.update {
        it.copy(selected = if (pkg in it.selected) it.selected - pkg else it.selected + pkg)
    }

    fun selectSafe() = _ui.update { s ->
        s.copy(selected = s.selected + s.visible().filter { it.risk == Risk.SAFE }.map { it.pkg })
    }

    fun clearSelection() = _ui.update { it.copy(selected = emptySet()) }

    fun showConfirm(show: Boolean) = _ui.update { it.copy(confirm = show) }
    fun dismissResults() = _ui.update { it.copy(results = null) }

    fun run(action: PkgAction) {
        val targets = _ui.value.selectedEntries()
        if (targets.isEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(confirm = false, progress = 0 to targets.size) }
            val results = repo.apply(action, targets) { done ->
                _ui.update { it.copy(progress = done to targets.size) }
            }
            _ui.update { it.copy(progress = null, results = results, selected = emptySet()) }
            refresh()
        }
    }

    private companion object {
        const val KEY_AUTO_CHECK = "auto_check_updates"
    }
}
