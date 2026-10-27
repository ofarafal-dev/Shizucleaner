package dev.ofarafal.shizucleaner.data

import android.content.Context
import dev.ofarafal.shizucleaner.service.FsOps
import org.json.JSONObject
import java.io.File

enum class Risk(val label: String) {
    SAFE("Bezpieczne"),
    CAUTION("Ostrożnie"),
    UNKNOWN("Nieznane"),
    CRITICAL("Krytyczne"),
}

data class KnownPackage(val pkg: String, val label: String, val risk: Risk, val note: String)

/**
 * Baza ocen pakietów i folderów po aplikacjach. Źródła, od najnowszego:
 * plik pobrany z GitHuba (filesDir/packages.json) -> asset w APK.
 * Lista pakietów krytycznych NIE jest tu — siedzi w [CriticalPackages], żeby baza nie mogła jej odblokować.
 */
object PackageDb {

    const val FILE = "packages.json"
    private val pkgRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")

    @Volatile
    private var packages: Map<String, KnownPackage> = emptyMap()

    @Volatile
    private var folders: Map<String, List<String>> = emptyMap()

    data class Parsed(val packages: Map<String, KnownPackage>, val folders: Map<String, List<String>>)

    fun load(ctx: Context) {
        val downloaded = File(ctx.filesDir, FILE)
        val parsed = (if (downloaded.exists()) runCatching { parse(downloaded.readText()) }.getOrNull() else null)
            ?: parse(ctx.assets.open(FILE).bufferedReader().use { it.readText() })
        packages = parsed.packages
        folders = parsed.folders
    }

    /** Rzuca wyjątek przy niepoprawnym JSON-ie. Pomija wpisy z błędnym pakietem lub poziomem ryzyka. */
    fun parse(text: String): Parsed {
        val json = JSONObject(text)
        val pk = LinkedHashMap<String, KnownPackage>()
        val arr = json.getJSONArray("packages")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val pkg = o.optString("pkg")
            val risk = runCatching { Risk.valueOf(o.optString("risk")) }.getOrNull()
            if (!pkgRegex.matches(pkg) || (risk != Risk.SAFE && risk != Risk.CAUTION)) continue
            pk[pkg] = KnownPackage(pkg, o.optString("label", pkg), risk, o.optString("note"))
        }
        val fo = LinkedHashMap<String, List<String>>()
        val lf = json.optJSONObject("leftoverFolders")
        if (lf != null) {
            for (key in lf.keys()) {
                if (!pkgRegex.matches(key)) continue
                val names = lf.getJSONArray(key)
                val ok = (0 until names.length()).map { names.getString(it) }.filter {
                    it.isNotBlank() && it != "." && it != ".." && !it.contains('/') && it !in FsOps.PROTECTED_TOP
                }
                if (ok.isNotEmpty()) fo[key] = ok
            }
        }
        return Parsed(pk, fo)
    }

    fun lookup(pkg: String): KnownPackage? = packages[pkg]

    fun isCritical(pkg: String): Boolean = CriticalPackages.isCritical(pkg)

    /** pakiet -> foldery w katalogu głównym pamięci, które po nim zostają */
    fun leftoverFolders(): Map<String, List<String>> = folders
}
