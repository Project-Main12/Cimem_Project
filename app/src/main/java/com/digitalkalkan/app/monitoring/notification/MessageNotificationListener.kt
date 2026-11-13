package com.digitalkalkan.app.monitoring.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Dijital Kalkan - Bildirim dinleyici servisi (Aşama 1)
 * Görev: Gelen bildirimi algılamak, gönderen uygulamayı ve görünür metni almak.
 * Şimdilik yalnızca Logcat'e yazar. Hiçbir veri cihaz dışına çıkmaz.
 */
class MessageNotificationListener : NotificationListenerService() {

    companion object {
        const val TAG = "DK-Notif"

        // UYARI: Yalnızca geliştirme testi içindir. Gerçek kişilerin mesajlarıyla
        // test yapmayın; ekip içi test mesajları kullanın. Sonraki aşamada false yapılacak.
        const val LOG_CONTENT = true
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Servis bağlandı: bildirim erişimi aktif.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Servis bağlantısı kesildi (izin kaldırılmış olabilir).")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val sourcePackage = sbn.packageName

        // 1) Kendi uygulamamızın bildirimlerini analiz dışı tut
        if (sourcePackage == packageName) {
            Log.d(TAG, "Kendi bildirimimiz, analiz dışı bırakıldı.")
            CapturedNotificationLog.add(sourcePackage, "", "", "Kendi bildirimimiz – analiz dışı")
            return
        }

        val notification = sbn.notification

        // 2) Grup özet bildirimlerini atla (ör. "3 yeni mesaj"); asıl mesajlar ayrı gelir
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            Log.d(TAG, "Grup özeti atlandı: $sourcePackage")
            CapturedNotificationLog.add(sourcePackage, "", "", "Grup özeti – atlandı")
            return
        }

        // 3) Görünür başlık ve metni al
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val visibleText = bigText.ifBlank { text }

        val isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0

        if (LOG_CONTENT) {
            Log.i(
                TAG,
                "YENİ BİLDİRİM | paket=$sourcePackage | süregelen=$isOngoing | " +
                    "başlık=\"$title\" | metin=\"$visibleText\""
            )
        } else {
            Log.i(TAG, "YENİ BİLDİRİM | paket=$sourcePackage | metin uzunluğu=${visibleText.length}")
        }

        // Test için uygulama ekranındaki listeye ekle (yalnızca bellekte)
        CapturedNotificationLog.add(sourcePackage, title, visibleText)

        // Sonraki aşama: visibleText -> metin temizleme -> Türkçe analiz motoru
    }
}
