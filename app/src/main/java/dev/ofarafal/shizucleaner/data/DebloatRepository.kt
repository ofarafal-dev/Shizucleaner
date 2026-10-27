package dev.ofarafal.shizucleaner.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import dev.ofarafal.shizucleaner.shizuku.ShizukuClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val pkg: String,
    val label: String,
    val isSystem: Boolean,
    val installed: Boolean, // zainstalowana dla użytkownika 0 (false = usunięta, ale APK nadal w systemie)
    val enabled: Boolean,
    val known: KnownPackage?,
    val risk: Risk,
)

enum class PkgAction { UNINSTALL, DISABLE, RESTORE }

data class ActionResult(val pkg: String, val label: String, val ok: Boolean, val message: String)

class DebloatRepository(private val ctx: Context, private val shell: ShizukuClient) {

    private val pkgRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")

    /** Stan systemu jest jedynym źródłem prawdy — lista "do przywrócenia" nie wymaga własnego logu. */
    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val protectedPkgs = protectedPackages()

        val raw: List<ApplicationInfo> = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        }

        raw.map { ai ->
            val pkg = ai.packageName
            val known = PackageDb.lookup(pkg)
            val critical = PackageDb.isCritical(pkg) || pkg in protectedPkgs
            val risk = when {
                critical -> Risk.CRITICAL
                known != null -> known.risk
                else -> Risk.UNKNOWN
            }
            AppEntry(
                pkg = pkg,
                label = runCatching { pm.getApplicationLabel(ai).toString() }.getOrDefault(pkg),
                isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                installed = (ai.flags and ApplicationInfo.FLAG_INSTALLED) != 0,
                enabled = ai.enabled,
                known = known,
                risk = risk,
            )
        }
    }

    suspend fun apply(
        action: PkgAction,
        targets: List<AppEntry>,
        onProgress: (Int) -> Unit,
    ): List<ActionResult> {
        val results = ArrayList<ActionResult>(targets.size)
        targets.forEachIndexed { i, app ->
            results += applyOne(action, app)
            onProgress(i + 1)
        }
        return results
    }

    private suspend fun applyOne(action: PkgAction, app: AppEntry): ActionResult {
        if (!pkgRegex.matches(app.pkg)) return fail(app, "Nieprawidłowa nazwa pakietu")
        if (action != PkgAction.RESTORE && app.risk == Risk.CRITICAL) {
            return fail(app, "Pakiet krytyczny — zablokowane")
        }
        return try {
            when (action) {
                PkgAction.UNINSTALL ->
                    runPm(app, "Success", "pm", "uninstall", "-k", "--user", "0", app.pkg)

                PkgAction.DISABLE ->
                    runPm(app, "disabled", "pm", "disable-user", "--user", "0", app.pkg)

                PkgAction.RESTORE -> {
                    if (!app.installed) {
                        val r = shell.exec("pm", "install-existing", "--user", "0", app.pkg)
                        if (!r.output.contains("installed for user", ignoreCase = true)) {
                            return fail(app, r.output.trim().ifEmpty { "kod ${r.exitCode}" })
                        }
                    }
                    runPm(app, "enabled", "pm", "enable", "--user", "0", app.pkg)
                }
            }
        } catch (e: Exception) {
            fail(app, e.message ?: "Błąd")
        }
    }

    private suspend fun runPm(app: AppEntry, needle: String, vararg cmd: String): ActionResult {
        val r = shell.exec(*cmd)
        val ok = r.ok && r.output.contains(needle, ignoreCase = true)
        return ActionResult(
            pkg = app.pkg,
            label = app.label,
            ok = ok,
            message = if (ok) "OK" else r.output.trim().ifEmpty { "kod ${r.exitCode}" },
        )
    }

    private fun fail(app: AppEntry, msg: String) = ActionResult(app.pkg, app.label, false, msg)

    /** Aplikacje, których usunięcie odcięłoby użytkownika od telefonu: ta aplikacja, launchery, klawiatura, dialer, SMS. */
    private fun protectedPackages(): Set<String> {
        val pm = ctx.packageManager
        val set = mutableSetOf(ctx.packageName)

        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        runCatching {
            pm.queryIntentActivities(home, 0).forEach { set += it.activityInfo.packageName }
        }
        runCatching {
            pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName?.let { set += it }
        }
        runCatching {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore('/')?.takeIf { it.isNotEmpty() }?.let { set += it }
        }
        runCatching {
            ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { set += it }
        }
        runCatching {
            Telephony.Sms.getDefaultSmsPackage(ctx)?.let { set += it }
        }
        return set
    }
}
