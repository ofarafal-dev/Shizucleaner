package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DesktopScreen(
    isolatedProfile: Boolean,
    onOpenBrowser: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
    ) {
        Text("Enklawa", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            "Tryb amnezji · klucz sesji tylko w RAM",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("Przeglądarka", "osobny profil", enabled = true, modifier = Modifier.weight(1f), onClick = onOpenBrowser)
            Tile("Notatki", "wkrótce", enabled = false, modifier = Modifier.weight(1f), onClick = {})
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("Pliki", "wkrótce", enabled = false, modifier = Modifier.weight(1f), onClick = {})
            Tile("Komunikator", "wkrótce", enabled = false, modifier = Modifier.weight(1f), onClick = {})
        }

        Spacer(Modifier.height(16.dp))
        Text(
            if (isolatedProfile) "Przeglądarka: izolowany profil WebView (usuwany przy wyjściu)."
            else "Przeglądarka: ten WebView nie obsługuje osobnych profili — używa profilu domyślnego i czyści go przy wyjściu. Zaktualizuj Android System WebView.",
            style = MaterialTheme.typography.bodySmall,
            color = if (isolatedProfile) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) { Text("Wyjdź i zniszcz świat") }
    }
}

@Composable
private fun Tile(title: String, subtitle: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .aspectRatio(1.2f)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
    ) {
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.BottomStart) {
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
