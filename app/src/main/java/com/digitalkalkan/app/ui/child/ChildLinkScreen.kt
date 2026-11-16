package com.digitalkalkan.app.ui.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.digitalkalkan.app.data.remote.AlertRow
import com.digitalkalkan.app.data.remote.FamilyLink
import com.digitalkalkan.app.domain.model.AppNames
import com.digitalkalkan.app.domain.model.RiskCategory
import com.digitalkalkan.app.ui.components.RiskChip
import com.digitalkalkan.app.ui.components.formatServerTime
import kotlinx.coroutines.launch

/** Çocuk cihazının bağlantı işlemleri (MainActivity'de gerçek Backend'e bağlanır). */
interface ChildLinkActions {
    suspend fun loadLink(): FamilyLink?
    suspend fun loadSentAlerts(): List<AlertRow>
    suspend fun redeem(code: String, label: String)
    suspend fun unlink(link: FamilyLink)
}

@Composable
fun ChildLinkScreen(actions: ChildLinkActions, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var link by remember { mutableStateOf<FamilyLink?>(null) }
    var alerts by remember { mutableStateOf<List<AlertRow>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        loading = true
        error = null
        try {
            link = actions.loadLink()
            alerts = if (link != null) actions.loadSentAlerts() else emptyList()
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }
    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        val current = link
        if (!loading && current == null) {
            PairingForm(onSubmit = { code, label ->
                scope.launch {
                    try {
                        actions.redeem(code, label)
                        refresh()
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            })
        }
        if (current != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Velinize bağlısınız ✅", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Bağlantı adı: ${current.childLabel}")
                    Text("Bağlanma: ${formatServerTime(current.createdAt)}")
                }
            }
            Text(
                "Velinize giden bilgi: yalnızca risk türü, uygulama adı ve saat. " +
                    "Mesajlarınızın kendisi, kiminle yazıştığınız ve kelime listeniz gönderilmez.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Velinize gönderilen uyarılar (${alerts.size})",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { scope.launch { refresh() } }) { Text("Yenile") }
            }
            if (alerts.isEmpty()) Text("Henüz uyarı gönderilmedi.")
            alerts.forEach { a ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(AppNames.label(a.sourceApp), fontWeight = FontWeight.Bold)
                        Text(formatServerTime(a.detectedAt), style = MaterialTheme.typography.bodySmall)
                    }
                    RiskCategory.fromKey(a.category)?.let { RiskChip(it) }
                }
                HorizontalDivider()
            }
            OutlinedButton(
                onClick = { scope.launch { runCatching { actions.unlink(current) }.onFailure { error = it.message }; refresh() } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Veli bağlantısını kaldır") }
        }
    }
}

@Composable
private fun PairingForm(onSubmit: (code: String, label: String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }

    Text("Velinize bağlanın", style = MaterialTheme.typography.titleLarge)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Bağlandığınızda ne olur?", fontWeight = FontWeight.Bold)
            Text("• Bildirimleriniz bu telefonda analiz edilir; mesajlarınız hiçbir yere gönderilmez.")
            Text("• Riskli bir içerik bulunursa velinize yalnızca risk türü, uygulama adı ve saat gider.")
            Text("• Velinize hangi uyarıların gittiğini bu ekranda her zaman görebilirsiniz.")
            Text("• Bağlantıyı istediğiniz zaman bu ekrandan kaldırabilirsiniz.")
        }
    }
    OutlinedTextField(
        value = label, onValueChange = { label = it.take(40) },
        label = { Text("Adınız (velinizin ekranında görünür)") },
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) },
        label = { Text("Velinizin telefonundaki 6 haneli kod") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = consent, onCheckedChange = { consent = it })
        Text("Yukarıdakileri okudum ve kabul ediyorum.")
    }
    Button(
        onClick = { onSubmit(code, label) },
        enabled = consent && code.length == 6 && label.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) { Text("Bağlan") }
}
