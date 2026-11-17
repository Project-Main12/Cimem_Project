package com.digitalkalkan.app.domain.alerts

import android.content.Context
import android.util.Log
import com.digitalkalkan.app.data.local.AppPrefs
import com.digitalkalkan.app.data.local.AppRole
import com.digitalkalkan.app.data.remote.Backend
import com.digitalkalkan.app.domain.model.AnalysisResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Çocuk cihazında riskli içerik bulunduğunda veliye SINIRLI bilgi gönderir:
 * kategori, seviye, uygulama paket adı. Mesaj metni, gönderen kişi GÖNDERİLMEZ.
 */
object AlertSender {
    private const val TAG = "DK-Alert"
    private const val DEDUPE_MS = 60_000L   // aynı uygulama+kategori için 1 dakikada en fazla 1 uyarı

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastSent = ConcurrentHashMap<String, Long>()

    /** Veliye gönderim başlatıldıysa true döner. */
    fun maybeSend(context: Context, sourcePackage: String, result: AnalysisResult): Boolean {
        if (!result.isRisky) return false
        val prefs = AppPrefs(context)
        if (prefs.role != AppRole.COCUK || !prefs.childLinked) return false

        val key = "$sourcePackage|${result.category.name}"
        val now = System.currentTimeMillis()
        val previous = lastSent[key]
        if (previous != null && now - previous < DEDUPE_MS) return false
        lastSent[key] = now

        scope.launch {
            try {
                Backend.awaitSession()
                val reply = Backend.sendAlert(result.category.name, result.level.name, sourcePackage)
                Log.i(TAG, "Veliye uyarı kaydedildi: ${result.category} / $sourcePackage | bildirim: $reply")
            } catch (e: Exception) {
                lastSent.remove(key)
                Log.w(TAG, "Uyarı gönderilemedi: ${e.message}")
            }
        }
        return true
    }
}
