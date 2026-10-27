package dev.ofarafal.shizucleaner.service

import dev.ofarafal.shizucleaner.data.CriticalPackages
import dev.ofarafal.shizucleaner.data.RuntimePermissions

/**
 * Allowlista poleceń wykonywanych jako shell. Każdy wzorzec to dokładna lista tokenów;
 * `<pkg>`, `<size>`, `<perm>` i `<apk>` to jedyne miejsca na dane z zewnątrz i są walidowane.
 * Polecenia niszczące są dodatkowo odrzucane dla pakietów krytycznych (druga linia obrony po UI).
 */
object CommandPolicy {

    private class Pattern(val tokens: List<String>, val destructive: Boolean)

    private val pkgRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")
    private val sizeRegex = Regex("^[0-9]{1,6}[KMG]$")
    private val permRegex = Regex("^android\\.permission\\.[A-Z0-9_]+$")
    private val apkRegex =
        Regex("^/storage/emulated/0/ShizuCleaner/backups/[a-zA-Z0-9_.]+/[A-Za-z0-9_.\\-]+\\.apk$")

    private val patterns = listOf(
        Pattern(listOf("pm", "uninstall", "-k", "--user", "0", "<pkg>"), destructive = true),
        Pattern(listOf("pm", "uninstall", "--user", "0", "<pkg>"), destructive = true),
        Pattern(listOf("pm", "disable-user", "--user", "0", "<pkg>"), destructive = true),
        Pattern(listOf("pm", "clear", "--user", "0", "<pkg>"), destructive = true),
        Pattern(listOf("pm", "enable", "--user", "0", "<pkg>"), destructive = false),
        Pattern(listOf("pm", "install-existing", "--user", "0", "<pkg>"), destructive = false),
        Pattern(listOf("pm", "trim-caches", "<size>"), destructive = false),
        Pattern(listOf("dumpsys", "diskstats"), destructive = false),
        // zapora per aplikacja (bez VPN)
        Pattern(listOf("cmd", "connectivity", "set-chain3-enabled", "true"), destructive = false),
        Pattern(listOf("cmd", "connectivity", "set-package-networking-enabled", "false", "<pkg>"), destructive = true),
        Pattern(listOf("cmd", "connectivity", "set-package-networking-enabled", "true", "<pkg>"), destructive = false),
        // cofanie uprawnień runtime
        Pattern(listOf("pm", "revoke", "--user", "0", "<pkg>", "<perm>"), destructive = true),
        // przywracanie z kopii APK wykonanej przez tę aplikację
        Pattern(listOf("pm", "install", "-r", "<apk>"), destructive = false),
    )

    fun isAllowed(c: Array<String>): Boolean = patterns.any { matches(it, c) }

    private fun matches(p: Pattern, c: Array<String>): Boolean {
        if (p.tokens.size != c.size) return false
        for (i in c.indices) {
            val ok = when (p.tokens[i]) {
                "<pkg>" -> pkgRegex.matches(c[i]) && !(p.destructive && CriticalPackages.isCritical(c[i]))
                "<size>" -> sizeRegex.matches(c[i])
                "<perm>" -> permRegex.matches(c[i]) && c[i] in RuntimePermissions.all
                "<apk>" -> apkRegex.matches(c[i]) && !c[i].contains("..")
                else -> p.tokens[i] == c[i]
            }
            if (!ok) return false
        }
        return true
    }
}
