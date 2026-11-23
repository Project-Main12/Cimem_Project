package com.digitalkalkan.app.data.local.db

import android.content.Context
import com.digitalkalkan.app.domain.model.AnalysisResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Analiz sonuçlarını arka planda yerel veritabanına yazar (metin olmadan). 30 günden eski kayıtlar silinir. */
object EventRecorder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val KEEP_DAYS = 30L

    fun record(context: Context, source: String, app: String, result: AnalysisResult, sentToParent: Boolean) {
        val dao = EventDatabase.get(context).events()
        scope.launch {
            runCatching {
                dao.insert(
                    EventEntity(
                        timeMillis = System.currentTimeMillis(),
                        source = source,
                        app = app,
                        category = result.category.name,
                        level = result.level.name,
                        method = result.method,
                        durationMicros = result.durationMicros,
                        confidence = result.confidence,
                        sentToParent = sentToParent
                    )
                )
                dao.deleteOlderThan(System.currentTimeMillis() - KEEP_DAYS * 24 * 3600 * 1000)
            }
        }
    }

    /** Araştırma için CSV (metin içermez). */
    suspend fun exportCsv(context: Context): String {
        val rows = EventDatabase.get(context).events().all()
        return buildString {
            appendLine("id,zaman_ms,kaynak,uygulama,kategori,seviye,yontem,sure_us,guven,veliye_gonderildi")
            rows.forEach {
                appendLine("${it.id},${it.timeMillis},${it.source},${it.app},${it.category},${it.level}," +
                    "\"${it.method}\",${it.durationMicros},${it.confidence ?: ""},${it.sentToParent}")
            }
        }
    }
}
