package dev.ofarafal.enklawa.ui

import android.os.Build
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ofarafal.enklawa.BuildConfig

/** „Ten świat”: informacje o tym świecie i o urządzeniu gospodarza. */
@Composable
fun ComputerApp(env: AppEnv) {
    val webView = runCatching { WebView.getCurrentWebViewPackage()?.versionName }.getOrNull() ?: "nieznany"
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Group("Ten świat") {
            Row2("Tryb", if (env.amnesia) "amnezja" else "zwykły")
            Row2("Klucz sesji", "tylko w RAM, niszczony przy wyjściu")
            Row2("Pliki w sesji", env.vault.names().size.toString())
            Row2("Profil przeglądarki", if (env.profile.isolated) "izolowany (WebView Profile)" else "domyślny WebView (brak izolacji)")
        }
        Group("System gospodarza") {
            Row2("Urządzenie", "${Build.MANUFACTURER} ${Build.MODEL}")
            Row2("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            Row2("Android System WebView", webView)
        }
        Group("Enklawa") {
            Row2("Wersja", "${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF1C2530)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = Xp.Link)
        content()
    }
}

@Composable
private fun Row2(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.45f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.55f))
    }
}
