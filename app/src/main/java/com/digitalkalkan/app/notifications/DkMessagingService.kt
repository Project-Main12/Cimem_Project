package com.digitalkalkan.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.digitalkalkan.app.MainActivity
import com.digitalkalkan.app.data.local.AppPrefs
import com.digitalkalkan.app.data.local.AppRole
import com.digitalkalkan.app.data.remote.Backend
import com.digitalkalkan.app.data.repository.UserEntry
import com.digitalkalkan.app.data.repository.UserLexiconRepository
import com.digitalkalkan.app.domain.analysis.AnalyzerProvider
import com.digitalkalkan.app.domain.model.AlertText
import com.digitalkalkan.app.domain.model.RiskCategory
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase üzerinden gelen veri mesajları:
 *  - "uyari"       (veli telefonu): "X kategorisinde uygunsuz mesaj tespit edildi" bildirimi
 *  - "kelime_ekle" (çocuk telefonu): velinin gönderdiği ifade yerel listeye eklenir
 *  - "kelime_sil"  (çocuk telefonu): velinin gönderdiği ifade yerel listeden kaldırılır
 */
class DkMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        if (AppPrefs(this).role != null) registerToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val d = message.data
        val role = AppPrefs(this).role
        when (d["type"]) {
            "uyari" -> if (role == AppRole.VELI) {
                val category = RiskCategory.fromKey(d["category"].orEmpty())
                show(this, ALERT_CHANNEL, AlertText.TITLE,
                    AlertText.body(d["child_label"], d["source_app"].orEmpty(), category))
            }
            "kelime_ekle", "kelime_sil" -> if (role == AppRole.COCUK) {
                val phrase = d["phrase"].orEmpty()
                val category = RiskCategory.fromKey(d["category"].orEmpty()) ?: return
                if (phrase.isBlank()) return
                val repo = UserLexiconRepository(this, UserLexiconRepository.PARENT_FILE_NAME)
                if (d["type"] == "kelime_ekle") repo.upsert(UserEntry(phrase, category, d["suffixes"] == "1"))
                else repo.removePhrase(phrase)
                AnalyzerProvider.reload(this)
                Log.i(TAG, "Veli kelime listesini güncelledi (${d["type"]})")
                show(this, INFO_CHANNEL, "Dijital Kalkan",
                    "Veliniz kelime listesini güncelledi. Ayrıntı: Kelimelerim sekmesi.")
            }
        }
    }

    companion object {
        private const val TAG = "DK-Push"
        private const val ALERT_CHANNEL = "dk_veli_uyarilari"
        private const val INFO_CHANNEL = "dk_bilgi"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun createChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL, "Çocuk cihazı uyarıları", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Çocuğunuzun cihazında tespit edilen uygunsuz mesaj uyarıları" }
            )
            nm.createNotificationChannel(
                NotificationChannel(INFO_CHANNEL, "Bilgilendirme", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Kelime listesi güncellemeleri" }
            )
        }

        /** Oturum açıldığında / bağlantı kurulduğunda: bu telefonun bildirim adresini kaydeder. */
        fun refreshToken() {
            runCatching {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { registerToken(it) }
            }.onFailure { Log.w(TAG, "Firebase hazır değil: ${it.message}") }
        }

        private fun registerToken(token: String) {
            scope.launch {
                runCatching {
                    Backend.awaitSession()
                    Backend.savePushToken(token)
                }.onFailure { Log.w(TAG, "Bildirim adresi kaydedilemedi: ${it.message}") }
            }
        }

        fun show(context: Context, channel: String, title: String, text: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
            createChannels(context)
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = NotificationCompat.Builder(context, channel)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), n)
        }
    }
}
