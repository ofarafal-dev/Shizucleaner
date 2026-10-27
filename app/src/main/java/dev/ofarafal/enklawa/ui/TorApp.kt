package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ofarafal.enklawa.os.WebApps
import dev.ofarafal.enklawa.tor.TorManager

@Composable
fun TorApp(env: AppEnv) {
    val ctx = LocalContext.current
    val st = TorManager.state
    val statusText = when (st) {
        TorManager.State.Off -> "Wyłączony"
        TorManager.State.Starting -> "Uruchamianie…"
        is TorManager.State.Running -> "Tor działa (SOCKS 127.0.0.1:${st.socksPort}); pierwsze połączenie może potrwać, gdy budują się obwody"
        is TorManager.State.Failed -> "Błąd: ${st.message}"
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Stan: $statusText", color = MaterialTheme.colorScheme.onSurface)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { if (TorManager.isActive) TorManager.stop(ctx) else TorManager.start(ctx) },
                modifier = Modifier.weight(1f),
            ) { Text(if (TorManager.isActive) "Zatrzymaj Tora" else "Uruchom Tora") }
            OutlinedButton(
                enabled = TorManager.routingApplied,
                onClick = { env.open(WebApps.idFor("https://check.torproject.org")) },
                modifier = Modifier.weight(1f),
            ) { Text("Sprawdź połączenie") }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Przeglądarka i aplikacje web przez Tor", color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (TorManager.routingApplied) "Włączone: ruch WebView idzie przez Tor."
                    else if (TorManager.routingWanted) "Czekam, aż Tor poda port…"
                    else "Wyłączone: przeglądarka łączy się bezpośrednio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = TorManager.routingWanted, onCheckedChange = { TorManager.setRouting(ctx, it) })
        }

        Text("Czego to NIE daje", color = Xp.Link)
        Text(
            "• To systemowy WebView przez Tor, a nie Tor Browser: strony mogą rozpoznać Cię po odcisku przeglądarki, a nie tylko po IP.\n" +
                "• Gdy Tor padnie przy włączonym przekierowaniu, przeglądarka przestaje łączyć się z internetem (nie wraca na łącze bezpośrednie).\n" +
                "• Przekierowanie dotyczy WebView. Sprawdzanie aktualizacji z ekranu wejścia idzie poza Torem.\n" +
                "• Strony otwarte przed włączeniem przekierowania mogły już połączyć się bezpośrednio — otwórz je od nowa.\n" +
                "• Tor działa, dopóki aplikacja jest na wierzchu; system może go zatrzymać w tle.\n" +
                "• Przy wyjściu ze świata stan Tora jest usuwany z danych aplikacji.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
