package dev.ofarafal.enklawa.browser

import java.net.URLEncoder

/** Zamienia tekst z paska adresu na adres do załadowania: URL albo zapytanie do wyszukiwarki. */
object UrlInput {

    const val SEARCH = "https://duckduckgo.com/?q="

    fun resolve(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return "about:blank"
        if (s.startsWith("http://") || s.startsWith("https://")) return s
        val looksLikeHost = !s.contains(' ') && (s.contains('.') || s.startsWith("localhost"))
        return if (looksLikeHost) "https://$s" else SEARCH + URLEncoder.encode(s, "UTF-8")
    }
}
