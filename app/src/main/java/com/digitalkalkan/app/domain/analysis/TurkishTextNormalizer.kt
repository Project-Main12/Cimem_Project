package com.digitalkalkan.app.domain.analysis

import java.util.Locale

/**
 * Türkçe metin normalleştirme.
 * python/ortak/turkce_metin.py ile AYNI kuralları uygular; biri değişirse diğeri de değişmeli.
 *
 * 1. Türkçe küçük harf (I -> ı, İ -> i)
 * 2. Sembol/rakam hileleri (@ptal, s4lak, $erefsiz) – yalnızca harfe bitişikse
 * 3. Harf arası yıldız silinir (or*spu -> orspu)
 * 4. Harf/rakam dışı her şey ayırıcıdır
 * 5. Art arda tek harfler birleştirilir (s.a.l.a.k -> salak)
 * 6. Harf tekrarları teke indirilir (salaaak -> salak)
 * 7. (isteğe bağlı) Türkçe harfler sadeleştirilir (ş -> s, ı -> i ...)
 */
object TurkishTextNormalizer {
    private val TR: Locale = Locale.forLanguageTag("tr-TR")
    private val LEET = mapOf(
        '@' to 'a', '4' to 'a', '3' to 'e', '1' to 'i', '!' to 'i',
        '0' to 'o', '5' to 's', '$' to 's', '7' to 't', '€' to 'e'
    )
    private val FOLD = mapOf(
        'ç' to 'c', 'ğ' to 'g', 'ı' to 'i', 'ö' to 'o', 'ş' to 's', 'ü' to 'u',
        'â' to 'a', 'î' to 'i', 'û' to 'u'
    )
    private val WORD = Regex("[\\p{L}\\p{N}]+")
    private val REPEAT = Regex("(.)\\1+")

    fun trLower(text: String): String = text.lowercase(TR)

    fun asciiFold(text: String): String =
        buildString(text.length) { text.forEach { append(FOLD[it] ?: it) } }

    private fun mapLeet(text: String): String = buildString(text.length) {
        for (i in text.indices) {
            val ch = text[i]
            val prevLetter = i > 0 && text[i - 1].isLetter()
            val nextLetter = i + 1 < text.length && text[i + 1].isLetter()
            if (ch == '*' && prevLetter && nextLetter) continue
            val mapped = LEET[ch]
            append(if (mapped != null && (prevLetter || nextLetter)) mapped else ch)
        }
    }

    fun tokens(text: String, fold: Boolean = true): List<String> {
        val raw = WORD.findAll(mapLeet(trLower(text))).map { it.value }.toList()
        val merged = mutableListOf<String>()
        val buffer = mutableListOf<String>()
        fun flush() {
            if (buffer.size >= 2) merged.add(buffer.joinToString("")) else merged.addAll(buffer)
            buffer.clear()
        }
        for (w in raw) {
            if (w.length == 1 && w[0].isLetter()) buffer.add(w) else { flush(); merged.add(w) }
        }
        flush()
        return merged.map { REPEAT.replace(it, "$1") }.map { if (fold) asciiFold(it) else it }
    }
}
