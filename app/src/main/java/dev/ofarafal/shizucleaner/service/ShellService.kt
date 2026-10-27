package dev.ofarafal.shizucleaner.service

import dev.ofarafal.shizucleaner.IShellService
import kotlin.system.exitProcess

/**
 * Działa w procesie z uprawnieniami shell (uid 2000), uruchomionym przez Shizuku.
 * Wykonuje tylko wąski zestaw poleceń `pm` dla użytkownika 0 — nic innego nie przejdzie,
 * nawet gdyby ktoś zdołał zbindować ten serwis.
 */
class ShellService : IShellService.Stub() {

    private val pkgRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")

    // podkomenda pm -> dokładna lista flag przed nazwą pakietu
    private val allowed: Map<String, List<String>> = mapOf(
        "uninstall" to listOf("-k", "--user", "0"),
        "disable-user" to listOf("--user", "0"),
        "enable" to listOf("--user", "0"),
        "install-existing" to listOf("--user", "0"),
    )

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: Array<String>): String {
        if (!isAllowed(command)) return "EXIT:126\nOdrzucone: polecenie spoza allowlisty"
        return try {
            val process = ProcessBuilder(command.toList()).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            "EXIT:${process.waitFor()}\n$output"
        } catch (e: Exception) {
            "EXIT:127\n${e.message}"
        }
    }

    private fun isAllowed(c: Array<String>): Boolean {
        if (c.size < 4 || c[0] != "pm") return false
        val flags = allowed[c[1]] ?: return false
        if (c.size != 2 + flags.size + 1) return false
        if (c.slice(2 until 2 + flags.size) != flags) return false
        return pkgRegex.matches(c.last())
    }
}
