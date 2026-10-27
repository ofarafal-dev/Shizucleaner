package dev.ofarafal.enklawa.ui

import android.os.SystemClock
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun formatStopwatch(ms: Long): String {
    val total = ms.coerceAtLeast(0)
    val minutes = total / 60_000
    val seconds = (total / 1000) % 60
    val tenths = (total / 100) % 10
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}

@Composable
fun ClockApp(@Suppress("UNUSED_PARAMETER") env: AppEnv) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var running by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var baseElapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(running) {
        while (true) {
            now = LocalDateTime.now()
            if (running) elapsed = baseElapsed + (SystemClock.elapsedRealtime() - startedAt)
            delay(if (running) 50 else 500)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")), fontSize = 44.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Stoper", color = Xp.Link, modifier = Modifier.padding(top = 12.dp))
        Text(formatStopwatch(elapsed), fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurface)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (running) {
                        baseElapsed = elapsed
                        running = false
                    } else {
                        startedAt = SystemClock.elapsedRealtime()
                        running = true
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text(if (running) "Stop" else "Start") }
            OutlinedButton(
                onClick = { running = false; elapsed = 0; baseElapsed = 0 },
                modifier = Modifier.weight(1f),
            ) { Text("Zeruj") }
        }
    }
}
