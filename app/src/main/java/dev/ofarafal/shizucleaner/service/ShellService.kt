package dev.ofarafal.shizucleaner.service

import dev.ofarafal.shizucleaner.IShellService
import kotlin.system.exitProcess

/**
 * Działa w procesie z uprawnieniami shell (uid 2000), uruchomionym przez Shizuku.
 * Polecenia przechodzą przez [CommandPolicy], operacje na plikach przez [FsOps] (ograniczone do pamięci współdzielonej).
 */
class ShellService : IShellService.Stub() {

    private val fs = FsOps()

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: Array<String>): String {
        if (!CommandPolicy.isAllowed(command)) return "EXIT:126\nOdrzucone: polecenie spoza allowlisty"
        return try {
            val process = ProcessBuilder(command.toList()).redirectErrorStream(true).start()
            var output = process.inputStream.bufferedReader().use { it.readText() }
            // diskstats ma też długie sekcje plikowe; zostawiamy tylko tabele rozmiarów (limit transakcji Binder)
            if (command.contentEquals(arrayOf("dumpsys", "diskstats"))) {
                val keep = listOf("Package Names:", "App Sizes:", "App Data Sizes:", "Cache Sizes:")
                output = output.lineSequence().filter { l -> keep.any { l.trimStart().startsWith(it) } }
                    .joinToString("\n")
            }
            "EXIT:${process.waitFor()}\n${output.take(200_000)}"
        } catch (e: Exception) {
            "EXIT:127\n${e.message}"
        }
    }

    override fun scan(kind: String, args: Array<String>): String = try {
        when (kind) {
            "appdirs" -> fs.scanAppDirs(args.toList())
            "large" -> fs.scanLarge(args.getOrNull(0)?.toLongOrNull() ?: (50L shl 20), args.getOrNull(1)?.toIntOrNull() ?: 300)
            "dups" -> fs.scanDuplicates(args.getOrNull(0)?.toLongOrNull() ?: (1L shl 20), args.getOrNull(1)?.toIntOrNull() ?: 200)
            "apks" -> fs.scanApks(500)
            "emptydirs" -> fs.scanEmptyDirs(500)
            else -> "E\tNieznany typ skanu: $kind"
        }
    } catch (e: Exception) {
        "E\t${e.message}"
    }

    override fun delete(paths: Array<String>, passes: Int): String = try {
        fs.delete(paths.toList(), passes)
    } catch (e: Exception) {
        "E\t${e.message}"
    }
}
