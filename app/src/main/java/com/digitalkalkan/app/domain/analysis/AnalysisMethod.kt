package com.digitalkalkan.app.domain.analysis

import com.digitalkalkan.app.domain.model.AnalysisResult
import com.digitalkalkan.app.domain.model.RiskCategory

/** Kullanıcının seçebileceği analiz yöntemleri (araştırmadaki 3 yöntem + birleşik). */
enum class AnalysisMethod(val label: String, val description: String) {
    BIRLESIK("Birleşik (önerilen)", "Kelime listesi + TF-IDF modeli. Testte en yüksek başarı (Macro-F1 0,85)."),
    YONTEM1("Yöntem 1: Kelime listesi", "Anahtar kelime filtreleme"),
    YONTEM2("Yöntem 2: TF-IDF + LR", "Geleneksel makine öğrenmesi"),
    YONTEM3("Yöntem 3: DistilBERTurk", "Türkçe dil modeli (LiteRT, cihaz üzerinde)")
}

/**
 * Birleşik yöntem: Kelime listesi (ana + çocuğun + velinin listesi) bir risk bulursa onu kullanır;
 * bulamazsa ML modelinin kararını, güveni [threshold] üzerindeyse kabul eder.
 * Böylece velinin/çocuğun eklediği lakaplar asla kaçmaz, kelime listesinin kaçırdığı örtük
 * ifadeler de model tarafından yakalanır.
 */
class CombinedAnalyzer(
    private val keyword: TextAnalyzer,
    private val model: TextAnalyzer,
    private val threshold: Float = 0.6f
) : TextAnalyzer {
    override val methodName = "Birleşik (kelime + ${model.methodName})"

    override fun analyze(text: String): AnalysisResult {
        val start = System.nanoTime()
        val k = keyword.analyze(text)
        val result = if (k.isRisky) k else {
            val m = model.analyze(text)
            if (m.isRisky && (m.confidence ?: 0f) >= threshold) m
            else m.copy(category = RiskCategory.NORMAL)
        }
        return result.copy(method = methodName, durationMicros = (System.nanoTime() - start) / 1000)
    }
}
