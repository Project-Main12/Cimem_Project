package com.digitalkalkan.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalkalkan.app.data.local.db.CategoryCount
import com.digitalkalkan.app.data.local.db.MethodStat
import com.digitalkalkan.app.data.local.db.SourceCount
import com.digitalkalkan.app.domain.analysis.AnalysisMethod
import com.digitalkalkan.app.domain.model.RiskCategory
import com.digitalkalkan.app.ui.components.RiskChip
import kotlinx.coroutines.launch
import java.util.Locale

/** Çocuk telefonu ayarları: yöntem seçimi, ekran/ses analizi, yerel ölçümler. */
interface SettingsActions {
    fun method(): AnalysisMethod
    fun setMethod(m: AnalysisMethod)
    fun bertAvailable(): Boolean
    fun monitorRunning(): Boolean
    fun monitorStatus(): String
    /** Ekran/ses analizini başlatır (sistem onay penceresi açılır). */
    fun startMonitor(ocr: Boolean, audio: Boolean)
    fun stopMonitor()
    fun voskReady(): Boolean
    suspend fun downloadVosk(progress: (Int) -> Unit)
    suspend fun stats(): Triple<List<MethodStat>, List<CategoryCount>, List<SourceCount>>
    fun exportCsv()
    suspend fun clearEvents()
}

@Composable
fun SettingsScreen(actions: SettingsActions, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var method by remember { mutableStateOf(actions.method()) }
    var ocr by rememberSaveable { mutableStateOf(true) }
    var audio by rememberSaveable { mutableStateOf(false) }
    var voskReady by remember { mutableStateOf(actions.voskReady()) }
    var voskProgress by remember { mutableStateOf<Int?>(null) }
    var voskError by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<Triple<List<MethodStat>, List<CategoryCount>, List<SourceCount>>?>(null) }
    LaunchedEffect(Unit) { stats = runCatching { actions.stats() }.getOrNull() }

    Column(
        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Analiz yöntemi", style = MaterialTheme.typography.titleMedium)
        AnalysisMethod.entries.forEach { m ->
            val disabled = m == AnalysisMethod.YONTEM3 && !actions.bertAvailable()
            Row(
                Modifier.fillMaxWidth().clickable(enabled = !disabled) { method = m; actions.setMethod(m) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = method == m, onClick = { method = m; actions.setMethod(m) }, enabled = !disabled)
                Column {
                    Text(m.label, fontWeight = FontWeight.Bold)
                    Text(if (disabled) "Model bu sürümde yok" else m.description,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider()

        Text("Ekran ve ses analizi", style = MaterialTheme.typography.titleMedium)
        Text("YouTube, oyun ve uygulama ekranlarındaki yazılar (OCR) ve izin veren uygulamaların sesi bu " +
            "telefonda analiz edilir. Görüntü, yazı ve ses kaydedilmez. Açıkken bildirim çubuğunda sürekli görünür.",
            style = MaterialTheme.typography.bodySmall)
        Card(Modifier.fillMaxWidth()) {
            Text("Durum: ${actions.monitorStatus()}", Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ekrandaki yazılar (OCR)", Modifier.weight(1f)); Switch(checked = ocr, onCheckedChange = { ocr = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Video/oyun sesi (konuşma tanıma)", Modifier.weight(1f)); Switch(checked = audio, onCheckedChange = { audio = it })
        }
        if (audio && !voskReady) {
            Text("Ses analizi için Türkçe konuşma tanıma modeli (~35 MB) bir kez indirilmelidir.",
                style = MaterialTheme.typography.bodySmall)
            voskProgress?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth()) }
            voskError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(
                enabled = voskProgress == null,
                onClick = {
                    voskError = null; voskProgress = 0
                    scope.launch {
                        runCatching { actions.downloadVosk { p -> voskProgress = p } }
                            .onSuccess { voskReady = true }
                            .onFailure { voskError = "İndirilemedi: ${it.message}" }
                        voskProgress = null
                    }
                }
            ) { Text("Modeli indir") }
        }
        if (actions.monitorRunning()) {
            Button(onClick = { actions.stopMonitor() }, modifier = Modifier.fillMaxWidth()) { Text("Analizi durdur") }
        } else {
            Button(
                onClick = { actions.startMonitor(ocr, audio && voskReady) },
                enabled = ocr || (audio && voskReady),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Başlat (ekran paylaşım onayı istenir)") }
        }
        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ölçümler (bu telefon)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { scope.launch { stats = runCatching { actions.stats() }.getOrNull() } }) { Text("Yenile") }
        }
        Text("Araştırma için: hangi yöntem kaç kez çalıştı, ortalama analiz süresi. Metin içermez.",
            style = MaterialTheme.typography.bodySmall)
        stats?.let { (methods, cats, sources) ->
            if (methods.isEmpty()) Text("Henüz kayıt yok.")
            methods.forEach {
                Text("• ${it.method}: ${it.count} analiz, ort. ${fmt(it.avgMicros / 1000)} ms, en çok ${fmt(it.maxMicros / 1000.0)} ms")
            }
            if (sources.isNotEmpty()) Text("Son 7 gün kaynak: " + sources.joinToString { "${it.source} ${it.count}" })
            cats.forEach { c ->
                RiskCategory.fromKey(c.category)?.let { Row(verticalAlignment = Alignment.CenterVertically) { RiskChip(it, showLevel = false); Text("  ${c.count}") } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { actions.exportCsv() }) { Text("CSV dışa aktar") }
            TextButton(onClick = { scope.launch { actions.clearEvents(); stats = runCatching { actions.stats() }.getOrNull() } }) {
                Text("Kayıtları sil")
            }
        }
    }
}

private fun fmt(v: Double) = String.format(Locale.US, "%.2f", v)
