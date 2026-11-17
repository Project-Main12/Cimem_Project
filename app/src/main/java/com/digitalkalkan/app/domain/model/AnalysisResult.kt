package com.digitalkalkan.app.domain.model

/** Kuralın geldiği yer: ana sözlük, çocuğun kendi listesi veya velinin eklediği liste. */
enum class RuleSource { ANA_SOZLUK, KULLANICI, VELI }

data class RuleMatch(
    val category: RiskCategory,
    val rule: String,
    val source: RuleSource
)

data class AnalysisResult(
    val category: RiskCategory,
    val matches: List<RuleMatch>,
    val durationMicros: Long,
    val method: String
) {
    val level: RiskLevel get() = category.level
    val isRisky: Boolean get() = category != RiskCategory.NORMAL
}
