package dev.ofarafal.enklawa.miniapp

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Uprawnienia, o które mini-aplikacja może prosić. Domyślnie nie ma żadnych. */
enum class Perm(val key: String, val label: String) {
    STORAGE("storage", "Własny zaszyfrowany magazyn danych (zapis w sejfie / w sesji)"),
    NETWORK("network", "Dostęp do internetu (wyłącznie przez Tora, zablokowany gdy Tor nie działa)"),
    ;

    companion object {
        fun fromKey(k: String): Perm? = entries.firstOrNull { it.key == k }
    }
}

data class Manifest(
    val id: String,
    val name: String,
    val version: String,
    val entry: String,
    val permissions: Set<Perm>,
)

class MiniAppException(message: String) : Exception(message)

/** Czysta logika paczek mini-aplikacji: manifest, rozpakowanie zip z limitami, ścieżki, typy MIME, nagłówki. */
object MiniApps {
    const val MANIFEST = "manifest.json"
    const val MAX_FILES = 300
    const val MAX_FILE_BYTES = 4 * 1024 * 1024
    const val MAX_TOTAL_BYTES = 8 * 1024 * 1024
    const val MAX_DATA_BYTES = 1024 * 1024
    private val ID = Regex("[a-z0-9][a-z0-9-]{1,31}")

    fun isValidId(id: String) = ID.matches(id)

    /** Normalizuje ścieżkę z paczki; null, gdy próbuje wyjść poza katalog lub jest dziwna (zip-slip). */
    fun safePath(raw: String): String? {
        if (raw.isEmpty() || raw.length > 200 || raw.contains('\\') || raw.contains('\u0000') || raw.startsWith("/")) return null
        val parts = raw.split('/').filter { it.isNotEmpty() && it != "." }
        if (parts.isEmpty() || parts.any { it == ".." }) return null
        return parts.joinToString("/")
    }

    fun parseManifest(text: String): Manifest {
        val o = try { JSONObject(text) } catch (e: Exception) { throw MiniAppException("manifest.json nie jest poprawnym JSON-em") }
        val id = o.optString("id")
        if (!isValidId(id)) throw MiniAppException("id: małe litery, cyfry i myślniki, 2–32 znaki")
        val name = o.optString("name").trim()
        if (name.isEmpty() || name.length > 40) throw MiniAppException("name: 1–40 znaków")
        val entry = safePath(o.optString("entry", "index.html")) ?: throw MiniAppException("entry: niepoprawna ścieżka")
        val perms = mutableSetOf<Perm>()
        o.optJSONArray("permissions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val k = arr.optString(i)
                perms += Perm.fromKey(k) ?: throw MiniAppException("nieznane uprawnienie: $k")
            }
        }
        return Manifest(id, name, o.optString("version", "1").take(20), entry, perms)
    }

    /** Czyta zip do mapy ścieżka→bajty. Odrzuca zip-slip, zbyt duże/liczne pliki i paczki bez manifestu. */
    fun readZip(input: InputStream): Map<String, ByteArray> {
        val raw = linkedMapOf<String, ByteArray>()
        var total = 0
        ZipInputStream(input).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                if (e.isDirectory) continue
                val path = safePath(e.name) ?: throw MiniAppException("niebezpieczna ścieżka w paczce: ${e.name}")
                if (raw.size >= MAX_FILES) throw MiniAppException("za dużo plików (limit $MAX_FILES)")
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8192)
                var size = 0
                while (true) {
                    val n = zin.read(buf)
                    if (n < 0) break
                    size += n
                    total += n
                    if (size > MAX_FILE_BYTES) throw MiniAppException("plik $path jest za duży")
                    if (total > MAX_TOTAL_BYTES) throw MiniAppException("paczka jest za duża (limit 8 MB po rozpakowaniu)")
                    out.write(buf, 0, n)
                }
                raw[path] = out.toByteArray()
            }
        }
        // Pojedynczy katalog główny w zipie (częsty przypadek przy pakowaniu folderu) zdejmujemy.
        if (!raw.containsKey(MANIFEST)) {
            val roots = raw.keys.map { it.substringBefore('/') }.toSet()
            if (roots.size == 1 && raw.keys.all { it.contains('/') }) {
                val prefix = roots.first() + "/"
                val stripped = raw.entries.associate { it.key.removePrefix(prefix) to it.value }
                if (stripped.containsKey(MANIFEST)) return stripped
            }
            throw MiniAppException("w paczce brakuje manifest.json")
        }
        return raw
    }

    /** Sprawdza paczkę jako całość i zwraca jej manifest. */
    fun validate(files: Map<String, ByteArray>): Manifest {
        val m = parseManifest(files[MANIFEST]?.toString(Charsets.UTF_8) ?: throw MiniAppException("w paczce brakuje manifest.json"))
        if (!files.containsKey(m.entry)) throw MiniAppException("brakuje pliku startowego ${m.entry}")
        return m
    }

    fun mime(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "text/html"
        "js", "mjs" -> "text/javascript"
        "css" -> "text/css"
        "json" -> "application/json"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "ico" -> "image/x-icon"
        "woff2" -> "font/woff2"
        "woff" -> "font/woff"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    private const val HOST_SUFFIX = ".microcosm.invalid"

    /** Każda mini-aplikacja ma własny „origin”, więc localStorage i ciasteczka są od siebie odizolowane. */
    fun hostFor(id: String) = "app-$id$HOST_SUFFIX"
    fun originFor(id: String) = "https://${hostFor(id)}"

    /** Nagłówek CSP: bez uprawnienia do sieci strona nie może nigdzie nawiązać połączenia (także fetch/img/frame). */
    fun csp(net: Boolean): String =
        if (net) "default-src 'self' 'unsafe-inline' 'unsafe-eval' data: blob: https: wss:; frame-ancestors 'none'"
        else "default-src 'self' 'unsafe-inline' data: blob:; connect-src 'self'; frame-ancestors 'none'"

    fun storageKey(id: String, rest: String) = "mini/$id/$rest"
    fun fileKey(id: String, path: String) = storageKey(id, "f/$path")

    /** Z listy nazw w magazynie wybiera id zainstalowanych mini-aplikacji. */
    fun installedIds(names: Collection<String>): List<String> =
        names.mapNotNull { Regex("mini/([a-z0-9-]+)/m").matchEntire(it)?.groupValues?.get(1) }.sorted()
}
