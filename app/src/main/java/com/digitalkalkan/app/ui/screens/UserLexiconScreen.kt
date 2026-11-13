package com.digitalkalkan.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalkalkan.app.data.repository.UserEntry
import com.digitalkalkan.app.domain.model.RiskCategory
import com.digitalkalkan.app.ui.components.RiskChip

/**
 * Kullanıcının kendi kelime listesi: çocuğa takılan lakaplar, sınıfa/oyuna özgü ifadeler.
 * Liste yalnızca bu telefonda saklanır.
 */
@Composable
fun UserLexiconScreen(
    entries: List<UserEntry>,
    onAdd: (phrase: String, category: RiskCategory, includeSuffixes: Boolean) -> String?,
    onRemove: (UserEntry) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phrase by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(RiskCategory.HAKARET) }
    var suffixes by rememberSaveable { mutableStateOf(true) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(
                "Çocuğunuza takılan lakapları veya sınıfta/oyunda kullanılan özel ifadeleri ekleyin. " +
                    "Bu liste yalnızca bu telefonda saklanır ve hemen analize dahil olur.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }

        OutlinedTextField(
            value = phrase,
            onValueChange = { phrase = it; message = null },
            label = { Text("Kelime veya ifade (ör. koca kafa)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Kategori", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RiskCategory.selectable.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text(c.label) }
                )
            }
        }
        Text("Lakaplar için genellikle \"Hakaret\" uygundur.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = suffixes, onCheckedChange = { suffixes = it })
            Text("Ekli hâllerini de yakala (ör. lakap → lakabın, lakaplı)",
                style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                val error = onAdd(phrase, category, suffixes)
                if (error == null) {
                    message = "\"${phrase.trim()}\" listeye eklendi."
                    isError = false
                    phrase = ""
                } else {
                    message = error
                    isError = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Listeye ekle") }

        message?.let {
            Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium)
        }

        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Listem (${entries.size})", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onShare, enabled = entries.isNotEmpty()) { Text("Öğretmene gönder") }
        }
        if (entries.isEmpty()) {
            Text("Henüz kelime eklenmedi.", style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(entries) { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.phrase, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        if (e.includeSuffixes) Text("ekli hâlleri de", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RiskChip(e.category, showLevel = false)
                    TextButton(onClick = { onRemove(e) }) { Text("Sil") }
                }
                HorizontalDivider()
            }
        }
    }
}
