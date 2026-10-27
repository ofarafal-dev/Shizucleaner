package dev.ofarafal.shizucleaner.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import dev.ofarafal.shizucleaner.shizuku.ShizukuClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UserApp(
    val pkg: String,
    val label: String,
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
) {
    val total: Long get() = appBytes + dataBytes + cacheBytes
}

/** Aplikacje zainstalowane przez użytkownika: rozmiary, czyszczenie danych, odinstalowanie z resztkami. */
class AppsRepository(
    private val ctx: Context,
    private val shell: ShizukuClient,
    private val cleaner: CleanerRepository,
) {

    /** [includeSystem]: dołącza aplikacje systemowe (bez krytycznych i chronionych) — dla zapory. */
    suspend fun load(readSizes: Boolean, includeSystem: Boolean = false): List<UserApp> {
        val apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val protectedPkgs = protectedPackages(ctx)
            val infos = if (Build.VERSION.SDK_INT >= 33) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }
            infos.filter {
                ((it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || includeSystem) &&
                    it.packageName !in protectedPkgs && !CriticalPackages.isCritical(it.packageName)
            }
                .map { it.packageName to runCatching { pm.getApplicationLabel(it).toString() }.getOrDefault(it.packageName) }
        }
        val sizes = if (readSizes) {
            runCatching { DiskStats.parse(shell.exec("dumpsys", "diskstats").output) }.getOrDefault(emptyMap())
        } else {
            emptyMap()
        }
        return apps.map { (pkg, label) ->
            val s = sizes[pkg]
            UserApp(pkg, label, s?.app ?: 0L, s?.data ?: 0L, s?.cache ?: 0L)
        }
    }

    /** Czyści wszystkie cache (jak „Wyczyść pamięć podręczną” w ustawieniach). */
    suspend fun trimCaches(): ActionResult {
        val r = shell.exec("pm", "trim-caches", "1000G")
        return ActionResult("*", "Cache wszystkich aplikacji", r.ok, if (r.ok) "OK" else r.output.trim().ifEmpty { "kod ${r.exitCode}" })
    }

    suspend fun clearData(app: UserApp): ActionResult {
        val r = shell.exec("pm", "clear", "--user", "0", app.pkg)
        val ok = r.ok && r.output.contains("Success", ignoreCase = true)
        return ActionResult(app.pkg, app.label, ok, if (ok) "OK" else r.output.trim().ifEmpty { "kod ${r.exitCode}" })
    }

    /** Odinstalowuje (razem z danymi) i opcjonalnie nadpisuje oraz kasuje resztki w pamięci współdzielonej. */
    suspend fun uninstall(app: UserApp, wipeLeftovers: Boolean, passes: Int, backupApk: Boolean): ActionResult {
        if (backupApk) {
            try {
                cleaner.backupApk(app.pkg)
            } catch (e: Exception) {
                return ActionResult(app.pkg, app.label, false, "Kopia APK nieudana, aplikacji nie usunięto: ${e.message}")
            }
        }
        val r = shell.exec("pm", "uninstall", "--user", "0", app.pkg)
        if (!(r.ok && r.output.contains("Success", ignoreCase = true))) {
            return ActionResult(app.pkg, app.label, false, r.output.trim().ifEmpty { "kod ${r.exitCode}" })
        }
        if (!wipeLeftovers) return ActionResult(app.pkg, app.label, true, "OK")

        val results = cleaner.delete(cleaner.pathsFor(app.pkg), passes)
        val bad = results.filter { !it.ok }
        return if (bad.isEmpty()) {
            ActionResult(app.pkg, app.label, true, "OK, resztki usunięte")
        } else {
            ActionResult(app.pkg, app.label, false, "Odinstalowano, ale resztki: ${bad.first().message}")
        }
    }
}
