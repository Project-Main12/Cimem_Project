package com.digitalkalkan.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.digitalkalkan.app.data.repository.UserEntry
import com.digitalkalkan.app.data.repository.UserLexiconRepository
import com.digitalkalkan.app.domain.analysis.AnalyzerProvider
import com.digitalkalkan.app.monitoring.notification.CapturedNotificationLog
import com.digitalkalkan.app.notifications.TestNotificationHelper
import com.digitalkalkan.app.permissions.NotificationAccess
import com.digitalkalkan.app.ui.DijitalKalkanApp
import com.digitalkalkan.app.ui.theme.DijitalKalkanTheme

class MainActivity : ComponentActivity() {

    private var listenerEnabled by mutableStateOf(false)
    private val userEntries = mutableStateListOf<UserEntry>()
    private lateinit var userLexicon: UserLexiconRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TestNotificationHelper.createChannel(this)
        userLexicon = UserLexiconRepository(this)
        userEntries.addAll(userLexicon.load())

        setContent {
            DijitalKalkanTheme {
                DijitalKalkanApp(
                    listenerEnabled = listenerEnabled,
                    onOpenSettings = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    captured = CapturedNotificationLog.items,
                    onClearCaptured = { CapturedNotificationLog.clear() },
                    analyze = { AnalyzerProvider.get(this).analyze(it) },
                    userEntries = userEntries,
                    onAddEntry = { phrase, category, suffixes ->
                        userLexicon.add(phrase, category, suffixes).also { error ->
                            if (error == null) refreshUserLexicon()
                        }
                    },
                    onRemoveEntry = { entry ->
                        userLexicon.remove(entry)
                        refreshUserLexicon()
                    },
                    onShareEntries = { shareUserLexicon() }
                )
            }
        }
    }

    // Kullanıcı ayarlardan geri döndüğünde izin durumunu yenile
    override fun onResume() {
        super.onResume()
        listenerEnabled = NotificationAccess.isGranted(this)
    }

    private fun refreshUserLexicon() {
        userEntries.clear()
        userEntries.addAll(userLexicon.load())
        AnalyzerProvider.reload(this)   // bildirim servisi de yeni listeyi kullanır
    }

    /** Listeyi düz metin olarak paylaş (ör. öğretmene e-posta/WhatsApp ile). */
    private fun shareUserLexicon() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Dijital Kalkan – kelime listem")
            putExtra(Intent.EXTRA_TEXT, UserLexiconRepository.format(userEntries))
        }
        startActivity(Intent.createChooser(send, "Listeyi gönder"))
    }
}
