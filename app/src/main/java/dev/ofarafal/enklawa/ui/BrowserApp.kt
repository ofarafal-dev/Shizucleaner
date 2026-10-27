package dev.ofarafal.enklawa.ui

import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.ofarafal.enklawa.browser.SecureWebView
import dev.ofarafal.enklawa.browser.UrlInput
import dev.ofarafal.enklawa.os.AppCatalog
import dev.ofarafal.enklawa.os.InstalledApp

@Composable
fun BrowserApp(env: AppEnv, startUrl: String? = null) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var field by remember { mutableStateOf<EditText?>(null) }
    var pageTitle by remember { mutableStateOf<String?>(null) }
    var pageIcon by remember { mutableStateOf<Bitmap?>(null) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val origin = AppCatalog.originOf(address)
    val installed = origin?.let { AppStore.find(it) }

    // Tor jest domyślny: do pierwszej gotowości zamiast przeglądarki pokazujemy ekran oczekiwania (WebView w ogóle nie powstaje).
    var passed by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(dev.ofarafal.enklawa.tor.TorManager.isReady) {
        if (dev.ofarafal.enklawa.tor.TorManager.isReady) passed = true
    }

    // Wstecz w historii strony tylko gdy to okno jest aktywne; inaczej back obsługuje powłoka.
    BackHandler(enabled = env.active && canGoBack) { webView?.goBack() }

    val go = rememberUpdatedState {
        webView?.loadUrl(UrlInput.resolve(address))
    }

    if (dev.ofarafal.enklawa.tor.TorManager.routingWanted && !passed && !dev.ofarafal.enklawa.tor.TorManager.isReady) {
        TorGate()
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(Xp.Body).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(Glyph.Back, enabled = canGoBack) { webView?.goBack() }
            AddressField(
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                onTextChanged = { address = it },
                onGo = { go.value() },
                onReady = { field = it },
            )
            GlyphIcon(Glyph.Onion, Modifier.size(22.dp), tint = torIndicatorColor())
            if (origin != null && installed == null) {
                ToolButton(Glyph.Apps) {
                    AppStore.install(env.vault, InstalledApp(AppCatalog.displayName(pageTitle, origin), origin), pageIcon)
                    android.widget.Toast.makeText(ctx, "Zainstalowano jako aplikację", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            ToolButton(Glyph.Reload) { webView?.reload() }
        }
        TorStatusStrip()
        if (progress in 1..99) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { ctx ->
                SecureWebView(ctx).also { wv ->
                    env.profile.attach(wv) // musi być przed pierwszym loadUrl
                    wv.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            // Tylko http(s); intent:, market:, file: itp. nie wychodzą ze świata.
                            val scheme = request.url.scheme?.lowercase()
                            return scheme != "http" && scheme != "https" && scheme != "about"
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            address = url
                            pageTitle = null
                            pageIcon = null
                            field?.takeIf { !it.hasFocus() }?.setText(url)
                            canGoBack = view.canGoBack()
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            canGoBack = view.canGoBack()
                        }
                    }
                    wv.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            progress = newProgress
                        }

                        override fun onReceivedTitle(view: WebView, title: String?) {
                            pageTitle = title
                        }

                        override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
                            pageIcon = icon
                            val o = AppCatalog.originOf(view.url ?: "")
                            if (icon != null && o != null && AppStore.find(o) != null && AppStore.iconFor(o) == null) {
                                AppStore.setIcon(env.vault, o, icon)
                            }
                        }
                    }
                    webView = wv
                    wv.loadUrl(startUrl ?: "about:blank")
                }
            },
            onRelease = { wv ->
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.clearHistory()
                wv.removeAllViews()
                wv.destroy()
            },
        )
    }
}

@Composable
private fun ToolButton(glyph: Glyph, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, Modifier.size(24.dp), tint = Xp.Link.copy(alpha = if (enabled) 1f else 0.3f))
    }
}

/** Pole adresu jako EditText, bo tylko tak można ustawić IME_FLAG_NO_PERSONALIZED_LEARNING. */
@Composable
private fun AddressField(
    modifier: Modifier,
    onTextChanged: (String) -> Unit,
    onGo: () -> Unit,
    onReady: (EditText) -> Unit,
) {
    val goState = rememberUpdatedState(onGo)
    val changedState = rememberUpdatedState(onTextChanged)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            EditText(ctx).apply {
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_URI or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                setSingleLine(true)
                hint = "Adres lub szukaj"
                setTextColor(AColor.BLACK)
                setHintTextColor(AColor.GRAY)
                textSize = 14f
                setPadding(24, 14, 24, 14)
                background = GradientDrawable().apply {
                    cornerRadius = 10f
                    setColor(AColor.WHITE)
                    setStroke(2, 0xFF7F9DB9.toInt())
                }
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        goState.value(); true
                    } else false
                }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
                    override fun afterTextChanged(s: Editable?) {
                        changedState.value(s?.toString().orEmpty())
                    }
                })
                onReady(this)
            }
        },
    )
}
