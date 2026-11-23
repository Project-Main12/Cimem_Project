package com.digitalkalkan.app.domain.analysis

import android.content.Context
import android.util.Log
import com.digitalkalkan.app.data.local.AppPrefs
import com.digitalkalkan.app.data.repository.UserLexiconRepository
import com.digitalkalkan.app.domain.model.RuleSource

/**
 * Uygulama genelinde analiz motorları.
 * - Kelime listesi: ana sözlük (assets) + çocuğun listesi + velinin gönderdiği liste
 * - Yöntem 2: assets/models/yontem2_tfidf_lr.json
 * - Yöntem 3: assets/models/yontem3_distilberturk.tflite + vocab (varsa)
 * Seçili yöntem [AppPrefs.analysisMethod] ile belirlenir; bildirim, ekran ve ses aynı motoru kullanır.
 */
object AnalyzerProvider {
    const val LEXICON_ASSET = "lexicon/tr_risk_lexicon.txt"
    const val TFIDF_ASSET = "models/yontem2_tfidf_lr.json"
    const val BERT_ASSET = "models/yontem3_distilberturk.tflite"
    const val BERT_VOCAB_ASSET = "models/yontem3_vocab.txt"
    const val BERT_META_ASSET = "models/yontem3_meta.json"

    @Volatile private var keyword: KeywordAnalyzer? = null
    @Volatile private var tfidf: TfidfLrAnalyzer? = null
    @Volatile private var bert: TextAnalyzer? = null
    @Volatile private var bertChecked = false

    /** Kelime listesi analizörü (geriye dönük uyumluluk). */
    fun get(context: Context): KeywordAnalyzer =
        keyword ?: synchronized(this) { keyword ?: buildKeyword(context.applicationContext).also { keyword = it } }

    fun tfidf(context: Context): TfidfLrAnalyzer = tfidf ?: synchronized(this) {
        tfidf ?: TfidfLrAnalyzer(read(context, TFIDF_ASSET)).also { tfidf = it }
    }

    /** BERT modeli APK'da yoksa veya yüklenemezse null. */
    fun bert(context: Context): TextAnalyzer? {
        if (bertChecked) return bert
        synchronized(this) {
            if (!bertChecked) {
                bert = runCatching { BertAnalyzer.fromAssets(context.applicationContext) }
                    .onFailure { Log.w("DK-Analiz", "BERT yüklenemedi: ${it.message}") }.getOrNull()
                bertChecked = true
            }
        }
        return bert
    }

    fun analyzer(context: Context, method: AnalysisMethod): TextAnalyzer = when (method) {
        AnalysisMethod.YONTEM1 -> get(context)
        AnalysisMethod.YONTEM2 -> tfidf(context)
        AnalysisMethod.YONTEM3 -> bert(context) ?: tfidf(context)
        // Test sonuçlarına göre en iyi birleşim: kelime listesi + TF-IDF, eşik 0.5 (doğrulama bölümünde seçildi)
        AnalysisMethod.BIRLESIK -> CombinedAnalyzer(get(context), tfidf(context), threshold = 0.5f)
    }

    /** Ayarlarda seçili yöntem. */
    fun engine(context: Context): TextAnalyzer = analyzer(context, AppPrefs(context).analysisMethod)

    /** Kelime listesi değişince (çocuk/veli ekledi) yeniden yüklenir. */
    fun reload(context: Context) {
        synchronized(this) { keyword = buildKeyword(context.applicationContext) }
    }

    private fun read(context: Context, asset: String) =
        context.assets.open(asset).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun buildKeyword(context: Context): KeywordAnalyzer {
        val main = read(context, LEXICON_ASSET)
        val user = UserLexiconRepository(context).rawText()
        val parent = UserLexiconRepository(context, UserLexiconRepository.PARENT_FILE_NAME).rawText()
        return KeywordAnalyzer(
            listOf(main to RuleSource.ANA_SOZLUK, user to RuleSource.KULLANICI, parent to RuleSource.VELI)
        )
    }
}
