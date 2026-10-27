package dev.ofarafal.shizucleaner.service

import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.random.Random

/**
 * Operacje na plikach wykonywane w procesie shell (Shizuku), który widzi Android/data i Android/obb.
 * Wszystko jest ograniczone do [root]; ścieżki przechodzą przez [resolve], która odrzuca root,
 * wyjścia poza root (.., dowiązania), główne foldery systemowe i dane własnej aplikacji.
 * Czysty java.io, żeby dało się to testować na JVM.
 */
class FsOps(
    rootPath: String = DEFAULT_ROOT,
    private val ownPackage: String = OWN_PACKAGE,
    private val appRootPath: String = "/data/app",
) {
    val root: File = File(rootPath).canonicalFile
    private val rootPrefix = root.path + File.separator
    private val backupsDir = File(root, "ShizuCleaner/backups")
    private val pkgRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")

    companion object {
        const val DEFAULT_ROOT = "/storage/emulated/0"
        const val OWN_PACKAGE = "dev.ofarafal.shizucleaner"
        private const val MAX_OUT = 200_000

        val PROTECTED_TOP = setOf(
            "Android", "DCIM", "Pictures", "Download", "Documents", "Movies", "Music",
            "Alarms", "Notifications", "Ringtones", "Podcasts", "Audiobooks",
        )
    }

    // ---------- ochrona ścieżek ----------

    fun resolve(path: String): File? {
        if (!path.startsWith("/") || path.indexOf('\u0000') >= 0) return null
        val f = try {
            File(path).canonicalFile
        } catch (e: Exception) {
            return null
        }
        if (!f.path.startsWith(rootPrefix)) return null
        val parts = f.path.removePrefix(rootPrefix).split(File.separatorChar)
        val top = parts[0]
        if (top in PROTECTED_TOP && parts.size < (if (top == "Android") 3 else 2)) return null
        if (top == "Android" && parts.size >= 3 && parts[2] == ownPackage) return null
        return f
    }

    private fun isLink(f: File) = Files.isSymbolicLink(f.toPath())
    private fun badName(f: File) = f.name.any { it == '\t' || it == '\n' || it == '\r' }

    // ---------- skany ----------

    private fun walk(dir: File, onFile: (File) -> Unit) {
        val children = dir.listFiles() ?: return
        for (c in children) {
            if (isLink(c) || badName(c)) continue
            if (c.isDirectory) {
                if (dir == root && c.name == "Android") continue
                if (c == backupsDir) continue // kopie APK nie trafiają do skanów (żeby nie skasować ich przypadkiem)
                walk(c, onFile)
            } else if (c.isFile) {
                onFile(c)
            }
        }
    }

    fun dirSize(f: File): Long {
        if (isLink(f)) return 0
        if (f.isFile) return f.length()
        var s = 0L
        f.listFiles()?.forEach { s += dirSize(it) }
        return s
    }

    /** Foldery aplikacji w Android/data|obb|media oraz wskazane foldery najwyższego poziomu ("top"). */
    fun scanAppDirs(extraTop: List<String>): String {
        val sb = StringBuilder()
        for (sub in listOf("data", "obb", "media")) {
            File(root, "Android/$sub").listFiles()?.forEach { d ->
                if (d.isDirectory && !isLink(d) && !badName(d)) {
                    sb.append("A\t$sub\t${d.name}\t${dirSize(d)}\t${d.lastModified()}\n")
                }
            }
        }
        for (name in extraTop) {
            if (name.isEmpty() || name == "." || name == ".." || name.contains('/') || name.contains('\u0000')) continue
            val d = File(root, name)
            if (d.isDirectory && !isLink(d) && !badName(d)) {
                sb.append("A\ttop\t$name\t${dirSize(d)}\t${d.lastModified()}\n")
            }
        }
        return sb.toString()
    }

    fun scanLarge(minBytes: Long, limit: Int): String {
        val hits = ArrayList<File>()
        walk(root) { if (it.length() >= minBytes) hits += it }
        return encodeHits(hits.sortedByDescending { it.length() }.take(limit))
    }

    fun scanApks(limit: Int): String {
        val hits = ArrayList<File>()
        walk(root) { if (it.name.endsWith(".apk", ignoreCase = true)) hits += it }
        return encodeHits(hits.sortedByDescending { it.length() }.take(limit))
    }

    fun scanEmptyDirs(limit: Int): String {
        val out = ArrayList<File>()
        fun visit(d: File) {
            val children = d.listFiles() ?: return
            for (c in children) {
                if (out.size >= limit) return
                if (isLink(c) || !c.isDirectory || badName(c)) continue
                if (d == root && c.name == "Android") continue
                if (isEmptyTree(c) && resolve(c.path) != null) out += c else visit(c)
            }
        }
        visit(root)
        return encodeHits(out)
    }

    private fun isEmptyTree(d: File): Boolean {
        val children = d.listFiles() ?: return false
        return children.all { !isLink(it) && it.isDirectory && isEmptyTree(it) }
    }

    fun scanDuplicates(minBytes: Long, maxGroups: Int): String {
        val bySize = HashMap<Long, MutableList<File>>()
        walk(root) { f ->
            val len = f.length()
            if (len >= minBytes) bySize.getOrPut(len) { ArrayList() }.add(f)
        }
        val groups = ArrayList<Pair<Long, List<File>>>()
        for ((size, files) in bySize) {
            if (files.size < 2) continue
            for ((head, sameHead) in files.groupBy { hash(it, 64 * 1024L) }) {
                if (head == null || sameHead.size < 2) continue
                for ((full, same) in sameHead.groupBy { hash(it, Long.MAX_VALUE) }) {
                    if (full != null && same.size >= 2) groups += size to same
                }
            }
        }
        groups.sortByDescending { it.first * (it.second.size - 1) }
        val sb = StringBuilder()
        for ((gi, g) in groups.take(maxGroups).withIndex()) {
            if (sb.length > MAX_OUT) break
            for (f in g.second.sortedBy { it.lastModified() }) {
                sb.append("D\t$gi\t${g.first}\t${f.lastModified()}\t${f.path}\n")
            }
        }
        return sb.toString()
    }

    private fun hash(f: File, limit: Long): String? = try {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            var left = limit
            while (left > 0) {
                val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                if (n < 0) break
                md.update(buf, 0, n)
                left -= n
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        null
    }

    private fun encodeHits(files: List<File>): String {
        val sb = StringBuilder()
        for (f in files) {
            if (sb.length > MAX_OUT) break
            sb.append("H\t${if (f.isFile) f.length() else 0}\t${f.lastModified()}\t${f.path}\n")
        }
        return sb.toString()
    }

    // ---------- kopie APK ----------

    /**
     * Kopiuje pliki APK aplikacji (base + splity) z [appRootPath] do ShizuCleaner/backups/[pkg]/.
     * Źródło musi leżeć pod /data/app i kończyć się na .apk; nazwa pakietu przechodzi przez regex.
     * Poprzednia kopia tego pakietu jest nadpisywana. Zwraca "OK\tfolder\tliczba\tbajty" albo "E\tpowód".
     */
    fun backupApks(srcs: List<String>, pkg: String): String {
        if (!pkgRegex.matches(pkg)) return "E\tNieprawidłowa nazwa pakietu"
        if (srcs.isEmpty() || srcs.size > 60) return "E\tNieprawidłowa liczba plików"
        val prefix = File(appRootPath).canonicalPath + File.separator
        val files = ArrayList<File>()
        for (s in srcs) {
            val f = try {
                File(s).canonicalFile
            } catch (e: Exception) {
                return "E\tBłędna ścieżka źródłowa"
            }
            if (!f.path.startsWith(prefix) || !f.name.endsWith(".apk", ignoreCase = true) || !f.isFile) {
                return "E\tNiedozwolone źródło: ${f.name}"
            }
            files += f
        }
        val dest = File(backupsDir, pkg)
        dest.deleteRecursively()
        if (!dest.mkdirs()) return "E\tNie można utworzyć folderu kopii"
        var bytes = 0L
        for (f in files) {
            f.copyTo(File(dest, f.name), overwrite = true)
            bytes += f.length()
        }
        return "OK\t${dest.path}\t${files.size}\t$bytes"
    }

    /** Pliki APK w folderze kopii (jeden poziom podfolderów = pakiet). */
    fun scanBackups(): String {
        val out = ArrayList<File>()
        backupsDir.listFiles()?.forEach { d ->
            if (d.isDirectory && !isLink(d)) {
                d.listFiles()?.forEach { f ->
                    if (f.isFile && !badName(f) && f.name.endsWith(".apk", ignoreCase = true)) out += f
                }
            }
        }
        return encodeHits(out)
    }

    // ---------- kasowanie ----------

    private class Stats {
        var files = 0L
        var bytes = 0L
        var notOverwritten = 0L
        val errors = ArrayList<String>()
    }

    /** Wynik: "OK\tścieżka\tpliki\tbajty\tnienadpisane" albo "ERR\tścieżka\tpowód" na ścieżkę. */
    fun delete(paths: List<String>, passes: Int): String {
        val p = passes.coerceIn(0, 3)
        val sb = StringBuilder()
        for (path in paths) {
            val f = resolve(path)
            if (f == null) {
                sb.append("ERR\t$path\tŚcieżka niedozwolona\n")
                continue
            }
            if (!f.exists() && !isLink(f)) {
                sb.append("OK\t$path\t0\t0\t0\n")
                continue
            }
            val st = Stats()
            remove(f, p, st)
            if (st.errors.isEmpty()) {
                sb.append("OK\t$path\t${st.files}\t${st.bytes}\t${st.notOverwritten}\n")
            } else {
                sb.append("ERR\t$path\t${st.errors.first()}\n")
            }
        }
        return sb.toString()
    }

    private fun remove(f: File, passes: Int, st: Stats) {
        if (isLink(f)) {
            if (!f.delete()) st.errors += "nie można usunąć dowiązania ${f.name}"
            return
        }
        if (f.isDirectory) {
            f.listFiles()?.forEach { remove(it, passes, st) }
            if (!f.delete()) st.errors += "nie można usunąć folderu ${f.name}"
            return
        }
        val len = f.length()
        if (passes > 0 && len > 0) {
            val ok = try {
                overwrite(f, len, passes)
                true
            } catch (e: Exception) {
                false
            }
            if (!ok) st.notOverwritten++
        }
        if (f.delete()) {
            st.files++
            st.bytes += len
        } else {
            st.errors += "nie można usunąć ${f.name}"
        }
    }

    /** Nadpisuje zawartość losowymi bajtami [passes] razy, potem ucina do zera. */
    private fun overwrite(f: File, len: Long, passes: Int) {
        RandomAccessFile(f, "rw").use { raf ->
            val buf = ByteArray(64 * 1024)
            repeat(passes) {
                raf.seek(0)
                var left = len
                while (left > 0) {
                    Random.nextBytes(buf)
                    val n = minOf(left, buf.size.toLong()).toInt()
                    raf.write(buf, 0, n)
                    left -= n
                }
                raf.fd.sync()
            }
            raf.setLength(0)
            raf.fd.sync()
        }
    }
}
