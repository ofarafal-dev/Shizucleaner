package dev.ofarafal.shizucleaner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ofarafal.shizucleaner.ShizuApp
import dev.ofarafal.shizucleaner.data.ActionResult
import dev.ofarafal.shizucleaner.data.ApkBackup
import dev.ofarafal.shizucleaner.data.CleanerRepository
import dev.ofarafal.shizucleaner.data.DbUpdater
import dev.ofarafal.shizucleaner.data.DupGroup
import dev.ofarafal.shizucleaner.data.FileHit
import dev.ofarafal.shizucleaner.data.Leftover
import dev.ofarafal.shizucleaner.data.UserApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun Application.shizu() = this as ShizuApp

// ---------------------------------------------------------------- Aplikacje

enum class AppsAction { UNINSTALL, CLEAR_DATA, TRIM_CACHES }

data class AppsUi(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val apps: List<UserApp> = emptyList(),
    val selected: Set<String> = emptySet(),
    val query: String = "",
    val confirm: AppsAction? = null,
    val wipeLeftovers: Boolean = true,
    val backupApk: Boolean = true,
    val progress: Pair<Int, Int>? = null,
    val results: List<ActionResult>? = null,
    val backups: List<ApkBackup>? = null,
    val backupsBusy: Boolean = false,
    val backupsMessage: String? = null,
) {
    fun visible(): List<UserApp> {
        val q = query.trim()
        return apps.filter { q.isEmpty() || it.label.contains(q, true) || it.pkg.contains(q, true) }
            .sortedWith(compareByDescending<UserApp> { it.total }.thenBy { it.label.lowercase() })
    }

    fun selectedApps(): List<UserApp> = apps.filter { it.pkg in selected }
}

class AppsViewModel(app: Application) : AndroidViewModel(app) {

    private val sh = app.shizu()
    private val _ui = MutableStateFlow(AppsUi())
    val ui: StateFlow<AppsUi> = _ui

    val passes: Int get() = sh.settings.overwritePasses

    fun load(shizukuReady: Boolean) {
        // Gdy Shizuku właśnie stało się gotowe, przeładowujemy też w trakcie wczytywania (żeby doczytać rozmiary).
        if (_ui.value.loading && !shizukuReady) return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            val apps = sh.apps.load(readSizes = shizukuReady)
            _ui.update {
                it.copy(loading = false, loaded = true, apps = apps, selected = it.selected.intersect(apps.map { a -> a.pkg }.toSet()))
            }
        }
    }

    fun setQuery(q: String) = _ui.update { it.copy(query = q) }
    fun toggle(pkg: String) = _ui.update { it.copy(selected = if (pkg in it.selected) it.selected - pkg else it.selected + pkg) }
    fun clearSelection() = _ui.update { it.copy(selected = emptySet()) }
    fun askConfirm(action: AppsAction?) = _ui.update { it.copy(confirm = action) }
    fun setWipe(on: Boolean) = _ui.update { it.copy(wipeLeftovers = on) }
    fun setBackupApk(on: Boolean) = _ui.update { it.copy(backupApk = on) }

    // ---- kopie APK
    fun openBackups() {
        _ui.update { it.copy(backups = emptyList(), backupsBusy = true, backupsMessage = null) }
        reloadBackups()
    }

    fun closeBackups() = _ui.update { it.copy(backups = null, backupsMessage = null) }

    private fun reloadBackups() {
        viewModelScope.launch {
            try {
                val list = sh.cleaner.listBackups()
                _ui.update { it.copy(backups = list, backupsBusy = false) }
            } catch (e: Exception) {
                _ui.update { it.copy(backupsBusy = false, backupsMessage = e.message ?: "Błąd odczytu kopii") }
            }
        }
    }

    fun restoreBackup(b: ApkBackup) {
        viewModelScope.launch {
            _ui.update { it.copy(backupsBusy = true, backupsMessage = null) }
            val r = try {
                sh.cleaner.restoreBackup(b)
            } catch (e: Exception) {
                ActionResult(b.pkg, b.pkg, false, e.message ?: "Błąd")
            }
            _ui.update { it.copy(backupsBusy = false, backupsMessage = if (r.ok) "Zainstalowano ${b.pkg}." else r.message) }
            load(true)
        }
    }

    fun deleteBackup(b: ApkBackup) {
        viewModelScope.launch {
            _ui.update { it.copy(backupsBusy = true, backupsMessage = null) }
            try {
                sh.cleaner.delete(listOf(b.dir), 0)
            } catch (e: Exception) {
                _ui.update { it.copy(backupsMessage = e.message ?: "Błąd") }
            }
            reloadBackups()
        }
    }
    fun dismissResults() = _ui.update { it.copy(results = null) }

    fun run(shizukuReady: Boolean) {
        val action = _ui.value.confirm ?: return
        val targets = _ui.value.selectedApps()
        val wipe = _ui.value.wipeLeftovers
        val backup = _ui.value.backupApk
        val p = passes
        viewModelScope.launch {
            _ui.update { it.copy(confirm = null, progress = 0 to maxOf(1, targets.size)) }
            val results = ArrayList<ActionResult>()
            try {
                if (action == AppsAction.TRIM_CACHES) {
                    results += sh.apps.trimCaches()
                } else {
                    targets.forEachIndexed { i, app ->
                        results += when (action) {
                            AppsAction.UNINSTALL -> sh.apps.uninstall(app, wipe, p, backup)
                            else -> sh.apps.clearData(app)
                        }
                        _ui.update { it.copy(progress = (i + 1) to targets.size) }
                    }
                }
            } catch (e: Exception) {
                results += ActionResult("*", "Błąd", false, e.message ?: "Błąd")
            }
            _ui.update { it.copy(progress = null, results = results, selected = emptySet()) }
            load(shizukuReady)
        }
    }
}

// ---------------------------------------------------------------- Resztki

data class LeftoversUi(
    val scanning: Boolean = false,
    val scanned: Boolean = false,
    val items: List<Leftover> = emptyList(),
    val selected: Set<String> = emptySet(),
    val confirm: Boolean = false,
    val progress: Pair<Int, Int>? = null,
    val report: String? = null,
    val error: String? = null,
)

class LeftoversViewModel(app: Application) : AndroidViewModel(app) {

    private val sh = app.shizu()
    private val _ui = MutableStateFlow(LeftoversUi())
    val ui: StateFlow<LeftoversUi> = _ui

    val passes: Int get() = sh.settings.overwritePasses

    fun scan() {
        if (_ui.value.scanning) return
        viewModelScope.launch {
            _ui.update { it.copy(scanning = true, error = null) }
            try {
                val items = sh.cleaner.scanLeftovers()
                _ui.update { it.copy(scanning = false, scanned = true, items = items, selected = emptySet()) }
            } catch (e: Exception) {
                _ui.update { it.copy(scanning = false, error = e.message ?: "Błąd skanowania") }
            }
        }
    }

    fun toggle(path: String) = _ui.update { it.copy(selected = if (path in it.selected) it.selected - path else it.selected + path) }
    fun selectAll() = _ui.update { it.copy(selected = it.items.map { i -> i.path }.toSet()) }
    fun clearSelection() = _ui.update { it.copy(selected = emptySet()) }
    fun askConfirm(show: Boolean) = _ui.update { it.copy(confirm = show) }
    fun dismissReport() = _ui.update { it.copy(report = null) }

    fun deleteSelected() {
        val paths = _ui.value.items.filter { it.path in _ui.value.selected }.map { it.path }
        if (paths.isEmpty()) return
        val p = passes
        viewModelScope.launch {
            _ui.update { it.copy(confirm = false, progress = 0 to paths.size) }
            val report = try {
                CleanerRepository.summarize(
                    sh.cleaner.delete(paths, p) { d, t -> _ui.update { s -> s.copy(progress = d to t) } }
                )
            } catch (e: Exception) {
                "Błąd: ${e.message}"
            }
            _ui.update { it.copy(progress = null, report = report) }
            scan()
        }
    }
}

// ---------------------------------------------------------------- Pliki

enum class FileKind(val label: String) {
    LARGE("Duże pliki"),
    DUPLICATES("Duplikaty"),
    APKS("Pliki APK"),
    EMPTY("Puste foldery"),
}

data class FilesUi(
    val kind: FileKind = FileKind.LARGE,
    val largeMinMb: Int = 50,
    val scanning: Boolean = false,
    val scanned: Set<FileKind> = emptySet(),
    val large: List<FileHit> = emptyList(),
    val dups: List<DupGroup> = emptyList(),
    val apks: List<FileHit> = emptyList(),
    val empty: List<FileHit> = emptyList(),
    val selected: Set<String> = emptySet(),
    val confirm: Boolean = false,
    val progress: Pair<Int, Int>? = null,
    val report: String? = null,
    val error: String? = null,
)

class FilesViewModel(app: Application) : AndroidViewModel(app) {

    private val sh = app.shizu()
    private val _ui = MutableStateFlow(FilesUi())
    val ui: StateFlow<FilesUi> = _ui

    val passes: Int get() = sh.settings.overwritePasses

    fun setKind(kind: FileKind) = _ui.update { it.copy(kind = kind, selected = emptySet(), error = null) }
    fun setLargeMin(mb: Int) = _ui.update { it.copy(largeMinMb = mb, scanned = it.scanned - FileKind.LARGE) }

    fun scan() {
        if (_ui.value.scanning) return
        val kind = _ui.value.kind
        viewModelScope.launch {
            _ui.update { it.copy(scanning = true, error = null, selected = emptySet()) }
            try {
                when (kind) {
                    FileKind.LARGE -> {
                        val r = sh.cleaner.scanLarge(_ui.value.largeMinMb)
                        _ui.update { it.copy(large = r) }
                    }
                    FileKind.DUPLICATES -> {
                        val r = sh.cleaner.scanDuplicates(minKb = 1024)
                        _ui.update { it.copy(dups = r) }
                    }
                    FileKind.APKS -> {
                        val r = sh.cleaner.scanApks()
                        _ui.update { it.copy(apks = r) }
                    }
                    FileKind.EMPTY -> {
                        val r = sh.cleaner.scanEmptyDirs()
                        _ui.update { it.copy(empty = r) }
                    }
                }
                _ui.update { it.copy(scanning = false, scanned = it.scanned + kind) }
            } catch (e: Exception) {
                _ui.update { it.copy(scanning = false, error = e.message ?: "Błąd skanowania") }
            }
        }
    }

    fun toggle(path: String) = _ui.update { it.copy(selected = if (path in it.selected) it.selected - path else it.selected + path) }
    fun clearSelection() = _ui.update { it.copy(selected = emptySet()) }

    fun selectAllCurrent() = _ui.update {
        val paths = when (it.kind) {
            FileKind.LARGE -> it.large.map { f -> f.path }
            FileKind.APKS -> it.apks.map { f -> f.path }
            FileKind.EMPTY -> it.empty.map { f -> f.path }
            FileKind.DUPLICATES -> it.dups.flatMap { g -> g.files.drop(1) }.map { f -> f.path }
        }
        it.copy(selected = paths.toSet())
    }

    /** Duplikaty: zaznacza wszystko poza najstarszą kopią w każdej grupie (grupy są posortowane od najstarszej). */
    fun selectDuplicatesKeepOldest() = selectAllCurrent()

    fun askConfirm(show: Boolean) = _ui.update { it.copy(confirm = show, error = null) }
    fun dismissReport() = _ui.update { it.copy(report = null) }

    /** Blokada: w żadnej grupie duplikatów nie wolno zaznaczyć wszystkich kopii. */
    fun wouldDeleteAllCopies(): Boolean {
        val s = _ui.value
        if (s.kind != FileKind.DUPLICATES) return false
        return s.dups.any { g -> g.files.all { it.path in s.selected } }
    }

    fun deleteSelected() {
        if (wouldDeleteAllCopies()) {
            _ui.update { it.copy(confirm = false, error = "W każdej grupie duplikatów musi zostać co najmniej jedna kopia.") }
            return
        }
        val paths = _ui.value.selected.toList()
        if (paths.isEmpty()) return
        val p = passes
        viewModelScope.launch {
            _ui.update { it.copy(confirm = false, progress = 0 to paths.size) }
            val report = try {
                CleanerRepository.summarize(
                    sh.cleaner.delete(paths, p) { d, t -> _ui.update { s -> s.copy(progress = d to t) } }
                )
            } catch (e: Exception) {
                "Błąd: ${e.message}"
            }
            _ui.update { it.copy(progress = null, report = report, scanned = it.scanned - it.kind) }
            scan()
        }
    }
}

// ---------------------------------------------------------------- Ustawienia

data class SettingsUi(
    val passes: Int = 1,
    val dbBusy: Boolean = false,
    val dbMessage: String? = null,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val sh = app.shizu()
    private val _ui = MutableStateFlow(SettingsUi(passes = sh.settings.overwritePasses))
    val ui: StateFlow<SettingsUi> = _ui

    fun setPasses(p: Int) {
        sh.settings.overwritePasses = p
        _ui.update { it.copy(passes = sh.settings.overwritePasses) }
    }

    fun updateDb() {
        if (_ui.value.dbBusy) return
        viewModelScope.launch {
            _ui.update { it.copy(dbBusy = true, dbMessage = null) }
            val msg = DbUpdater.update(getApplication())
            _ui.update { it.copy(dbBusy = false, dbMessage = msg) }
        }
    }
}
