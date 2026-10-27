package dev.ofarafal.shizucleaner.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Pobiera aktualną bazę pakietów z repozytorium (bez wydawania nowej wersji aplikacji). */
object DbUpdater {

    private const val URL_RAW =
        "https://raw.githubusercontent.com/ofarafal-dev/Shizucleaner/main/app/src/main/assets/packages.json"

    suspend fun update(ctx: Context): String = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(URL_RAW).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "ShizuCleaner")
            }
            if (conn.responseCode != 200) {
                return@withContext "Nie udało się pobrać (HTTP ${conn.responseCode}). Repozytorium musi być publiczne."
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            if (text.length > 2_000_000) return@withContext "Odrzucono: plik za duży"
            val parsed = PackageDb.parse(text)
            if (parsed.packages.size < 20) return@withContext "Odrzucono: za mało poprawnych wpisów"
            File(ctx.filesDir, PackageDb.FILE).writeText(text)
            PackageDb.load(ctx)
            "Zaktualizowano bazę: ${parsed.packages.size} pakietów, ${parsed.folders.size} wpisów folderów"
        } catch (e: Exception) {
            "Błąd: ${e.message ?: e.javaClass.simpleName}"
        }
    }
}
