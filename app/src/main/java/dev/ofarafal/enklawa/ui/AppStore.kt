package dev.ofarafal.enklawa.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.ofarafal.enklawa.crypto.SessionVault
import dev.ofarafal.enklawa.os.AppCatalog
import dev.ofarafal.enklawa.os.InstalledApp
import java.io.ByteArrayOutputStream

/**
 * Zainstalowane strony-aplikacje bieżącego świata. Zapis idzie do magazynu świata (zaszyfrowanego kluczem sesji):
 * w sejfie zostaje na stałe, w amnezji i w trybie zwykłym znika razem ze światem.
 * Ikony pochodzą z samego WebView (favicon odebrany przez przeglądarkę, czyli przez Tora, gdy jest włączony);
 * nie pobieramy ich osobnym zapytaniem, żeby nie wyciekało poza proxy.
 */
object AppStore {
    var apps: List<InstalledApp> by mutableStateOf(emptyList())
        private set
    private val icons = mutableStateMapOf<String, ImageBitmap>()

    fun find(url: String): InstalledApp? = apps.firstOrNull { it.url == url }
    fun iconFor(url: String): ImageBitmap? = icons[url]

    fun load(vault: SessionVault) {
        apps = runCatching { AppCatalog.parse(vault.read(AppCatalog.FILE)?.toString(Charsets.UTF_8)) }.getOrDefault(emptyList())
        icons.clear()
        apps.forEach { app ->
            runCatching {
                vault.read(AppCatalog.iconKey(app.url))?.let { b ->
                    BitmapFactory.decodeByteArray(b, 0, b.size)?.let { icons[app.url] = it.asImageBitmap() }
                }
            }
        }
    }

    fun clear() {
        apps = emptyList()
        icons.clear()
    }

    fun install(vault: SessionVault, app: InstalledApp, icon: Bitmap?) {
        apps = AppCatalog.add(apps, app)
        save(vault)
        if (icon != null) setIcon(vault, app.url, icon)
    }

    fun setIcon(vault: SessionVault, url: String, icon: Bitmap) {
        val out = ByteArrayOutputStream()
        if (!icon.compress(Bitmap.CompressFormat.PNG, 100, out)) return
        runCatching { vault.write(AppCatalog.iconKey(url), out.toByteArray()) }
        icons[url] = icon.asImageBitmap()
    }

    fun uninstall(vault: SessionVault, url: String) {
        apps = AppCatalog.remove(apps, url)
        icons.remove(url)
        runCatching { vault.delete(AppCatalog.iconKey(url)) }
        save(vault)
    }

    private fun save(vault: SessionVault) {
        runCatching { vault.write(AppCatalog.FILE, AppCatalog.serialize(apps).toByteArray(Charsets.UTF_8)) }
    }
}
