package dev.ofarafal.enklawa

import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.ofarafal.enklawa.browser.BrowserProfile
import dev.ofarafal.enklawa.session.WorldSession
import dev.ofarafal.enklawa.ui.BrowserScreen
import dev.ofarafal.enklawa.ui.DesktopScreen
import dev.ofarafal.enklawa.ui.EnklawaTheme
import dev.ofarafal.enklawa.ui.EntryScreen
import dev.ofarafal.enklawa.ui.SettingsScreen

private enum class Screen { Desktop, Browser, Settings }

class MainActivity : ComponentActivity() {

    private var session by mutableStateOf<WorldSession?>(null)
    private var profile by mutableStateOf<BrowserProfile?>(null)
    private var screen by mutableStateOf(Screen.Desktop)
    private var ending by mutableStateOf(false)
    private var amnesia by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Brak zrzutów ekranu i podglądu w ostatnich aplikacjach (w całej aplikacji, także na ekranie wejścia).
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        setContent {
            EnklawaTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val s = session
                    val p = profile
                    when {
                        ending -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Niszczenie świata…")
                        }
                        s == null || p == null -> EntryScreen(onEnter = ::enterWorld)
                        else -> Crossfade(targetState = screen, animationSpec = tween(260), label = "screen") { sc ->
                            when (sc) {
                                Screen.Browser -> BrowserScreen(p, onClose = { screen = Screen.Desktop })
                                Screen.Settings -> SettingsScreen(amnesia = amnesia, onBack = { screen = Screen.Desktop })
                                Screen.Desktop -> DesktopScreen(
                                    isolatedProfile = p.isolated,
                                    amnesia = amnesia,
                                    onOpenBrowser = { screen = Screen.Browser },
                                    onOpenSettings = { screen = Screen.Settings },
                                    onExit = ::exitWorld,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun enterWorld(amnesiaMode: Boolean) {
        amnesia = amnesiaMode
        // Profile po przerwanym procesie (crash) — usuwamy, zanim zaczniemy nową sesję.
        runCatching { BrowserProfile.sweepLeftovers() }
        val s = WorldSession.start((application as EnklawaApp).sessionParent)
        val p = BrowserProfile.create(persistent = !amnesiaMode)
        s.onEnd { p.destroy() }
        session = s
        profile = p
        screen = Screen.Desktop
    }

    /**
     * Wyjście: usuwamy UI przeglądarki (niszczy WebView), po krótkiej chwili kończymy sesję
     * (profil, klucz, pliki), zamykamy zadanie i zabijamy proces. Gdyby deleteProfile się nie udało,
     * pozostałość usunie sweepLeftovers przy następnym wejściu.
     */
    private fun exitWorld() {
        val s = session ?: return
        ending = true
        session = null
        profile = null
        screen = Screen.Desktop
        window.decorView.postDelayed({
            s.end()
            finishAndRemoveTask()
            Process.killProcess(Process.myPid())
        }, 400)
    }

    override fun onDestroy() {
        // Zamknięcie aktywności bez przycisku (np. przesunięcie z ekranu) też kończy świat.
        if (isFinishing) session?.end()
        super.onDestroy()
    }
}
