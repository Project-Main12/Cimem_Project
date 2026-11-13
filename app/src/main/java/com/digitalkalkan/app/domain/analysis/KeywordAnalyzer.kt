package com.digitalkalkan.app.domain.analysis

import com.digitalkalkan.app.domain.model.AnalysisResult
import com.digitalkalkan.app.domain.model.RiskCategory
import com.digitalkalkan.app.domain.model.RuleMatch
import com.digitalkalkan.app.domain.model.RuleSource

/** Tüm analiz yöntemlerinin ortak arayüzü (ileride BERT modeli de bunu uygulayacak). */
interface TextAnalyzer {
    val methodName: String
    fun analyze(text: String): AnalysisResult
}

/**
 * Yöntem 1: Anahtar kelime (sözlük) tabanlı sınıflandırıcı.
 * python/ortak/sozluk.py ile aynı mantık. Sözlük biçimi: assets/lexicon/tr_risk_lexicon.txt
 */
class KeywordAnalyzer(lexicons: List<Pair<String, RuleSource>>) : TextAnalyzer {

    override val methodName = "Anahtar kelime"

    private class Rule(
        val raw: String,
        val category: RiskCategory?,      // null = ISTISNA
        val parts: List<String>,
        val prefix: Boolean,
        val diacriticSensitive: Boolean,
        val source: RuleSource
    ) {
        fun matches(folded: List<String>, tr: List<String>, i: Int): Boolean {
            val words = if (diacriticSensitive) tr else folded
            if (i + parts.size > words.size) return false
            for (j in parts.indices) {
                val w = words[i + j]
                val last = j == parts.lastIndex
                if (last && prefix) {
                    if (!w.startsWith(parts[j])) return false
                } else if (w != parts[j]) return false
            }
            return true
        }
    }

    private val rules = mutableListOf<Rule>()
    private val exceptions = mutableListOf<Rule>()

    val ruleCount: Int get() = rules.size

    init {
        for ((text, source) in lexicons) parse(text, source)
    }

    private fun parse(text: String, source: RuleSource) {
        var section: String? = null
        for (line in text.lineSequence()) {
            val s = line.trim()
            if (s.isEmpty() || s.startsWith("#")) continue
            if (s.startsWith("[") && s.endsWith("]")) {
                section = s.substring(1, s.length - 1).trim()
                continue
            }
            val isException = section == "ISTISNA"
            val category = section?.let { RiskCategory.fromKey(it) }
            if (!isException && (category == null || category == RiskCategory.NORMAL)) continue
            val sensitive = s.startsWith("!")
            var body = if (sensitive) s.substring(1) else s
            val prefix = body.endsWith("*")
            body = body.trimEnd('*')
            val parts = TurkishTextNormalizer.tokens(body, fold = !sensitive)
            if (parts.isEmpty()) continue
            val rule = Rule(s, category, parts, prefix, sensitive, source)
            if (isException) exceptions.add(rule) else rules.add(rule)
        }
    }

    override fun analyze(text: String): AnalysisResult {
        val start = System.nanoTime()
        val folded = TurkishTextNormalizer.tokens(text, fold = true)
        val tr = TurkishTextNormalizer.tokens(text, fold = false)
        val found = mutableListOf<RuleMatch>()
        for (i in folded.indices) {
            for (rule in rules) {
                if (!rule.matches(folded, tr, i)) continue
                if (rule.parts.size == 1 &&
                    exceptions.any { it.parts.size == 1 && it.matches(folded, tr, i) }
                ) continue
                found.add(RuleMatch(rule.category!!, rule.raw, rule.source))
            }
        }
        val category = found.maxByOrNull { it.category.priority }?.category ?: RiskCategory.NORMAL
        val micros = (System.nanoTime() - start) / 1000
        return AnalysisResult(category, found.distinct(), micros, methodName)
    }
}
