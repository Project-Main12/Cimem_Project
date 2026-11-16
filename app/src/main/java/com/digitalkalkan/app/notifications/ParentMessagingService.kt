package com.digitalkalkan.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.digitalkalkan.app.MainActivity
import com.digitalkalkan.app.data.local.AppPrefs
import com.digitalkalkan.app.data.local.AppRole
import com.digitalkalkan.app.data.remote.Backend
import com.digitalkalkan.app.domain.model.AppNames
import com.digitalkalkan.app.domain.model.RiskCategory
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Veli telefonu: Firebase üzerinden gelen risk uyarısını bildirim olarak gösterir.
 * Gelen veride yalnızca kategori, seviye, uygulama ve çocuk adı vardır.
 */
class ParentMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        if (AppPrefs(this).role == AppRole.VELI) registerToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (AppPrefs(this).role != AppRole.VELI) return
        val d = message.data
        val category = RiskCategory.fromKey(d["category"].orEmpty())
        show(
            this,
            title = "⚠️ ${category?.label ?: "Risk"} · ${category?.level?.label ?: ""}".trim(),
            text = listOfNotNull(d["child_label"], AppNames.label(d["source_app"].orEmpty())).joinToString(" · ")
        )
    }

    companion object {
        private const val CHANNEL_ID = "dk_veli_uyarilari"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Çocuk cihazı uyarıları", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Çocuğunuzun cihazında tespit edilen risk uyarıları" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        /** Veli oturum açınca çağrılır: bu telefonun bildirim adresini sunucuya kaydeder. */
        fun refreshToken() {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { registerToken(it) }
        }

        private fun registerToken(token: String) {
            scope.launch {
                runCatching {
                    Backend.awaitSession()
                    Backend.savePushToken(token)
                }
            }
        }

        fun show(context: Context, title: String, text: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
            createChannel(context)
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), n)
        }
    }
}
