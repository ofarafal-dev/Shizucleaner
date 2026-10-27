package dev.ofarafal.enklawa.tor

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import org.torproject.jni.TorService
import java.io.File
import java.util.concurrent.Executor

/** Nazwy katalogów, które biblioteka Tor mogła utworzyć w danych aplikacji (stan, cache konsensusu). */
fun isTorDirName(name: String): Boolean {
    val n = name.lowercase()
    return n.startsWith("tor") || n.startsWith("app_tor")
}

/**
 * Wbudowany demon Tor (Guardian Project tor-android) i przekierowanie ruchu WebView przez jego SOCKS.
 *
 * Uczciwie: to systemowy WebView przez Tor, a nie Tor Browser. Ukrywa adres IP przed odwiedzanymi stronami,
 * ale nie daje odporności na fingerprinting z Tor Browsera. Gdy Tor padnie, a przekierowanie jest włączone,
 * ruch przeglądarki jest blokowany (nie wraca na łącze bezpośrednie).
 */
object TorManager {

    sealed interface State {
        data object Off : State
        data object Starting : State
        /** Demon działa i ma otwarty port SOCKS (obwody budują się w tle; pierwsze żądania mogą potrwać). */
        data class Running(val socksPort: Int) : State
        data class Failed(val message: String) : State
    }

    var state: State by mutableStateOf(State.Off)
        private set

    /** Czy użytkownik chce kierować WebView przez Tor. */
    var routingWanted: Boolean by mutableStateOf(false)
        private set

    /** Czy proxy zostało faktycznie ustawione w WebView. */
    var routingApplied: Boolean by mutableStateOf(false)
        private set

    private val main = Handler(Looper.getMainLooper())
    private val inline = Executor { it.run() }
    private var service: TorService? = null
    private var connection: ServiceConnection? = null
    private var receiver: BroadcastReceiver? = null
    private var attempts = 0

    val isActive: Boolean get() = state !is State.Off && state !is State.Failed

    fun start(context: Context) {
        if (isActive) return
        val app = context.applicationContext
        state = State.Starting
        attempts = 0

        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                when (i.action) {
                    TorService.ACTION_STATUS -> when (i.getStringExtra(TorService.EXTRA_STATUS)) {
                        TorService.STATUS_STARTING -> if (state is State.Off) state = State.Starting
                        TorService.STATUS_ON -> onTorOn()
                        TorService.STATUS_OFF -> if (state !is State.Failed) { state = State.Off; clearRouting() }
                    }
                    TorService.ACTION_ERROR -> {
                        state = State.Failed("Tor zgłosił błąd")
                    }
                }
            }
        }
        receiver = r
        val filter = IntentFilter().apply {
            addAction(TorService.ACTION_STATUS)
            addAction(TorService.ACTION_ERROR)
        }
        // Różne wersje biblioteki wysyłają status zwykłym albo lokalnym broadcastem; słuchamy obu.
        ContextCompat.registerReceiver(app, r, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        LocalBroadcastManager.getInstance(app).registerReceiver(r, filter)

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service = (binder as TorService.LocalBinder).service
            }

            override fun onServiceDisconnected(name: ComponentName) {
                service = null
            }
        }
        connection = conn
        runCatching {
            val intent = Intent(app, TorService::class.java)
            app.startService(intent.setAction(TorService.ACTION_START))
            app.bindService(Intent(app, TorService::class.java), conn, Context.BIND_AUTO_CREATE)
        }.onFailure { state = State.Failed(it.message ?: "Nie można uruchomić Tora") }
    }

    private fun currentPort(): Int = runCatching { service?.socksPort ?: -1 }.getOrDefault(-1)

    private fun onTorOn() {
        val port = currentPort()
        if (port > 0) {
            state = State.Running(port)
            if (routingWanted) applyRouting(port)
        } else if (attempts++ < 30) {
            main.postDelayed({ if (isActive) onTorOn() }, 500)
        } else {
            state = State.Failed("Tor działa, ale nie podał portu SOCKS")
        }
    }

    /** Włącza lub wyłącza przekierowanie WebView przez Tor. Przy włączaniu startuje demona, jeśli trzeba. */
    fun setRouting(context: Context, wanted: Boolean) {
        routingWanted = wanted
        if (!wanted) {
            clearRouting()
            return
        }
        if (!isActive) start(context)
        val port = when (val s = state) {
            is State.Running -> s.socksPort
            else -> -1
        }
        if (port > 0) applyRouting(port)
    }

    private fun applyRouting(port: Int) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            state = State.Failed("Ten WebView nie obsługuje proxy — przekierowanie przez Tor niemożliwe")
            routingWanted = false
            return
        }
        // Bez addDirect(): gdy proxy nie działa, ruch jest blokowany, a nie idzie bezpośrednio.
        val cfg = ProxyConfig.Builder().addProxyRule("socks5://127.0.0.1:$port").build()
        ProxyController.getInstance().setProxyOverride(cfg, inline) { routingApplied = true }
    }

    private fun clearRouting() {
        routingApplied = false
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            runCatching { ProxyController.getInstance().clearProxyOverride(inline) { } }
        }
    }

    fun stop(context: Context) {
        val app = context.applicationContext
        routingWanted = false
        clearRouting()
        receiver?.let {
            runCatching { app.unregisterReceiver(it) }
            runCatching { LocalBroadcastManager.getInstance(app).unregisterReceiver(it) }
        }
        receiver = null
        connection?.let { runCatching { app.unbindService(it) } }
        connection = null
        service = null
        runCatching { app.stopService(Intent(app, TorService::class.java)) }
        state = State.Off
    }

    /** Wyjście ze świata: zatrzymaj Tora i usuń pozostałości jego stanu z danych aplikacji. */
    fun shutdown(context: Context) {
        stop(context)
        wipeData(context)
    }

    fun wipeData(context: Context) {
        val app = context.applicationContext
        val roots = listOfNotNull(app.filesDir, app.cacheDir, app.filesDir?.parentFile)
        roots.forEach { root ->
            root.listFiles { f -> isTorDirName(f.name) }?.forEach { deleteRecursively(it) }
        }
    }

    private fun deleteRecursively(f: File) {
        if (f.isDirectory) f.listFiles()?.forEach(::deleteRecursively)
        f.delete()
    }
}
