package com.digitalkalkan.app.domain.model

/** Risk seviyesi (ebeveyne gösterilecek). */
enum class RiskLevel(val label: String) {
    YOK("Yok"), DUSUK("Düşük"), ORTA("Orta"), YUKSEK("Yüksek")
}

/**
 * Sınıflar. [priority] büyük olan baskındır (bir mesaj birden fazla sınıfa uyarsa).
 * Python tarafındaki sıra ile aynıdır:
 * KENDINE_ZARAR > UYGUNSUZ_YAKLASIM > TEHDIT > SIBER_ZORBALIK > KUFUR > HAKARET
 */
enum class RiskCategory(
    val label: String,
    val priority: Int,
    val level: RiskLevel,
    val color: Long
) {
    NORMAL("Normal", 0, RiskLevel.YOK, 0xFF2E7D32),
    HAKARET("Hakaret", 1, RiskLevel.DUSUK, 0xFFF57F17),
    KUFUR("Küfür", 2, RiskLevel.ORTA, 0xFFE65100),
    SIBER_ZORBALIK("Siber zorbalık", 3, RiskLevel.YUKSEK, 0xFFC62828),
    TEHDIT("Tehdit", 4, RiskLevel.YUKSEK, 0xFFB71C1C),
    UYGUNSUZ_YAKLASIM("Uygunsuz yaklaşım", 5, RiskLevel.YUKSEK, 0xFF6A1B9A),
    KENDINE_ZARAR("Kendine zarar", 6, RiskLevel.YUKSEK, 0xFF4A148C);

    companion object {
        fun fromKey(key: String): RiskCategory? = entries.firstOrNull { it.name == key }

        /** Kullanıcının kelime eklerken seçebileceği sınıflar. */
        val selectable: List<RiskCategory> get() = entries.filter { it != NORMAL }
    }
}
