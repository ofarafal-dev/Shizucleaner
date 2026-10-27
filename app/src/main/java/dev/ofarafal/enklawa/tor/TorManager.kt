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
        /** Demon działa i ma port SOCKS, ale obwody jeszcze się budują (brak potwierdzenia, że ruch przechodzi). */
        data class Running(val socksPort: Int) : State
        /** Test SOCKS przez Tora zakończył się sukcesem: ruch faktycznie przechodzi. */
        data class Ready(val socksPort: Int) : State
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

    /** Tor potwierdzony testem; dopiero wtedy ruch przeglądarki jest naprawdę anonimizowany. */
    val isReady: Boolean get() = state is State.Ready

    private var probe: Thread? = null
    private var probeFails = 0

    /**
     * Domyślny start świata: Tor włączony, a ruch WebView od razu zablokowany (proxy na martwy port), dopóki Tor
     * nie zgłosi gotowości. Dzięki temu nic nie wychodzi bezpośrednio w czasie uruchamiania.
     */
    fun begin(context: Context) {
        routingWanted = true
        applyBlocking()
        start(context)
    }

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
                        TorService.STATUS_OFF -> if (state !is State.Failed) {
                            state = State.Off
                            routingApplied = false
                            // Przy włączonym przekierowaniu brak Tora = brak ruchu, a nie ruch bezpośredni.
                            if (routingWanted) applyBlocking() else clearRouting()
                        }
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
            if (state !is State.Ready) state = State.Running(port)
            if (routingWanted) applyRouting(port)
            startProbe(port)
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
            is State.Ready -> s.socksPort
            else -> -1
        }
        if (port > 0) applyRouting(port)
    }

    private fun applyBlocking() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return
        val cfg = ProxyConfig.Builder().addProxyRule("socks5://127.0.0.1:1").build()
        runCatching { ProxyController.getInstance().setProxyOverride(cfg, inline) { } }
    }

    private fun applyRouting(port: Int) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            state = State.Failed("Ten WebView nie obsługuje proxy — przekierowanie przez Tor niemożliwe")
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

    private fun startProbe(port: Int) {
        if (probe?.isAlive == true) return
        probeFails = 0
        val t = Thread {
            while (!Thread.currentThread().isInterrupted) {
                val ok = SocksProbe.canConnect(port)
                main.post { onProbe(ok, port) }
                try {
                    Thread.sleep(if (ok) 30_000L else 4_000L)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        t.isDaemon = true
        probe = t
        t.start()
    }

    private fun onProbe(ok: Boolean, port: Int) {
        if (state is State.Off || state is State.Failed || state is State.Starting) return
        if (ok) {
            probeFails = 0
            state = State.Ready(port)
        } else if (state is State.Ready && ++probeFails >= 3) {
            // Pojedynczy błąd nie przełącza stanu; trzy z rzędu tak.
            state = State.Running(port)
        }
    }

    private fun stopProbe() {
        probe?.interrupt()
        probe = null
    }

    /**
     * Zatrzymuje demona. Z [keepRouting] przekierowanie zostaje „chciane”: ruch WebView jest wtedy blokowany,
     * a nie wraca po cichu na łącze bezpośrednie. Bez niego (wyjście ze świata) wszystko jest czyszczone.
     */
    fun stop(context: Context, keepRouting: Boolean = false) {
        stopProbe()
        val app = context.applicationContext
        if (keepRouting && routingWanted) {
            routingApplied = false
            applyBlocking()
        } else {
            routingWanted = false
            clearRouting()
        }
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
