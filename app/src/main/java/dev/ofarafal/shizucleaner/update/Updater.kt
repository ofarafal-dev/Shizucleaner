package dev.ofarafal.shizucleaner.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import dev.ofarafal.shizucleaner.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val size: Long,
    val sha256: String?,
    val notes: String,
)

sealed interface CheckResult {
    data object UpToDate : CheckResult
    data class Available(val info: UpdateInfo) : CheckResult
    data class Error(val message: String) : CheckResult
}

/**
 * Aktualizacje z GitHub Releases (ostatnie wydanie, tag "v<numer buildu>", asset ShizuCleaner.apk).
 * Numer w tagu == versionCode zbudowanej aplikacji (ustawia go workflow).
 * Jedyne połączenie sieciowe aplikacji; uruchamiane ręcznie lub (domyślnie) przy starcie.
 */
object Updater {

    const val REPO = "ofarafal-dev/shizucleaner"
    private const val ASSET_NAME = "ShizuCleaner.apk"
    const val ACTION_INSTALL_STATUS = "dev.ofarafal.shizucleaner.INSTALL_STATUS"

    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = open("https://api.github.com/repos/$REPO/releases/latest")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            when (val code = conn.responseCode) {
                200 -> parse(conn.inputStream.bufferedReader().use { it.readText() })
                404 -> CheckResult.Error(
                    "Nie znaleziono wydania. Repozytorium musi być publiczne i mieć co najmniej jedno wydanie."
                )
                403, 429 -> CheckResult.Error("Limit zapytań GitHub — spróbuj za kilka minut.")
                else -> CheckResult.Error("GitHub zwrócił kod $code")
            }
        } catch (e: Exception) {
            CheckResult.Error("Brak połączenia: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun parse(body: String): CheckResult {
        val json = JSONObject(body)
        val tag = json.optString("tag_name")
        val code = tag.removePrefix("v").toIntOrNull()
            ?: return CheckResult.Error("Nieprawidłowy tag wydania: $tag")
        if (code <= BuildConfig.VERSION_CODE) return CheckResult.UpToDate

        val assets = json.optJSONArray("assets")
        for (i in 0 until (assets?.length() ?: 0)) {
            val a = assets!!.getJSONObject(i)
            if (a.optString("name") != ASSET_NAME) continue
            val digest = a.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")
            return CheckResult.Available(
                UpdateInfo(
                    versionCode = code,
                    versionName = json.optString("name").ifEmpty { tag },
                    apkUrl = a.getString("browser_download_url"),
                    size = a.optLong("size"),
                    sha256 = digest,
                    notes = json.optString("body"),
                )
            )
        }
        return CheckResult.Error("Wydanie $tag nie zawiera pliku $ASSET_NAME")
    }

    /** Pobiera APK do cache aplikacji. Zwraca plik albo rzuca wyjątek z czytelnym komunikatem. */
    suspend fun download(ctx: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, "ShizuCleaner-${info.versionCode}.apk")

            val conn = open(info.apkUrl)
            if (conn.responseCode != 200) error("Pobieranie nie powiodło się (kod ${conn.responseCode})")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.size
            val sha = MessageDigest.getInstance("SHA-256")

            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        sha.update(buf, 0, n)
                        read += n
                        if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }

            val actual = sha.digest().joinToString("") { "%02x".format(it) }
            if (info.sha256 != null && !actual.equals(info.sha256, ignoreCase = true)) {
                out.delete()
                error("Suma kontrolna się nie zgadza — plik odrzucony")
            }
            out
        }

    /**
     * Instalacja przez PackageInstaller. System sam odrzuci APK podpisany innym kluczem niż zainstalowana
     * aplikacja, więc to jest właściwa weryfikacja autentyczności (SHA-256 wyżej chroni tylko przed uszkodzeniem).
     * Na Androidzie 12+ kolejne aktualizacje mogą przejść bez dialogu, jeśli to my zainstalowaliśmy poprzednią wersję.
     */
    fun install(ctx: Context, apk: File) {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(ctx.packageName)
            if (Build.VERSION.SDK_INT >= 31) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("update.apk", 0, apk.length()).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            val intent = Intent(ctx, InstallReceiver::class.java).setAction(ACTION_INSTALL_STATUS)
            val pi = PendingIntent.getBroadcast(
                ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(pi.intentSender)
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ShizuCleaner/${BuildConfig.VERSION_NAME}")
        }
}
