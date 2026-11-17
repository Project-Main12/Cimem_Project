package com.digitalkalkan.app.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalkalkan.app.data.remote.AlertRow
import com.digitalkalkan.app.data.remote.FamilyLink
import com.digitalkalkan.app.data.repository.UserEntry
import com.digitalkalkan.app.domain.model.AlertText
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import java.time.OffsetDateTime
import com.digitalkalkan.app.domain.model.AppNames
import com.digitalkalkan.app.domain.model.RiskCategory
import com.digitalkalkan.app.ui.components.RiskChip
import com.digitalkalkan.app.ui.components.formatServerTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Veli tarafının işlemleri (MainActivity'de gerçek Backend'e bağlanır). */
interface ParentActions {
    fun isSignedIn(): Boolean
    fun email(): String?
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun signOut()
    suspend fun createCode(): String
    suspend fun links(): List<FamilyLink>
    suspend fun alerts(): List<AlertRow>
    suspend fun markRead(alert: AlertRow)
    suspend fun removeLink(link: FamilyLink)
    /** Bu telefondan o çocuğa gönderilmiş ifadeler (yalnızca veli telefonunda kayıtlı). */
    fun sentWords(childId: String): List<UserEntry>
    /** İfadeyi çocuğun telefonuna gönderir; kullanıcıya gösterilecek sonuç mesajını döndürür. */
    suspend fun sendWord(childId: String, phrase: String, category: RiskCategory, includeSuffixes: Boolean): String
    suspend fun removeWord(childId: String, entry: UserEntry): String
}

@Composable
fun ParentApp(actions: ParentActions, onChangeRole: () -> Unit) {
    var signedIn by remember { mutableStateOf(actions.isSignedIn()) }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            if (signedIn) ParentHome(actions, onSignedOut = { signedIn = false })
            else ParentAuth(actions, onSignedIn = { signedIn = true }, onChangeRole = onChangeRole)
        }
    }
}

@Composable
private fun ParentAuth(actions: ParentActions, onSignedIn: () -> Unit, onChangeRole: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Dijital Kalkan · Veli", style = MaterialTheme.typography.headlineSmall)
        Text(if (register) "Veli hesabı oluşturun" else "Veli hesabınızla giriş yapın",
            style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("E-posta") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Şifre (en az 6 karakter)") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                busy = true; error = null
                scope.launch {
                    try {
                        if (register) actions.signUp(email, password) else actions.signIn(email, password)
                        onSignedIn()
                    } catch (e: Exception) {
                        error = e.message
                    }
                    busy = false
                }
            },
            enabled = !busy && email.contains("@") && password.length >= 6,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Bekleyin…" else if (register) "Kayıt ol" else "Giriş yap") }
        TextButton(onClick = { register = !register; error = null }) {
            Text(if (register) "Zaten hesabım var: giriş yap" else "Hesabım yok: kayıt ol")
        }
        TextButton(onClick = onChangeRole) { Text("Bu telefon çocuk telefonu (rolü değiştir)") }
    }
}

@Composable
private fun ParentHome(actions: ParentActions, onSignedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    var links by remember { mutableStateOf<List<FamilyLink>>(emptyList()) }
    var alerts by remember { mutableStateOf<List<AlertRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf<String?>(null) }
    var secondsLeft by remember { mutableIntStateOf(0) }

    suspend fun refresh() {
        loading = true; error = null
        try {
            links = actions.links()
            alerts = actions.alerts()
        } catch (e: Exception) { error = e.message }
        loading = false
    }
    LaunchedEffect(Unit) {
        refresh()
        while (true) {                       // ekran açıkken 30 sn'de bir sessizce yenile
            delay(30_000)
            runCatching { alerts = actions.alerts() }
        }
    }
    LaunchedEffect(code) {
        if (code == null) return@LaunchedEffect
        secondsLeft = 600
        while (secondsLeft > 0) {
            delay(1000); secondsLeft--
            if (secondsLeft % 5 == 0) {           // çocuk bağlandı mı? 5 sn'de bir kontrol
                val before = links.size
                runCatching { links = actions.links() }
                if (links.size > before) { code = null; break }
            }
        }
        if (secondsLeft == 0) code = null
    }

    val labels = links.associate { it.childId to it.childLabel }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Dijital Kalkan · Veli", style = MaterialTheme.typography.headlineSmall)
                    Text(actions.email().orEmpty(), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { scope.launch { runCatching { actions.signOut() }; onSignedOut() } }) {
                    Text("Çıkış")
                }
            }
        }
        item {
            val current = code
            if (current == null) {
                Button(
                    onClick = { scope.launch { runCatching { code = actions.createCode() }.onFailure { error = it.message } } },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("➕  Çocuk cihazı ekle") }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Çocuğun telefonuna bu kodu girin", style = MaterialTheme.typography.titleMedium)
                        Text(
                            current.chunked(3).joinToString(" "),
                            fontSize = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                        )
                        Text("Kalan süre: %d:%02d".format(secondsLeft / 60, secondsLeft % 60))
                        TextButton(onClick = { code = null }) { Text("İptal") }
                    }
                }
            }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Text("Bağlı cihazlar (${links.size})", style = MaterialTheme.typography.titleMedium) }
        if (links.isEmpty() && !loading) item { Text("Henüz bağlı cihaz yok.") }
        items(links, key = { it.id }) { l ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🧒 ${l.childLabel}", fontWeight = FontWeight.Bold)
                    Text("Bağlanma: ${formatServerTime(l.createdAt)}", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { scope.launch { runCatching { actions.removeLink(l) }; refresh() } }) {
                    Text("Kaldır")
                }
            }
        }
        item { HorizontalDivider() }
        item { WeeklySummary(alerts) }
        if (links.isNotEmpty()) item { HorizontalDivider() }
        if (links.isNotEmpty()) item { WordsSection(actions, links) }
        item { HorizontalDivider() }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Uyarılar (${alerts.count { it.readAt == null }} yeni)",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (loading) CircularProgressIndicator(Modifier.padding(4.dp))
                else OutlinedButton(onClick = { scope.launch { refresh() } }) { Text("Yenile") }
            }
        }
        item {
            Text("Mesajların kendisi gösterilmez; yalnızca risk türü, uygulama ve saat. " +
                "Bir uyarı görürseniz çocuğunuzla sakin bir şekilde konuşmanız önerilir.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (alerts.isEmpty() && !loading) item { Text("Henüz uyarı yok.") }
        items(alerts, key = { it.id }) { a ->
            val unread = a.readAt == null
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (unread) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val cat = RiskCategory.fromKey(a.category)
                        cat?.let { RiskChip(it) }
                        Text("${labels[a.childId] ?: "Çocuk"} · ${AppNames.label(a.sourceApp)}", fontWeight = FontWeight.Bold)
                        Text(AlertText.sentence(cat), style = MaterialTheme.typography.bodyMedium)
                        Text(formatServerTime(a.detectedAt), style = MaterialTheme.typography.bodySmall)
                    }
                    if (unread) TextButton(onClick = { scope.launch { runCatching { actions.markRead(a) }; refresh() } }) {
                        Text("Okundu")
                    }
                }
            }
        }
        item { Text(" ") }
    }
}


/** Son 7 günün kategori bazında özeti (haftalık rapor). */
@Composable
private fun WeeklySummary(alerts: List<AlertRow>) {
    val since = OffsetDateTime.now().minusDays(7)
    val week = alerts.filter { runCatching { OffsetDateTime.parse(it.detectedAt).isAfter(since) }.getOrDefault(false) }
    val counts = week.groupingBy { it.category }.eachCount()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Son 7 gün", style = MaterialTheme.typography.titleMedium)
            if (week.isEmpty()) {
                Text("Uygunsuz mesaj tespit edilmedi. 👍")
            } else {
                Text("Toplam ${week.size} uyarı", fontWeight = FontWeight.Bold)
                RiskCategory.selectable.sortedByDescending { it.priority }.forEach { c ->
                    val n = counts[c.name] ?: 0
                    if (n > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                        RiskChip(c, showLevel = false)
                        Text("  $n kez")
                    }
                }
                val apps = week.groupingBy { it.sourceApp }.eachCount().entries.sortedByDescending { it.value }.take(3)
                Text("En çok: " + apps.joinToString { "${AppNames.label(it.key)} (${it.value})" },
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Veli, çocuğun telefonundaki analize lakap/özel ifade ekler. İfade sunucuda saklanmaz. */
@Composable
private fun WordsSection(actions: ParentActions, links: List<FamilyLink>) {
    val scope = rememberCoroutineScope()
    var childId by rememberSaveable { mutableStateOf(links.first().childId) }
    if (links.none { it.childId == childId }) childId = links.first().childId
    var phrase by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(RiskCategory.HAKARET) }
    var suffixes by rememberSaveable { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var version by remember { mutableIntStateOf(0) }
    val sent = remember(childId, version) { actions.sentWords(childId) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Kelime listesi", style = MaterialTheme.typography.titleMedium)
        Text("Çocuğunuza takılan lakapları veya özel ifadeleri ekleyin. İfade yalnızca çocuğunuzun " +
            "telefonuna iletilir ve orada saklanır; internette tutulmaz.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (links.size > 1) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                links.forEach { l ->
                    FilterChip(selected = childId == l.childId, onClick = { childId = l.childId },
                        label = { Text(l.childLabel) })
                }
            }
        }
        OutlinedTextField(
            value = phrase, onValueChange = { phrase = it.take(60); message = null },
            label = { Text("Kelime veya ifade (ör. koca kafa)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RiskCategory.selectable.forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = suffixes, onCheckedChange = { suffixes = it })
            Text("Ekli hâllerini de yakala", style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = {
                busy = true
                scope.launch {
                    message = runCatching { actions.sendWord(childId, phrase, category, suffixes) }
                        .getOrElse { it.message ?: "Hata" }
                    if (message?.startsWith("✅") == true) phrase = ""
                    version++
                    busy = false
                }
            },
            enabled = !busy && phrase.trim().length >= 3,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Gönderiliyor…" else "Çocuğun telefonuna ekle") }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        sent.forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.phrase, fontWeight = FontWeight.Bold)
                    if (e.includeSuffixes) Text("ekli hâlleri de", style = MaterialTheme.typography.bodySmall)
                }
                RiskChip(e.category, showLevel = false)
                TextButton(onClick = {
                    scope.launch {
                        message = runCatching { actions.removeWord(childId, e) }.getOrElse { it.message ?: "Hata" }
                        version++
                    }
                }) { Text("Kaldır") }
            }
        }
    }
}
