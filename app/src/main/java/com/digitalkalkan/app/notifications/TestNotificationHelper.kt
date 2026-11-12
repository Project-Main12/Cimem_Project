package com.digitalkalkan.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** "Kendi bildirimini analiz dışı tutma" kuralını test etmek için yardımcı. */
object TestNotificationHelper {
    private const val CHANNEL_ID = "dk_test_channel"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Dijital Kalkan Test", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Geliştirme testleri için bildirim kanalı" }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Dijital Kalkan test bildirimi")
            .setContentText("Bu bildirim analiz dışı kalmalıdır.")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}
