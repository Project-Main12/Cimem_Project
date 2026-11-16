package com.digitalkalkan.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalkalkan.app.data.local.AppRole

/** İlk açılış: bu telefon veli telefonu mu, çocuk telefonu mu? */
@Composable
fun RoleSelectScreen(onSelect: (AppRole) -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dijital Kalkan", style = MaterialTheme.typography.headlineMedium)
            Text("Bu telefon kimin?", style = MaterialTheme.typography.titleMedium)
            RoleCard(
                "👨‍👩‍👧  Veli telefonu",
                "Çocuğunuzun telefonunu bağlarsınız ve risk uyarılarını alırsınız. " +
                    "Mesajların kendisini görmezsiniz; yalnızca risk türü, uygulama ve saat gelir."
            ) { onSelect(AppRole.VELI) }
            RoleCard(
                "🧒  Çocuk telefonu",
                "Bu telefondaki bildirimler cihaz üzerinde analiz edilir. Veliye bağlanırsanız, " +
                    "riskli içerikte velinize yalnızca kısa bir uyarı gider. Bağlantı her zaman görünür."
            ) { onSelect(AppRole.COCUK) }
            Text(
                "Seçiminizi daha sonra ayarlardan değiştirebilirsiniz.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoleCard(title: String, body: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
