package dev.ofarafal.enklawa.ui

import android.graphics.Bitmap
import android.graphics.Color as AColor
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.ofarafal.enklawa.browser.BrowserProfile
import dev.ofarafal.enklawa.browser.SecureWebView
import dev.ofarafal.enklawa.browser.UrlInput

@Composable
fun BrowserScreen(profile: BrowserProfile, onClose: () -> Unit) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var field by remember { mutableStateOf<EditText?>(null) }

    BackHandler { if (canGoBack) webView?.goBack() else onClose() }

    val go = rememberUpdatedState {
        val url = UrlInput.resolve(address)
        webView?.loadUrl(url)
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose) { Text("‹ Pulpit") }
            AddressField(
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                onTextChanged = { address = it },
                onGo = { go.value() },
                onReady = { field = it },
                externalText = address,
            )
            TextButton(onClick = { webView?.reload() }) { Text("⟳") }
        }
        if (progress in 1..99) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { ctx ->
                SecureWebView(ctx).also { wv ->
                    profile.attach(wv) // musi być przed pierwszym loadUrl
                    wv.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            // Tylko http(s); intent:, market:, file: itp. nie wychodzą ze świata.
                            val scheme = request.url.scheme?.lowercase()
                            return scheme != "http" && scheme != "https" && scheme != "about"
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            address = url
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
                    }
                    webView = wv
                    wv.loadUrl("about:blank")
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

/** Pole adresu jako EditText, bo tylko tak można ustawić IME_FLAG_NO_PERSONALIZED_LEARNING. */
@Composable
private fun AddressField(
    modifier: Modifier,
    externalText: String,
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
                setTextColor(AColor.WHITE)
                setHintTextColor(AColor.GRAY)
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
