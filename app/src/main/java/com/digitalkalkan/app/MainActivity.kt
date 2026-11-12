package com.digitalkalkan.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.digitalkalkan.app.notifications.TestNotificationHelper
import com.digitalkalkan.app.permissions.NotificationAccess
import com.digitalkalkan.app.ui.theme.DijitalKalkanTheme

class MainActivity : ComponentActivity() {

    private var listenerEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TestNotificationHelper.createChannel(this)

        setContent {
            DijitalKalkanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        listenerEnabled = listenerEnabled,
                        onOpenSettings = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // Kullanıcı ayarlardan geri döndüğünde izin durumunu yenile
    override fun onResume() {
        super.onResume()
        listenerEnabled = NotificationAccess.isGranted(this)
    }
}

@Composable
fun MainScreen(
    listenerEnabled: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) TestNotificationHelper.show(context) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dijital Kalkan", style = MaterialTheme.typography.headlineMedium)

        Text(
            "Bu uygulama, izin verdiğinizde telefona gelen bildirimlerin yalnızca " +
                "görünür başlık ve metnini okur. Bu test sürümünde hiçbir veri " +
                "cihaz dışına gönderilmez. İzni istediğiniz an kapatabilirsiniz."
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (listenerEnabled) "Bildirim erişimi: AÇIK ✅"
                       else "Bildirim erişimi: KAPALI ❌",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(if (listenerEnabled) "Bildirim erişimi ayarlarını yönet"
                 else "Bildirim erişimi ver")
        }

        OutlinedButton(
            onClick = {
                val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    TestNotificationHelper.show(context)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kendi test bildirimimi gönder")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    DijitalKalkanTheme { MainScreen(listenerEnabled = false, onOpenSettings = {}) }
}
