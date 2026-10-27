package dev.ofarafal.enklawa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ofarafal.enklawa.os.Calc

private val Keys = listOf(
    listOf("C", "(", ")", "÷"),
    listOf("7", "8", "9", "×"),
    listOf("4", "5", "6", "−"),
    listOf("1", "2", "3", "+"),
    listOf("0", ".", "⌫", "="),
)

@Composable
fun CalcApp(@Suppress("UNUSED_PARAMETER") env: AppEnv) {
    var expr by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }

    fun press(k: String) {
        when (k) {
            "C" -> { expr = ""; result = null }
            "⌫" -> { expr = expr.dropLast(1); result = null }
            "=" -> Calc.eval(expr)?.let { result = Calc.format(it) } ?: run { result = "błąd" }
            else -> {
                // Po wyniku wpisanie cyfry zaczyna od nowa, a operator kontynuuje działanie.
                val prev = result
                if (prev != null && prev != "błąd") {
                    expr = if (k.first().isDigit() || k == "." || k == "(") k else prev + k
                } else {
                    expr += k
                }
                result = null
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C2530)).padding(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                expr.ifEmpty { " " },
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
            Text(
                result ?: " ",
                fontSize = 32.sp,
                color = if (result == "błąd") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }
        Keys.forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    val accent = k == "=" || k in listOf("÷", "×", "−", "+")
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (k == "=") Xp.Accent else if (accent) Color(0xFF2A3A4D) else Color(0xFF1C2530))
                            .clickable { press(k) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            k,
                            fontSize = 20.sp,
                            color = if (k == "=") Color(0xFF06231E) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
