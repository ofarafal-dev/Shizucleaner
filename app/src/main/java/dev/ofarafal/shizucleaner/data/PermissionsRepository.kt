package dev.ofarafal.shizucleaner.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dev.ofarafal.shizucleaner.shizuku.ShizukuClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PermApp(
    val pkg: String,
    val label: String,
    val isSystem: Boolean,
    /** przyznane uprawnienia z wybranej kategorii */
    val granted: List<String>,
)

/** Kto ma jakie uprawnienie runtime (czytane bez Shizuku) i cofanie ich przez `pm revoke`. */
class PermissionsRepository(private val ctx: Context, private val shell: ShizukuClient) {

    suspend fun appsWith(category: PermCategory, includeSystem: Boolean): List<PermApp> =
        withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val skip = protectedPackages(ctx)
            val wanted = category.perms.toSet()
            val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= 33) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
            val out = ArrayList<PermApp>()
            for (pi in packages) {
                val ai = pi.applicationInfo ?: continue
                val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystem && !includeSystem) continue
                if (pi.packageName in skip || CriticalPackages.isCritical(pi.packageName)) continue
                val requested = pi.requestedPermissions ?: continue
                val flags = pi.requestedPermissionsFlags ?: continue
                val granted = requested.indices
                    .filter { i -> requested[i] in wanted && (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0 }
                    .map { requested[it] }
                if (granted.isEmpty()) continue
                out += PermApp(
                    pkg = pi.packageName,
                    label = runCatching { pm.getApplicationLabel(ai).toString() }.getOrDefault(pi.packageName),
                    isSystem = isSystem,
                    granted = granted,
                )
            }
            out.sortedBy { it.label.lowercase() }
        }

    suspend fun revoke(app: PermApp): ActionResult {
        val failed = ArrayList<String>()
        for (perm in app.granted) {
            try {
                val r = shell.exec("pm", "revoke", "--user", "0", app.pkg, perm)
                if (!r.ok) failed += "${perm.substringAfterLast('.')}: ${r.output.trim().lineSequence().firstOrNull().orEmpty()}"
            } catch (e: Exception) {
                failed += e.message ?: "Błąd"
            }
        }
        return if (failed.isEmpty()) ActionResult(app.pkg, app.label, true, "OK")
        else ActionResult(app.pkg, app.label, false, failed.joinToString("; "))
    }
}
