package dev.ofarafal.enklawa.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import dev.ofarafal.enklawa.browser.BrowserProfile
import dev.ofarafal.enklawa.crypto.SessionVault
import dev.ofarafal.enklawa.os.AppCatalog
import dev.ofarafal.enklawa.os.WebApps

/** Wszystko, czego aplikacja w oknie może potrzebować od świata, w którym działa. */
class AppEnv(
    val amnesia: Boolean,
    val profile: BrowserProfile,
    val vault: SessionVault,
    val active: Boolean,
    val open: (String) -> Unit,
    /** Nazwa sejfu, gdy świat działa w trybie z pamięcią; w amnezji i zwykłym null. */
    val vaultName: String? = null,
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
    /** Własna ikona (np. favicon strony) albo litera; gdy brak obu, rysujemy [glyph]. */
    val iconBitmap: ImageBitmap? = null,
    val letter: String? = null,
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
    AppDef("tor", "Tor", Glyph.Onion, Color(0xFFA48BE0), Color(0xFF311B92), content = { TorApp(it) }),
)

private val TilePalette = listOf(
    Color(0xFF5BC8FF) to Color(0xFF1565C0),
    Color(0xFF8EE08F) to Color(0xFF2E7D32),
    Color(0xFFFFB38A) to Color(0xFFD1491E),
    Color(0xFFD08CE6) to Color(0xFF6A1B9A),
    Color(0xFFFFE27A) to Color(0xFFF08A00),
    Color(0xFF9AD0FF) to Color(0xFF2F6BD6),
)

/** Aplikacje z rejestru albo strony otwarte jako aplikacje (id „web:<adres>”; zainstalowane mają nazwę i ikonę z katalogu). */
fun appById(id: String): AppDef? =
    AppRegistry.firstOrNull { it.id == id }
        ?: id.removePrefix("mini:").takeIf { id.startsWith("mini:") }?.let { mid ->
            val m = dev.ofarafal.enklawa.miniapp.MiniAppStore.find(mid)
            val (top, bottom) = TilePalette[(mid.hashCode() and 0x7fffffff) % TilePalette.size]
            AppDef(id, m?.name ?: mid, Glyph.Apps, top, bottom, content = { MiniAppView(it, mid) }, letter = AppCatalog.initial(m?.name ?: mid))
        }
        ?: WebApps.urlOf(id)?.let { url ->
            val installed = AppStore.find(url)
            val title = installed?.name ?: WebApps.titleFor(url)
            val (top, bottom) = TilePalette[(url.hashCode() and 0x7fffffff) % TilePalette.size]
            AppDef(
                id, title, Glyph.Globe, top, bottom,
                content = { BrowserApp(it, startUrl = url) },
                iconBitmap = AppStore.iconFor(url),
                letter = AppCatalog.initial(title),
            )
        }
