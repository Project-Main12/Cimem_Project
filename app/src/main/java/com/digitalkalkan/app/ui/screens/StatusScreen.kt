package com.digitalkalkan.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.digitalkalkan.app.monitoring.notification.CapturedNotification
import com.digitalkalkan.app.monitoring.notification.CapturedNotificationLog
import com.digitalkalkan.app.notifications.TestNotificationHelper
import com.digitalkalkan.app.ui.components.RiskChip

@Composable
fun StatusScreen(
    listenerEnabled: Boolean,
    onOpenSettings: () -> Unit,
    captured: List<CapturedNotification>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) TestNotificationHelper.show(context) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "İzin verdiğinizde gelen bildirimlerin görünür metni bu telefonda analiz edilir. " +
                "Hiçbir veri cihaz dışına gönderilmez.",
            style = MaterialTheme.typography.bodyMedium
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (listenerEnabled) "Bildirim erişimi: AÇIK ✅" else "Bildirim erişimi: KAPALI ❌",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(14.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Text(if (listenerEnabled) "Erişimi yönet" else "Bildirim erişimi ver")
            }
            OutlinedButton(
                onClick = {
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (needsPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else TestNotificationHelper.show(context)
                },
                modifier = Modifier.weight(1f)
            ) { Text("Test bildirimi") }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Yakalanan bildirimler (${captured.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClear) { Text("Temizle") }
        }
        if (captured.isEmpty()) {
            Text("Henüz bildirim yok. Başka bir telefondan bu telefona mesaj gönderin.",
                style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(captured) { item ->
                CapturedRow(item)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CapturedRow(item: CapturedNotification) {
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${item.time}  •  ${item.packageName}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            item.result?.let {
                Spacer(Modifier.width(6.dp))
                RiskChip(it.category)
            }
        }
        if (item.skippedReason != null) {
            Text(item.skippedReason, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary)
        } else {
            if (item.title.isNotBlank()) Text("Başlık: ${item.title}", style = MaterialTheme.typography.bodyMedium)
            Text("Metin: ${item.text.ifBlank { "(boş / gizli)" }}", style = MaterialTheme.typography.bodyMedium)
            item.result?.takeIf { it.isRisky }?.let { r ->
                Text(
                    "Eşleşen: " + r.matches.joinToString { it.rule },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
