package com.digitalkalkan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digitalkalkan.app.domain.model.AnalysisResult
import com.digitalkalkan.app.domain.model.RuleSource
import com.digitalkalkan.app.ui.components.RiskChip
import java.util.Locale

/** Bildirim beklemeden bir cümle yazıp analiz sonucunu görme ekranı. */
@Composable
fun TryTextScreen(
    analyzeAll: (String) -> List<AnalysisResult>,
    modifier: Modifier = Modifier,
    initialText: String = "",
    initialResults: List<AnalysisResult> = emptyList()
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    var results by remember { mutableStateOf(initialResults) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Bir cümle yazın ve analiz motorunun ne dediğini görün. Yazdığınız metin kaydedilmez.",
            style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Cümle") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Button(
            onClick = { results = analyzeAll(text) },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Analiz et") }

        if (results.size > 1) Text("İlk kart seçili yöntemdir; diğerleri karşılaştırma içindir.",
            style = MaterialTheme.typography.bodySmall)
        results.forEach { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sonuç:  ", style = MaterialTheme.typography.titleMedium)
                        RiskChip(r.category)
                    }
                    r.confidence?.let {
                        Text("Model güveni: %${(it * 100).toInt()}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (r.matches.isEmpty()) {
                        if (r.scores.isEmpty()) Text("Hiçbir kurala uymadı.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Eşleşen kurallar:", style = MaterialTheme.typography.bodyMedium)
                        r.matches.forEach { m ->
                            val kaynak = when (m.source) {
                                RuleSource.KULLANICI -> "  (sizin listeniz)"
                                RuleSource.VELI -> "  (velinizin listesi)"
                                else -> ""
                            }
                            Text("• ${m.category.label}: ${m.rule}$kaynak", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(
                        "Yöntem: ${r.method} · Süre: ${String.format(Locale.US, "%.2f", r.durationMicros / 1000.0)} ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
