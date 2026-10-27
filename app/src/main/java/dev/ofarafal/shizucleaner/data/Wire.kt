package dev.ofarafal.shizucleaner.data

data class FileHit(val path: String, val size: Long, val mtime: Long)

data class DupGroup(val size: Long, val files: List<FileHit>) {
    val wasted: Long get() = size * (files.size - 1)
}

data class AppDir(val root: String, val name: String, val size: Long, val mtime: Long)

data class DeleteResult(
    val path: String,
    val ok: Boolean,
    val files: Long,
    val bytes: Long,
    val notOverwritten: Long,
    val message: String,
)

/** Dekodowanie odpowiedzi serwisu (linie rozdzielone tabulatorami). Czysty Kotlin, testowany na JVM. */
object Wire {

    fun checkError(raw: String) {
        if (raw.startsWith("E\t")) throw IllegalStateException(raw.substring(2).trim())
    }

    fun hits(raw: String): List<FileHit> = raw.lineSequence().mapNotNull { line ->
        if (!line.startsWith("H\t")) return@mapNotNull null
        val p = line.split('\t', limit = 4)
        if (p.size < 4) return@mapNotNull null
        val size = p[1].toLongOrNull() ?: return@mapNotNull null
        FileHit(path = p[3], size = size, mtime = p[2].toLongOrNull() ?: 0L)
    }.toList()

    fun dups(raw: String): List<DupGroup> {
        val groups = LinkedHashMap<String, MutableList<FileHit>>()
        val sizes = HashMap<String, Long>()
        for (line in raw.lineSequence()) {
            if (!line.startsWith("D\t")) continue
            val p = line.split('\t', limit = 5)
            if (p.size < 5) continue
            val size = p[2].toLongOrNull() ?: continue
            sizes[p[1]] = size
            groups.getOrPut(p[1]) { ArrayList() }.add(FileHit(p[4], size, p[3].toLongOrNull() ?: 0L))
        }
        return groups.map { (k, v) -> DupGroup(sizes.getValue(k), v) }
    }

    fun appDirs(raw: String): List<AppDir> = raw.lineSequence().mapNotNull { line ->
        if (!line.startsWith("A\t")) return@mapNotNull null
        val p = line.split('\t', limit = 5)
        if (p.size < 5) return@mapNotNull null
        AppDir(p[1], p[2], p[3].toLongOrNull() ?: 0L, p[4].toLongOrNull() ?: 0L)
    }.toList()

    fun deleteResults(raw: String): List<DeleteResult> = raw.lineSequence().mapNotNull { line ->
        val p = line.split('\t')
        when {
            p[0] == "OK" && p.size >= 5 -> DeleteResult(
                p[1], true, p[2].toLongOrNull() ?: 0L, p[3].toLongOrNull() ?: 0L, p[4].toLongOrNull() ?: 0L, "OK",
            )
            p[0] == "ERR" && p.size >= 3 -> DeleteResult(p[1], false, 0, 0, 0, p[2])
            else -> null
        }
    }.toList()
}

/** Rozmiary aplikacji z `dumpsys diskstats` (tabele: Package Names / App Sizes / App Data Sizes / Cache Sizes). */
object DiskStats {

    data class Sizes(val app: Long, val data: Long, val cache: Long)

    fun parse(text: String): Map<String, Sizes> {
        fun column(prefix: String): List<String> {
            val line = text.lineSequence().firstOrNull { it.trimStart().startsWith(prefix) } ?: return emptyList()
            val body = line.substringAfter('[', "").substringBeforeLast(']', "")
            if (body.isBlank()) return emptyList()
            return body.split(',').map { it.trim().removeSurrounding("\"") }
        }
        val names = column("Package Names:")
        val app = column("App Sizes:")
        val data = column("App Data Sizes:")
        val cache = column("Cache Sizes:")
        val out = HashMap<String, Sizes>()
        names.forEachIndexed { i, n ->
            out[n] = Sizes(
                app.getOrNull(i)?.toLongOrNull() ?: 0L,
                data.getOrNull(i)?.toLongOrNull() ?: 0L,
                cache.getOrNull(i)?.toLongOrNull() ?: 0L,
            )
        }
        return out
    }
}
