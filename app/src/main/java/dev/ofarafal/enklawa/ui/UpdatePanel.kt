package dev.ofarafal.enklawa.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ofarafal.enklawa.BuildConfig
import dev.ofarafal.enklawa.update.CheckResult
import dev.ofarafal.enklawa.update.Updater
import kotlinx.coroutines.launch

/** Wspólny panel aktualizacji (ekran wejścia i Ustawienia): wersja, sprawdzenie, pobranie, instalacja. */
@Composable
fun UpdatePanel(autoCheck: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var check by remember { mutableStateOf<CheckResult?>(null) }
    var checking by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun runCheck() {
        scope.launch {
            checking = true
            message = null
            check = Updater.check()
            checking = false
        }
    }

    LaunchedEffect(Unit) { if (autoCheck) runCheck() }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Wersja ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodyMedium)

        when (val c = check) {
            null -> if (checking) Text("Sprawdzam aktualizacje…", style = MaterialTheme.typography.bodySmall)
            is CheckResult.UpToDate -> Text("Masz najnowszą wersję.", style = MaterialTheme.typography.bodySmall)
            is CheckResult.Error -> Text(c.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            is CheckResult.Available -> {
                Text("Dostępna wersja ${c.info.versionName}", style = MaterialTheme.typography.bodyMedium)
                Button(
                    enabled = progress == null,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!ctx.packageManager.canRequestPackageInstalls()) {
                            ctx.startActivity(
                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            message = "Zezwól na instalację z tego źródła, wróć i dotknij ponownie."
                            return@Button
                        }
                        scope.launch {
                            try {
                                progress = 0f
                                val apk = Updater.download(ctx, c.info) { progress = it }
                                Updater.install(ctx, apk)
                                message = "Instalowanie…"
                            } catch (e: Exception) {
                                message = e.message ?: "Aktualizacja nieudana"
                            } finally {
                                progress = null
                            }
                        }
                    },
                ) { Text("Pobierz i zainstaluj") }
            }
        }

        OutlinedButton(
            onClick = { runCheck() },
            enabled = !checking && progress == null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (checking) "Sprawdzam…" else "Sprawdź aktualizacje") }

        progress?.let { p -> LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth()) }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
