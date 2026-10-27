package dev.ofarafal.shizucleaner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ofarafal.shizucleaner.ShizuApp
import dev.ofarafal.shizucleaner.data.ActionResult
import dev.ofarafal.shizucleaner.data.PermApp
import dev.ofarafal.shizucleaner.data.PermCategory
import dev.ofarafal.shizucleaner.data.RuntimePermissions
import dev.ofarafal.shizucleaner.data.UserApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- Zapora

data class FirewallUi(
    val loading: Boolean = false,
    val apps: List<UserApp> = emptyList(),
    val blocked: Set<String> = emptySet(),
    val query: String = "",
    val includeSystem: Boolean = false,
    val busyPkg: String? = null,
    val message: String? = null,
) {
    fun visible(): List<UserApp> {
        val q = query.trim()
        return apps.filter { q.isEmpty() || it.label.contains(q, true) || it.pkg.contains(q, true) }
            // zablokowane na górze, reszta alfabetycznie
            .sortedWith(compareByDescending<UserApp> { it.pkg in blocked }.thenBy { it.label.lowercase() })
    }
}

class FirewallViewModel(app: Application) : AndroidViewModel(app) {

    private val sh = app as ShizuApp
    private val _ui = MutableStateFlow(FirewallUi(blocked = sh.firewall.blocked()))
    val ui: StateFlow<FirewallUi> = _ui

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, blocked = sh.firewall.blocked()) }
            val apps = sh.apps.load(readSizes = false, includeSystem = _ui.value.includeSystem)
            _ui.update { it.copy(loading = false, apps = apps) }
        }
    }

    fun setQuery(q: String) = _ui.update { it.copy(query = q) }

    fun setIncludeSystem(on: Boolean) {
        _ui.update { it.copy(includeSystem = on) }
        load()
    }

    fun toggle(app: UserApp, block: Boolean) {
        if (_ui.value.busyPkg != null) return
        viewModelScope.launch {
            _ui.update { it.copy(busyPkg = app.pkg, message = null) }
            val r: ActionResult = sh.firewall.setBlocked(app.pkg, app.label, block)
            _ui.update {
                it.copy(
                    busyPkg = null,
                    blocked = sh.firewall.blocked(),
                    message = if (r.ok) null else "${app.label}: ${r.message}",
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Uprawnienia

data class PermissionsUi(
    val category: PermCategory = RuntimePermissions.categories.first(),
    val includeSystem: Boolean = false,
    val loading: Boolean = false,
    val apps: List<PermApp> = emptyList(),
    val selected: Set<String> = emptySet(),
    val query: String = "",
    val confirm: Boolean = false,
    val progress: Pair<Int, Int>? = null,
    val report: String? = null,
) {
    fun visible(): List<PermApp> {
        val q = query.trim()
        return apps.filter { q.isEmpty() || it.label.contains(q, true) || it.pkg.contains(q, true) }
    }

    fun selectedApps(): List<PermApp> = apps.filter { it.pkg in selected }
}

class PermissionsViewModel(app: Application) : AndroidViewModel(app) {

    private val sh = app as ShizuApp
    private val _ui = MutableStateFlow(PermissionsUi())
    val ui: StateFlow<PermissionsUi> = _ui

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            val s = _ui.value
            val apps = sh.permissions.appsWith(s.category, s.includeSystem)
            _ui.update { it.copy(loading = false, apps = apps, selected = it.selected.intersect(apps.map { a -> a.pkg }.toSet())) }
        }
    }

    fun setCategory(c: PermCategory) {
        _ui.update { it.copy(category = c, selected = emptySet(), apps = emptyList()) }
        load()
    }

    fun setIncludeSystem(on: Boolean) {
        _ui.update { it.copy(includeSystem = on, selected = emptySet()) }
        load()
    }

    fun setQuery(q: String) = _ui.update { it.copy(query = q) }
    fun toggle(pkg: String) = _ui.update { it.copy(selected = if (pkg in it.selected) it.selected - pkg else it.selected + pkg) }
    fun selectAllVisible() = _ui.update { it.copy(selected = it.selected + it.visible().map { a -> a.pkg }) }
    fun clearSelection() = _ui.update { it.copy(selected = emptySet()) }
    fun askConfirm(show: Boolean) = _ui.update { it.copy(confirm = show) }
    fun dismissReport() = _ui.update { it.copy(report = null) }

    fun revokeSelected() {
        val targets = _ui.value.selectedApps()
        if (targets.isEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(confirm = false, progress = 0 to targets.size) }
            val results = ArrayList<ActionResult>()
            targets.forEachIndexed { i, app ->
                results += sh.permissions.revoke(app)
                _ui.update { it.copy(progress = (i + 1) to targets.size) }
            }
            val failed = results.filter { !it.ok }
            val report = "Cofnięto uprawnienie w ${results.count { it.ok }} z ${results.size} aplikacji." +
                if (failed.isEmpty()) "" else "\n\nBłędy:\n" + failed.joinToString("\n") { "• ${it.label}: ${it.message}" }
            _ui.update { it.copy(progress = null, report = report, selected = emptySet()) }
            load()
        }
    }
}
