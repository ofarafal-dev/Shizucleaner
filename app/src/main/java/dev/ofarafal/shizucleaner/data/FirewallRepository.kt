package dev.ofarafal.shizucleaner.data

import android.content.Context
import dev.ofarafal.shizucleaner.shizuku.ShizukuClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Zapora per aplikacja bez VPN: `cmd connectivity set-package-networking-enabled` (chain3).
 * Android czyści te reguły po restarcie, więc lista zablokowanych pakietów jest zapamiętana
 * i nakładana ponownie za każdym razem, gdy Shizuku staje się gotowe.
 */
class FirewallRepository(ctx: Context, private val shell: ShizukuClient) {

    private val prefs = ctx.getSharedPreferences("firewall", Context.MODE_PRIVATE)
    private val lock = Mutex()

    fun blocked(): Set<String> = HashSet(prefs.getStringSet(KEY, emptySet()) ?: emptySet())

    private fun save(set: Set<String>) = prefs.edit().putStringSet(KEY, HashSet(set)).apply()

    private suspend fun enableChain(): String? {
        val r = shell.exec("cmd", "connectivity", "set-chain3-enabled", "true")
        return if (r.ok) null else r.output.trim().ifEmpty { "System nie obsługuje tej zapory (cmd connectivity)." }
    }

    suspend fun setBlocked(pkg: String, label: String, block: Boolean): ActionResult = lock.withLock {
        try {
            if (block) {
                enableChain()?.let { return ActionResult(pkg, label, false, it) }
            }
            val r = shell.exec("cmd", "connectivity", "set-package-networking-enabled", block.toString(), pkg)
            if (!r.ok) {
                return ActionResult(pkg, label, false, r.output.trim().ifEmpty { "kod ${r.exitCode}" })
            }
            save(if (block) blocked() + pkg else blocked() - pkg)
            ActionResult(pkg, label, true, "OK")
        } catch (e: Exception) {
            ActionResult(pkg, label, false, e.message ?: "Błąd")
        }
    }

    /** Nakłada zapamiętane blokady (po restarcie telefonu lub Shizuku). Zwraca liczbę poprawnie nałożonych. */
    suspend fun reapplyAll(): Int = lock.withLock {
        val set = blocked()
        if (set.isEmpty()) return 0
        if (enableChain() != null) return 0
        var ok = 0
        for (pkg in set) {
            val r = shell.exec("cmd", "connectivity", "set-package-networking-enabled", "false", pkg)
            if (r.ok) ok++
        }
        ok
    }

    private companion object {
        const val KEY = "blocked"
    }
}
