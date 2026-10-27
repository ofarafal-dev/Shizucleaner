package dev.ofarafal.enklawa.browser

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.security.SecureRandom

/**
 * Izolowany profil przeglądarki świata (androidx.webkit.Profile): osobne ciasteczka, localStorage,
 * IndexedDB, cache i uprawnienia, oddzielone od profilu domyślnego WebView w całym systemie.
 *
 * Wymaga WebView z obsługą MULTI_PROFILE. Gdy go brak, [isolated] == false i przeglądarka działa
 * na profilu domyślnym, a przy wyjściu czyścimy go ręcznie (słabsza gwarancja — UI to pokazuje).
 */
class BrowserProfile private constructor(val name: String, val isolated: Boolean) {

    fun attach(webView: WebView) {
        if (isolated) WebViewCompat.setProfile(webView, name)
    }

    /** Wywołać PO zniszczeniu wszystkich WebView tego profilu — inaczej deleteProfile rzuci wyjątek. */
    fun destroy(): Boolean {
        return runCatching {
            if (isolated) {
                ProfileStore.getInstance().deleteProfile(name)
            } else {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
            }
        }.isSuccess
    }

    companion object {
        const val PREFIX = "enk_"

        fun isSupported(): Boolean = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

        fun create(random: SecureRandom = SecureRandom()): BrowserProfile {
            if (!isSupported()) return BrowserProfile("", false)
            val id = ByteArray(8).also(random::nextBytes).joinToString("") { "%02x".format(it) }
            val name = PREFIX + id
            ProfileStore.getInstance().getOrCreateProfile(name)
            return BrowserProfile(name, true)
        }

        /** Usuwa profile `enk_*` zostawione przez poprzedni, przerwany proces. Zwraca liczbę usuniętych. */
        fun sweepLeftovers(): Int {
            if (!isSupported()) return 0
            val store = ProfileStore.getInstance()
            var n = 0
            store.allProfileNames.filter { it.startsWith(PREFIX) }.forEach {
                if (runCatching { store.deleteProfile(it) }.getOrDefault(false)) n++
            }
            return n
        }
    }
}
