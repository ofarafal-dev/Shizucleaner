package dev.ofarafal.enklawa.os

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest

/** Zainstalowana „aplikacja”: strona z nazwą. Lista leży w magazynie świata (w sejfie trwale, w amnezji tylko do wyjścia). */
data class InstalledApp(val name: String, val url: String) {
    val id: String get() = WebApps.idFor(url)
}

/** Czysta logika katalogu aplikacji (bez Androida), testowalna na JVM. */
object AppCatalog {
    const val FILE = ".apps.json"
    const val MAX_NAME = 24

    fun parse(json: String?): List<InstalledApp> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val url = o.getString("url")
                if (WebApps.urlOf(WebApps.idFor(url)) == null) null else InstalledApp(o.getString("name"), url)
            }
        }.getOrDefault(emptyList())
    }

    fun serialize(list: List<InstalledApp>): String {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("name", it.name).put("url", it.url)) }
        return arr.toString()
    }

    /** Dodaje aplikację; ten sam adres tylko odświeża nazwę (bez duplikatów). */
    fun add(list: List<InstalledApp>, app: InstalledApp): List<InstalledApp> =
        list.filter { it.url != app.url } + app

    fun remove(list: List<InstalledApp>, url: String): List<InstalledApp> = list.filter { it.url != url }

    /** Adres „głównej” strony aplikacji: schemat + host (+ port). Tylko http i https; inne schematy odrzucamy (null). */
    fun originOf(raw: String): String? = runCatching {
        val u = URI(raw.trim())
        val scheme = u.scheme?.lowercase()
        val host = u.host?.lowercase()
        if ((scheme != "http" && scheme != "https") || host.isNullOrBlank()) null
        else buildString {
            append(scheme).append("://").append(host)
            if (u.port != -1) append(':').append(u.port)
        }
    }.getOrNull()

    /** Nazwa z tytułu strony (przycięta); gdy pusta, host bez „www.”. */
    fun displayName(title: String?, url: String): String {
        val host = runCatching { URI(url).host?.removePrefix("www.") }.getOrNull().orEmpty()
        val t = title?.trim().orEmpty()
        val base = if (t.isEmpty() || t.startsWith("http", ignoreCase = true) || t == "about:blank") host else t
        val clean = base.ifEmpty { url }
        return if (clean.length > MAX_NAME) clean.take(MAX_NAME - 1).trimEnd() + "…" else clean
    }

    fun initial(name: String): String = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

    /** Klucz pliku ikony w magazynie; zależy tylko od adresu. */
    fun iconKey(url: String): String {
        val h = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        return "icon:" + h.take(8).joinToString("") { "%02x".format(it) }
    }

    /** Nazwy plików systemowych (katalog aplikacji, ikony) — ukrywane w „Plikach”. */
    fun isInternalName(name: String): Boolean = name.startsWith(".") || name.startsWith("icon:")
}
