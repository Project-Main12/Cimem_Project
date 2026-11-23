package com.digitalkalkan.app.domain.analysis

import com.digitalkalkan.app.domain.model.AnalysisResult
import com.digitalkalkan.app.domain.model.RiskCategory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Yöntem 2: TF-IDF (harf 2–5 n-gram + kelime) + Lojistik Regresyon.
 * Model python/training/yontem2_tfidf.py ile eğitilir ve assets/models/yontem2_tfidf_lr.json olarak gelir.
 * Hesaplama scikit-learn ile birebir aynıdır (TfidfVectorizer sublinear_tf + L2, softmax).
 */
class TfidfLrAnalyzer(modelJson: String) : TextAnalyzer {

    override val methodName = "TF-IDF + Lojistik Regresyon"

    @Serializable
    private data class Vec(
        val vocabulary: Map<String, Int>,
        val idf: List<Double>,
        val ngram_min: Int = 1,
        val ngram_max: Int = 1
    )

    @Serializable
    private data class Model(
        val format: String,
        val classes: List<String>,
        val char: Vec,
        val word: Vec,
        val coef: List<List<Double>>,
        val intercept: List<Double>
    )

    private val model = Json { ignoreUnknownKeys = true }.decodeFromString<Model>(modelJson)
    private val classes = model.classes.map { RiskCategory.fromKey(it) ?: RiskCategory.NORMAL }
    private val charOffset = 0
    private val wordOffset = model.char.idf.size
    private val coef = model.coef.map { it.toDoubleArray() }
    private val intercept = model.intercept.toDoubleArray()

    /** scikit-learn "char_wb" n-gram üretimi ile aynı. */
    private fun charWbNgrams(text: String, minN: Int, maxN: Int): List<String> {
        val out = ArrayList<String>()
        for (raw in text.split(' ').filter { it.isNotEmpty() }) {
            val w = " $raw "
            for (n in minN..maxN) {
                var offset = 0
                out.add(w.substring(offset, minOf(offset + n, w.length)))
                while (offset + n < w.length) {
                    offset++
                    out.add(w.substring(offset, offset + n))
                }
                if (offset == 0) break
            }
        }
        return out
    }

    /** TF (1+log) × IDF, ardından L2 normalizasyon; seyrek (indeks→değer) döndürür. */
    private fun tfidf(terms: List<String>, vec: Vec, offset: Int): Map<Int, Double> {
        val counts = HashMap<Int, Int>()
        for (t in terms) vec.vocabulary[t]?.let { counts[it] = (counts[it] ?: 0) + 1 }
        val values = counts.mapValues { (i, c) -> (1.0 + ln(c.toDouble())) * vec.idf[i] }
        val norm = sqrt(values.values.sumOf { it * it })
        return if (norm == 0.0) emptyMap()
        else values.entries.associate { (i, v) -> (i + offset) to v / norm }
    }

    fun probabilities(text: String): DoubleArray {
        val prepared = TurkishTextNormalizer.tokens(text, fold = true).joinToString(" ")
        val features = tfidf(charWbNgrams(prepared, model.char.ngram_min, model.char.ngram_max), model.char, charOffset) +
            tfidf(prepared.split(' ').filter { it.isNotEmpty() }, model.word, wordOffset)
        val logits = DoubleArray(classes.size) { k ->
            var z = intercept[k]
            for ((i, v) in features) z += coef[k][i] * v
            z
        }
        val max = logits.max()
        val e = logits.map { exp(it - max) }
        val sum = e.sum()
        return DoubleArray(e.size) { e[it] / sum }
    }

    override fun analyze(text: String): AnalysisResult {
        val start = System.nanoTime()
        val p = probabilities(text)
        val best = p.indices.maxBy { p[it] }
        val scores = classes.indices.associate { classes[it] to p[it].toFloat() }
        return AnalysisResult(classes[best], emptyList(), (System.nanoTime() - start) / 1000, methodName, scores)
    }
}
