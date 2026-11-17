package com.digitalkalkan.app.domain.model

/**
 * Veliye gösterilen uyarı metinleri. Mesajın kendisi ASLA yer almaz.
 * Örnek: "Ali · WhatsApp: Tehdit kategorisinde uygunsuz mesaj tespit edildi (yüksek risk)."
 */
object AlertText {
    const val TITLE = "⚠️ Uygunsuz mesaj tespit edildi"

    fun sentence(category: RiskCategory?): String {
        val cat = category?.label ?: "Riskli içerik"
        val level = category?.level?.label?.lowercase(java.util.Locale.forLanguageTag("tr-TR"))
        return "$cat kategorisinde uygunsuz mesaj tespit edildi" + (level?.let { " ($it risk)" } ?: "") + "."
    }

    fun body(childLabel: String?, sourcePackage: String, category: RiskCategory?): String =
        listOfNotNull(childLabel, AppNames.label(sourcePackage)).joinToString(" · ") + ": " + sentence(category)
}
