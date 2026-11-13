package com.digitalkalkan.app.domain.analysis

import android.content.Context
import com.digitalkalkan.app.data.repository.UserLexiconRepository
import com.digitalkalkan.app.domain.model.RuleSource

/**
 * Uygulama genelinde tek bir analiz motoru tutar.
 * Ana sözlük (assets) + kullanıcının kendi listesi birlikte yüklenir.
 * Kullanıcı listesi değişince [reload] çağrılır; bildirim servisi de yeni listeyi kullanır.
 */
object AnalyzerProvider {
    const val LEXICON_ASSET = "lexicon/tr_risk_lexicon.txt"

    @Volatile
    private var cached: KeywordAnalyzer? = null

    fun get(context: Context): KeywordAnalyzer =
        cached ?: synchronized(this) {
            cached ?: build(context.applicationContext).also { cached = it }
        }

    fun reload(context: Context) {
        synchronized(this) { cached = build(context.applicationContext) }
    }

    private fun build(context: Context): KeywordAnalyzer {
        val main = context.assets.open(LEXICON_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val user = UserLexiconRepository(context).rawText()
        return KeywordAnalyzer(listOf(main to RuleSource.ANA_SOZLUK, user to RuleSource.KULLANICI))
    }
}
