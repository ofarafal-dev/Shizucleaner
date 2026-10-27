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
class BrowserProfile private constructor(
    val name: String,
    val isolated: Boolean,
    val persistent: Boolean = false,
) {

    fun attach(webView: WebView) {
        if (isolated) WebViewCompat.setProfile(webView, name)
    }

    /** Wywołać PO zniszczeniu wszystkich WebView tego profilu — inaczej deleteProfile rzuci wyjątek. */
    fun destroy(): Boolean {
        // Profil zwykłego (nie-amnezyjnego) świata zostaje; kasuje go dopiero użytkownik.
        if (persistent) return true
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

        /** Nazwa celowo NIE zaczyna się od PREFIX, żeby sprzątanie po amnezji jej nie ruszało. */
        const val PERSISTENT_NAME = "enkp_main"

        /** Które profile sprzątanie po przerwanej sesji amnezji ma usunąć. */
        fun leftoversToSweep(names: Collection<String>): List<String> = names.filter { it.startsWith(PREFIX) }

        fun isSupported(): Boolean = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

        fun create(persistent: Boolean = false, random: SecureRandom = SecureRandom()): BrowserProfile {
            if (!isSupported()) return BrowserProfile("", false, persistent)
            if (persistent) {
                ProfileStore.getInstance().getOrCreateProfile(PERSISTENT_NAME)
                return BrowserProfile(PERSISTENT_NAME, true, true)
            }
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
            leftoversToSweep(store.allProfileNames).forEach {
                if (runCatching { store.deleteProfile(it) }.getOrDefault(false)) n++
            }
            return n
        }
    }
}
