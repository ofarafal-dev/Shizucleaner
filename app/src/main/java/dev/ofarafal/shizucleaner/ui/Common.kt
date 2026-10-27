package dev.ofarafal.shizucleaner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ofarafal.shizucleaner.shizuku.ShizukuState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = -1
    do {
        v /= 1024
        i++
    } while (v >= 1024 && i < units.lastIndex)
    return String.format(Locale.getDefault(), if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
}

fun formatDate(ms: Long): String =
    if (ms <= 0) "" else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ms))

fun passesText(p: Int): String = when (p) {
    0 -> "bez nadpisywania (zwykłe kasowanie)"
    1 -> "z jednym nadpisaniem losowymi danymi"
    else -> "z $p nadpisaniami losowymi danymi"
}

const val FLASH_NOTE =
    "Uwaga: na pamięci flash nadpisanie nie gwarantuje fizycznego usunięcia starych bloków. " +
        "Realną ochroną jest szyfrowanie telefonu; nadpisanie to dodatkowa warstwa."

@Composable
fun ShizukuBanner(state: ShizukuState, onRequest: () -> Unit, onRetry: () -> Unit) {
    val text: String
    val label: String?
    val action: (() -> Unit)?
    when (state) {
        ShizukuState.READY -> return
        ShizukuState.NOT_RUNNING -> {
            text = "Shizuku nie działa. Uruchom aplikację Shizuku (Wireless debugging lub adb) i wróć tutaj."
            label = "Sprawdź ponownie"
            action = onRetry
        }
        ShizukuState.UNSUPPORTED -> {
            text = "Wersja Shizuku jest za stara — zaktualizuj ją."
            label = "Sprawdź ponownie"
            action = onRetry
        }
        ShizukuState.NO_PERMISSION -> {
            text = "Brak uprawnienia Shizuku dla tej aplikacji. Jeśli prośba się nie pojawia, nadaj je w aplikacji Shizuku."
            label = "Nadaj uprawnienie"
            action = onRequest
        }
        ShizukuState.CONNECTING -> {
            text = "Łączenie z usługą Shizuku…"
            label = null
            action = null
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (label != null && action != null) {
                TextButton(onClick = action) { Text(label) }
            }
        }
    }
}

@Composable
fun ActionBar(label: String, enabled: Boolean, progress: Pair<Int, Int>?, onClick: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            progress?.let { (done, total) ->
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done / total.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
    }
}

@Composable
fun BusyBox(text: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(text, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Hint(text: String) {
    Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun SelectRow(title: String, subtitle: String?, trailing: String?, checked: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        leadingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle() }) },
        headlineContent = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = subtitle?.let {
            { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis) }
        },
        trailingContent = trailing?.let { { Text(it, style = MaterialTheme.typography.labelMedium) } },
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    body: String,
    passes: Int,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Tryb: ${passesText(passes)}. Operacji nie da się cofnąć.",
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                if (passes > 0) {
                    Text(FLASH_NOTE, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@Composable
fun ReportDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(text) } },
    )
}
