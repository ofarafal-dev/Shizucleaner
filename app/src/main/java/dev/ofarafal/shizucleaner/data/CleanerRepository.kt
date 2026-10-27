package dev.ofarafal.shizucleaner.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.ofarafal.shizucleaner.service.FsOps
import dev.ofarafal.shizucleaner.shizuku.ShizukuClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LeftoverKind(val label: String) {
    DATA("Android/data"),
    OBB("Android/obb"),
    MEDIA("Android/media"),
    FOLDER("Folder w pamięci"),
}

data class Leftover(val pkg: String, val kind: LeftoverKind, val path: String, val size: Long, val mtime: Long)

data class ApkBackup(val pkg: String, val dir: String, val files: List<FileHit>) {
    val size: Long get() = files.sumOf { it.size }
    val mtime: Long get() = files.maxOfOrNull { it.mtime } ?: 0L
}

/** Skany plików i kasowanie przez serwis Shizuku. */
class CleanerRepository(private val ctx: Context, private val shell: ShizukuClient) {

    private val pkgRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")

    /** Wszystkie pakiety znane systemowi, także te usunięte dla użytkownika (ich dane są celowo zachowane). */
    suspend fun knownPackages(): Set<String> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val infos = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        }
        infos.map { it.packageName }.toSet() + ctx.packageName
    }

    suspend fun scanLeftovers(): List<Leftover> {
        val known = knownPackages()
        val folders = PackageDb.leftoverFolders().filterKeys { it !in known }
        val owner = HashMap<String, String>()
        folders.forEach { (pkg, names) -> names.forEach { owner.putIfAbsent(it, pkg) } }

        val raw = shell.scan("appdirs", *owner.keys.toTypedArray())
        Wire.checkError(raw)

        return Wire.appDirs(raw).mapNotNull { d ->
            when (d.root) {
                "data", "obb", "media" -> {
                    if (!pkgRegex.matches(d.name) || d.name in known) {
                        null
                    } else {
                        val kind = when (d.root) {
                            "data" -> LeftoverKind.DATA
                            "obb" -> LeftoverKind.OBB
                            else -> LeftoverKind.MEDIA
                        }
                        Leftover(d.name, kind, "$ROOT/Android/${d.root}/${d.name}", d.size, d.mtime)
                    }
                }
                "top" -> owner[d.name]?.let { Leftover(it, LeftoverKind.FOLDER, "$ROOT/${d.name}", d.size, d.mtime) }
                else -> null
            }
        }.sortedByDescending { it.size }
    }

    /** Wszystkie miejsca, w których aplikacja zostawia dane w pamięci współdzielonej. */
    fun pathsFor(pkg: String): List<String> =
        listOf("$ROOT/Android/data/$pkg", "$ROOT/Android/obb/$pkg", "$ROOT/Android/media/$pkg") +
            (PackageDb.leftoverFolders()[pkg] ?: emptyList()).map { "$ROOT/$it" }

    suspend fun scanLarge(minMb: Int): List<FileHit> {
        val raw = shell.scan("large", (minMb.toLong() shl 20).toString(), "300")
        Wire.checkError(raw)
        return Wire.hits(raw)
    }

    suspend fun scanDuplicates(minKb: Int): List<DupGroup> {
        val raw = shell.scan("dups", (minKb.toLong() shl 10).toString(), "200")
        Wire.checkError(raw)
        return Wire.dups(raw)
    }

    suspend fun scanApks(): List<FileHit> {
        val raw = shell.scan("apks")
        Wire.checkError(raw)
        return Wire.hits(raw)
    }

    suspend fun scanEmptyDirs(): List<FileHit> {
        val raw = shell.scan("emptydirs")
        Wire.checkError(raw)
        return Wire.hits(raw)
    }

    // ---------- kopie APK (tylko pliki APK, bez danych aplikacji) ----------

    /** Kopiuje base.apk i splity aplikacji do ShizuCleaner/backups/<pakiet>/. Rzuca wyjątek przy błędzie. */
    @Suppress("DEPRECATION")
    suspend fun backupApk(pkg: String): String {
        val ai = ctx.packageManager.getApplicationInfo(pkg, 0)
        val srcs = listOf(ai.sourceDir) + (ai.splitSourceDirs?.toList() ?: emptyList())
        val raw = shell.backup(srcs, pkg)
        Wire.checkError(raw)
        return raw
    }

    suspend fun listBackups(): List<ApkBackup> {
        val raw = shell.scan("backups")
        Wire.checkError(raw)
        return Wire.hits(raw)
            .groupBy { it.path.substringBeforeLast('/') }
            .map { (dir, files) -> ApkBackup(dir.substringAfterLast('/'), dir, files) }
            .sortedByDescending { it.mtime }
    }

    /** Instalacja z kopii. Obsługuje tylko pojedynczy APK; kopie ze splitami trzeba zainstalować ręcznie. */
    suspend fun restoreBackup(b: ApkBackup): ActionResult {
        if (b.files.size != 1) {
            return ActionResult(
                b.pkg, b.pkg, false,
                "Kopia ma ${b.files.size} plików (split APK). Zainstaluj ręcznie z folderu ShizuCleaner/backups/${b.pkg}.",
            )
        }
        val r = shell.exec("pm", "install", "-r", b.files.single().path)
        val ok = r.ok && r.output.contains("Success", ignoreCase = true)
        return ActionResult(b.pkg, b.pkg, ok, if (ok) "OK" else r.output.trim().ifEmpty { "kod ${r.exitCode}" })
    }

    /** Kasuje porcjami po 25 ścieżek, żeby pokazać postęp i nie blokować Binder na wiele minut. */
    suspend fun delete(
        paths: List<String>,
        passes: Int,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<DeleteResult> {
        val out = ArrayList<DeleteResult>(paths.size)
        var done = 0
        for (chunk in paths.chunked(25)) {
            val raw = shell.delete(chunk, passes)
            Wire.checkError(raw)
            out += Wire.deleteResults(raw)
            done += chunk.size
            onProgress(done, paths.size)
        }
        return out
    }

    companion object {
        const val ROOT = FsOps.DEFAULT_ROOT

        fun summarize(results: List<DeleteResult>): String {
            val ok = results.filter { it.ok }
            val bad = results.filter { !it.ok }
            val sb = StringBuilder()
            sb.append("Usunięto ${ok.sumOf { it.files }} plików, zwolniono ${ok.sumOf { it.bytes }.let(::fmt)}.")
            val skipped = ok.sumOf { it.notOverwritten }
            if (skipped > 0) sb.append("\n$skipped plików skasowano bez nadpisania (brak zapisu).")
            if (bad.isNotEmpty()) {
                sb.append("\n\nBłędy (${bad.size}):")
                bad.take(10).forEach { sb.append("\n• ${it.path.substringAfterLast('/')}: ${it.message}") }
            }
            return sb.toString()
        }

        private fun fmt(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val units = arrayOf("KB", "MB", "GB", "TB")
            var v = bytes.toDouble()
            var i = -1
            do {
                v /= 1024
                i++
            } while (v >= 1024 && i < units.lastIndex)
            return String.format(java.util.Locale.getDefault(), "%.1f %s", v, units[i])
        }
    }
}
