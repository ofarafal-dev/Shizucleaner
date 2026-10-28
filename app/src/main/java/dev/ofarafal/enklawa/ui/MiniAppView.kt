package dev.ofarafal.enklawa.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.ofarafal.enklawa.browser.SecureWebView
import dev.ofarafal.enklawa.crypto.SessionVault
import dev.ofarafal.enklawa.miniapp.MiniAppStore
import dev.ofarafal.enklawa.miniapp.MiniApps
import dev.ofarafal.enklawa.miniapp.Perm
import dev.ofarafal.enklawa.tor.TorManager
import java.io.ByteArrayInputStream

/** Most JS: window.Microcosm.get/set/remove. Istnieje tylko, gdy użytkownik przyznał uprawnienie „storage”. */
private class StorageBridge(private val vault: SessionVault, private val id: String) {
    @JavascriptInterface fun get(key: String): String? = MiniAppStore.dataGet(vault, id, key.take(200))
    @JavascriptInterface fun set(key: String, value: String): Boolean = MiniAppStore.dataSet(vault, id, key.take(200), value)
    @JavascriptInterface fun remove(key: String) { MiniAppStore.dataSet(vault, id, key.take(200), null) }
}

/** Okno mini-aplikacji: pytanie o uprawnienia przy pierwszym uruchomieniu, potem izolowany WebView ładujący pliki z sejfu. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiniAppView(env: AppEnv, appId: String) {
    val manifest = MiniAppStore.find(appId)
    if (manifest == null) {
        Text("Ta aplikacja nie jest zainstalowana.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
        return
    }
    var grants by remember(appId) { mutableStateOf(MiniAppStore.grants(env.vault, appId)) }
    val decided = grants
    if (decided == null && manifest.permissions.isNotEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("„${manifest.name}” prosi o uprawnienia", color = MaterialTheme.colorScheme.onSurface)
            manifest.permissions.forEach { Text("• ${it.label}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(
                "Aplikacja działa wyłącznie na plikach zapisanych w tym świecie. Bez zgody nie ma ani magazynu, ani internetu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    MiniAppStore.setGrants(env.vault, appId, manifest.permissions); grants = manifest.permissions
                }, modifier = Modifier.weight(1f)) { Text("Zezwól") }
                OutlinedButton(onClick = {
                    MiniAppStore.setGrants(env.vault, appId, emptySet()); grants = emptySet()
                }, modifier = Modifier.weight(1f)) { Text("Uruchom bez uprawnień") }
            }
        }
        return
    }
    val granted = decided ?: emptySet()
    val vault = env.vault
    val origin = MiniApps.originFor(appId)
    val host = MiniApps.hostFor(appId)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            SecureWebView(ctx).also { wv ->
                env.profile.attach(wv)
                wv.settings.allowFileAccess = false
                if (Perm.STORAGE in granted) wv.addJavascriptInterface(StorageBridge(vault, appId), "Microcosm")
                wv.webViewClient = object : WebViewClient() {
                    private fun blocked() = WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))

                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val url = request.url
                        if (url.scheme == "https" && url.host == host) {
                            val path = (url.path ?: "/").trim('/').ifEmpty { manifest.entry }
                            val bytes = MiniAppStore.file(vault, appId, path)
                                ?: return WebResourceResponse("text/plain", "utf-8", 404, "Not found", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                            val net = Perm.NETWORK in granted
                            return WebResourceResponse(
                                MiniApps.mime(path), "utf-8", 200, "OK",
                                mapOf("Content-Security-Policy" to MiniApps.csp(net), "Cache-Control" to "no-store"),
                                ByteArrayInputStream(bytes),
                            )
                        }
                        // Poza własnym origin: tylko z uprawnieniem do sieci i tylko gdy Tor jest gotowy (albo użytkownik świadomie go wyłączył).
                        val netOk = Perm.NETWORK in granted && (!TorManager.routingWanted || TorManager.isReady)
                        return if (netOk) null else blocked()
                    }

                    // Żadnej nawigacji poza własny origin: aplikacja nie zamienia się w przeglądarkę.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        !(request.url.scheme == "https" && request.url.host == host)
                }
                wv.loadUrl("$origin/${manifest.entry}")
            }
        },
        onRelease = { it.destroy() },
    )
}
