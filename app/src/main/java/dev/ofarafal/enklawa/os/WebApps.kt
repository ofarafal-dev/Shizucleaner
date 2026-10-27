package dev.ofarafal.enklawa.os

import java.net.URI

data class WebPreset(val name: String, val url: String, val note: String)

/** Strony uruchamiane jak aplikacje: własne okno przeglądarki otwarte od razu na wskazanym adresie. */
object WebApps {
    private const val PREFIX = "web:"

    val presets: List<WebPreset> = listOf(
        WebPreset("Element (Matrix)", "https://app.element.io", "komunikator"),
        WebPreset("Telegram Web", "https://web.telegram.org/k/", "komunikator"),
        WebPreset("WhatsApp Web", "https://web.whatsapp.com", "komunikator; może wymagać wersji dla komputerów"),
        WebPreset("Proton Mail", "https://mail.proton.me", "poczta"),
        WebPreset("OpenStreetMap", "https://www.openstreetmap.org", "mapa"),
        WebPreset("Wikipedia", "https://pl.wikipedia.org", "encyklopedia"),
    )

    fun idFor(url: String): String = PREFIX + url

    fun urlOf(id: String): String? =
        if (id.startsWith(PREFIX)) id.removePrefix(PREFIX).takeIf { it.startsWith("https://") || it.startsWith("http://") } else null

    fun titleFor(url: String): String =
        presets.firstOrNull { it.url == url }?.name
            ?: runCatching { URI(url).host?.removePrefix("www.") }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: url
}
