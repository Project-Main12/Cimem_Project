package com.digitalkalkan.app.monitoring.notification

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ekranda gösterilecek tek bir yakalanan bildirim kaydı. */
data class CapturedNotification(
    val time: String,
    val packageName: String,
    val title: String,
    val text: String,
    val skippedReason: String? = null   // null değilse analiz dışı bırakıldı
)

/**
 * Test amaçlı geçici liste: son 30 bildirim YALNIZCA BELLEKTE tutulur.
 * Diske yazılmaz, internete gönderilmez; uygulama kapanınca silinir.
 */
object CapturedNotificationLog {
    private const val MAX = 30
    private val formatter = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("tr-TR"))

    val items = mutableStateListOf<CapturedNotification>()

    fun add(packageName: String, title: String, text: String, skippedReason: String? = null) {
        items.add(0, CapturedNotification(formatter.format(Date()), packageName, title, text, skippedReason))
        while (items.size > MAX) items.removeAt(items.lastIndex)
    }

    fun clear() = items.clear()
}
