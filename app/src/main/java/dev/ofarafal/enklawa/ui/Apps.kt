package dev.ofarafal.enklawa.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.ofarafal.enklawa.browser.BrowserProfile
import dev.ofarafal.enklawa.crypto.SessionVault
import dev.ofarafal.enklawa.os.WebApps

/** Wszystko, czego aplikacja w oknie może potrzebować od świata, w którym działa. */
class AppEnv(
    val amnesia: Boolean,
    val profile: BrowserProfile,
    val vault: SessionVault,
    val active: Boolean,
    val open: (String) -> Unit,
)

/** Definicja aplikacji systemu: ikona, nazwa i zawartość okna. Aplikacje to moduły tego APK, nie zewnętrzne programy. */
data class AppDef(
    val id: String,
    val title: String,
    val glyph: Glyph,
    val top: Color,
    val bottom: Color,
    val singleton: Boolean = true,
    val available: Boolean = true,
    val content: @Composable (AppEnv) -> Unit = {},
)

val AppRegistry: List<AppDef> = listOf(
    AppDef("browser", "Przeglądarka", Glyph.Globe, Color(0xFF5BC8FF), Color(0xFF1565C0), content = { BrowserApp(it) }),
    AppDef("webapps", "Aplikacje web", Glyph.Apps, Color(0xFFD08CE6), Color(0xFF6A1B9A), content = { WebAppsApp(it) }),
    AppDef("notepad", "Notatki", Glyph.Notes, Color(0xFFFFE27A), Color(0xFFF08A00), singleton = false, content = { NotepadApp(it) }),
    AppDef("explorer", "Pliki", Glyph.Folder, Color(0xFF8EE08F), Color(0xFF2E7D32), content = { ExplorerApp(it) }),
    AppDef("calc", "Kalkulator", Glyph.Calc, Color(0xFFFFB38A), Color(0xFFD1491E), content = { CalcApp(it) }),
    AppDef("clock", "Zegar", Glyph.Clock, Color(0xFF9AD0FF), Color(0xFF2F6BD6), content = { ClockApp(it) }),
    AppDef("computer", "Ten świat", Glyph.Computer, Color(0xFFB7D3F2), Color(0xFF4A78B5), content = { ComputerApp(it) }),
    AppDef("settings", "Ustawienia", Glyph.Settings, Color(0xFF9FB4C7), Color(0xFF3B4C5E), content = { SettingsApp(it) }),
    AppDef("tor", "Tor", Glyph.Onion, Color(0xFFA48BE0), Color(0xFF311B92), available = false),
)

/** Aplikacje z rejestru albo strony otwarte jako aplikacje (id „web:<adres>”). */
fun appById(id: String): AppDef? =
    AppRegistry.firstOrNull { it.id == id }
        ?: WebApps.urlOf(id)?.let { url ->
            AppDef(id, WebApps.titleFor(url), Glyph.Globe, Color(0xFF5BC8FF), Color(0xFF1565C0), content = { BrowserApp(it, startUrl = url) })
        }
