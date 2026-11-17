package com.digitalkalkan.app.monitoring.notification

import androidx.compose.runtime.mutableStateListOf
import com.digitalkalkan.app.domain.model.AnalysisResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ekranda gösterilecek tespit kaydı. GİZLİLİK: Mesaj metni ve gönderen kişi TUTULMAZ;
 * yalnızca uygulama, saat ve analiz sonucu.
 */
data class CapturedNotification(
    val time: String,
    val packageName: String,
    val skippedReason: String? = null,   // null değilse analiz dışı bırakıldı
    val result: AnalysisResult? = null,
    val sentToParent: Boolean = false
)

/** Son 30 tespit YALNIZCA BELLEKTE tutulur; diske yazılmaz, uygulama kapanınca silinir. */
object CapturedNotificationLog {
    private const val MAX = 30
    private val formatter = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("tr-TR"))

    val items = mutableStateListOf<CapturedNotification>()

    fun add(
        packageName: String,
        skippedReason: String? = null,
        result: AnalysisResult? = null,
        sentToParent: Boolean = false
    ) {
        items.add(0, CapturedNotification(formatter.format(Date()), packageName, skippedReason, result, sentToParent))
        while (items.size > MAX) items.removeAt(items.lastIndex)
    }

    fun clear() = items.clear()
}
