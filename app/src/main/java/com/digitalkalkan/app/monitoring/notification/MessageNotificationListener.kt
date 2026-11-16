package com.digitalkalkan.app.monitoring.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.digitalkalkan.app.domain.alerts.AlertSender
import com.digitalkalkan.app.domain.analysis.AnalyzerProvider

/**
 * Dijital Kalkan - Bildirim dinleyici servisi
 * Aşama 1: Bildirimi algıla, gönderen uygulamayı ve görünür metni al.
 * Aşama 2: Metni cihaz üzerindeki Türkçe analiz motoruna gönder (Yöntem 1: anahtar kelime).
 * Aşama 3: Bağlıysa veliye yalnızca kategori/seviye/uygulama bilgisini gönder.
 * Mesaj metni hiçbir zaman cihaz dışına çıkmaz.
 */
class MessageNotificationListener : NotificationListenerService() {

    companion object {
        const val TAG = "DK-Notif"

        // Aşama 2'den itibaren mesaj içeriği Logcat'e YAZILMAZ (gizlilik).
        // Yalnızca geliştirme sırasında, sentetik mesajlarla test için true yapılabilir.
        const val LOG_CONTENT = false
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

        // 4) Türkçe analiz (cihaz üzerinde). Başlık genelde gönderen adı olduğu için yalnızca metin analiz edilir.
        val result = if (visibleText.isBlank()) null else AnalyzerProvider.get(this).analyze(visibleText)

        if (LOG_CONTENT) {
            Log.i(TAG, "BİLDİRİM | paket=$sourcePackage | başlık=\"$title\" | metin=\"$visibleText\" | sonuç=${result?.category}")
        } else {
            Log.i(
                TAG,
                "BİLDİRİM | paket=$sourcePackage | kategori=${result?.category ?: "-"} | " +
                    "eşleşme=${result?.matches?.size ?: 0} | süre=${result?.durationMicros ?: 0} µs"
            )
        }

        // Test için uygulama ekranındaki listeye ekle (yalnızca bellekte)
        CapturedNotificationLog.add(sourcePackage, title, visibleText, result = result)

        // 5) Cihaz bir veliye bağlıysa: yalnızca kategori/seviye/uygulama gönderilir
        if (result != null) AlertSender.maybeSend(this, sourcePackage, result)
    }
}
