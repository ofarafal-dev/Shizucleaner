package dev.ofarafal.enklawa

import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.ofarafal.enklawa.browser.BrowserProfile
import dev.ofarafal.enklawa.crypto.SessionKey
import dev.ofarafal.enklawa.session.AutoLock
import dev.ofarafal.enklawa.session.WorldSession
import dev.ofarafal.enklawa.vault.VaultInfo
import dev.ofarafal.enklawa.ui.EnklawaTheme
import dev.ofarafal.enklawa.ui.EntryScreen
import dev.ofarafal.enklawa.ui.Glyph
import dev.ofarafal.enklawa.ui.GlyphIcon
import dev.ofarafal.enklawa.ui.XpShell

class MainActivity : ComponentActivity() {

    private var session by mutableStateOf<WorldSession?>(null)
    private var profile by mutableStateOf<BrowserProfile?>(null)
    private var ending by mutableStateOf(false)
    private var amnesia by mutableStateOf(false)
    private var vaultName by mutableStateOf<String?>(null)
    private var stoppedAt: Long? = null

    // Zgoda na powiadomienia (Android 13+): bez niej usługa w tle nadal działa, tylko jej powiadomienia nie widać.
    private val notifPermission = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) runCatching { notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    }

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
                        ending -> ShutdownScreen()
                        s == null || p == null -> EntryScreen(
                            vaults = (application as EnklawaApp).vaults,
                            onEnter = ::enterWorld,
                            onVaultUnlocked = ::enterVault,
                        )
                        else -> XpShell(amnesia = amnesia, vaultName = vaultName, profile = p, vault = s.vault, onExit = ::exitWorld)
                    }
                }
            }
        }
    }

    private fun enterWorld(amnesiaMode: Boolean) {
        amnesia = amnesiaMode
        vaultName = null
        // Profile po przerwanym procesie (crash) — usuwamy, zanim zaczniemy nową sesję.
        runCatching { BrowserProfile.sweepLeftovers() }
        val s = WorldSession.start((application as EnklawaApp).sessionParent)
        val p = BrowserProfile.create(persistent = !amnesiaMode)
        askNotifications()
        dev.ofarafal.enklawa.tor.TorManager.begin(applicationContext)
        s.onEnd { p.destroy() }
        session = s
        profile = p
    }

    /** Tryb z pamięcią: klucz już odblokowany hasłem; przeglądarka na profilu tymczasowym. */
    private fun enterVault(info: VaultInfo, key: SessionKey) {
        amnesia = false
        vaultName = info.name
        runCatching { BrowserProfile.sweepLeftovers() }
        val dataDir = (application as EnklawaApp).vaults.dataDir(info.id)
        val s = WorldSession.openVault(dataDir, key)
        val p = BrowserProfile.create(persistent = false)
        askNotifications()
        dev.ofarafal.enklawa.tor.TorManager.begin(applicationContext)
        s.onEnd { p.destroy() }
        session = s
        profile = p
    }

    /**
     * Wyjście: usuwamy UI (niszczy WebView), po krótkiej chwili kończymy sesję
     * (profil, klucz, pliki), zamykamy zadanie i zabijamy proces. Gdyby deleteProfile się nie udało,
     * pozostałość usunie sweepLeftovers przy następnym wejściu.
     */
    private fun exitWorld() {
        val s = session ?: return
        ending = true
        session = null
        profile = null
        window.decorView.postDelayed({
            runCatching { dev.ofarafal.enklawa.tor.TorManager.shutdown(applicationContext) }
            s.end()
            finishAndRemoveTask()
            Process.killProcess(Process.myPid())
        }, 500)
    }

    override fun onStop() {
        super.onStop()
        if (vaultName != null && session != null) stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        val was = stoppedAt
        stoppedAt = null
        if (vaultName != null && AutoLock.shouldLock(was, SystemClock.elapsedRealtime())) lockVault()
    }

    /** Blokada sejfu po dłuższym czasie w tle: klucz znika z RAM, wracamy na ekran wejścia (proces żyje dalej). */
    private fun lockVault() {
        val s = session ?: return
        session = null
        profile = null
        vaultName = null
        runCatching { dev.ofarafal.enklawa.tor.TorManager.shutdown(applicationContext) }
        window.decorView.post { s.end() }
    }

    override fun onDestroy() {
        // Zamknięcie aktywności bez przycisku (np. przesunięcie z ekranu) też kończy świat.
        if (isFinishing) session?.end()
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun ShutdownScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1220), Color(0xFF132038), Color(0xFF0B1220)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GlyphIcon(Glyph.Logo, Modifier.size(72.dp))
            Text(
                "Zamykanie świata…",
                color = Color.White, fontSize = 22.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text("Niszczenie klucza sesji", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }
    }
}
