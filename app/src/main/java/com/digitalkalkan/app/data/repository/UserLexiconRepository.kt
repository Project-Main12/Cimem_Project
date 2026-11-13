package com.digitalkalkan.app.data.repository

import android.content.Context
import com.digitalkalkan.app.domain.analysis.TurkishTextNormalizer
import com.digitalkalkan.app.domain.model.RiskCategory
import java.io.File

/** Kullanıcının eklediği bir kelime/ifade (ör. çocuğa takılan lakap). */
data class UserEntry(
    val phrase: String,
    val category: RiskCategory,
    val includeSuffixes: Boolean
) {
    /** Sözlük dosyasındaki satır karşılığı. */
    val lexiconLine: String get() = if (includeSuffixes) "$phrase*" else phrase
}

/**
 * Kullanıcı kelime listesi. YALNIZCA bu telefonda saklanır (uygulamanın özel klasörü),
 * internete gönderilmez. Biçim, ana sözlükle aynıdır; böylece öğretmen kolayca birleştirebilir.
 */
class UserLexiconRepository(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)

    fun rawText(): String = if (file.exists()) file.readText(Charsets.UTF_8) else ""

    fun load(): List<UserEntry> = parse(rawText())

    /** Ekler; hata varsa kullanıcıya gösterilecek mesajı döndürür, başarılıysa null. */
    fun add(phrase: String, category: RiskCategory, includeSuffixes: Boolean): String? {
        val clean = cleanPhrase(phrase)
        val current = load()
        validate(clean, current)?.let { return it }
        save(current + UserEntry(clean, category, includeSuffixes))
        return null
    }

    fun remove(entry: UserEntry) = save(load().filterNot { it == entry })

    private fun save(entries: List<UserEntry>) {
        file.writeText(format(entries), Charsets.UTF_8)
    }

    companion object {
        const val FILE_NAME = "kullanici_sozluk.txt"
        private const val MIN_LETTERS = 3
        private const val MAX_LENGTH = 60
        private val FORBIDDEN = Regex("[\\[\\]#*!|]")

        fun cleanPhrase(phrase: String): String = phrase.trim().replace(Regex("\\s+"), " ")

        fun validate(clean: String, existing: List<UserEntry>): String? {
            if (clean.isEmpty()) return "Lütfen bir kelime veya ifade yazın."
            if (clean.length > MAX_LENGTH) return "En fazla $MAX_LENGTH karakter yazabilirsiniz."
            if (FORBIDDEN.containsMatchIn(clean)) return "Şu işaretler kullanılamaz: [ ] # * ! |"
            val tokens = TurkishTextNormalizer.tokens(clean)
            if (tokens.isEmpty()) return "İfade en az bir harf içermeli."
            if (tokens.joinToString("").length < MIN_LETTERS)
                return "Çok kısa. En az $MIN_LETTERS harf olmalı (kısa kelimeler yanlış alarm verir)."
            if (existing.any { TurkishTextNormalizer.tokens(it.phrase) == tokens })
                return "Bu ifade zaten listede."
            return null
        }

        fun parse(text: String): List<UserEntry> {
            val result = mutableListOf<UserEntry>()
            var category: RiskCategory? = null
            for (line in text.lineSequence()) {
                val s = line.trim()
                if (s.isEmpty() || s.startsWith("#")) continue
                if (s.startsWith("[") && s.endsWith("]")) {
                    category = RiskCategory.fromKey(s.substring(1, s.length - 1).trim())
                    continue
                }
                val cat = category ?: continue
                if (cat == RiskCategory.NORMAL) continue
                val suffix = s.endsWith("*")
                result.add(UserEntry(s.trimEnd('*').trim(), cat, suffix))
            }
            return result
        }

        fun format(entries: List<UserEntry>): String = buildString {
            appendLine("# Dijital Kalkan – Kullanıcı kelime listesi")
            appendLine("# Bu telefonda eklenmiştir. Biçim ana sözlükle (tr_risk_lexicon.txt) aynıdır.")
            appendLine("# Ana sözlüğe eklenmesi için danışman öğretmene gönderilebilir.")
            for (cat in RiskCategory.selectable) {
                val items = entries.filter { it.category == cat }
                if (items.isEmpty()) continue
                appendLine()
                appendLine("[${cat.name}]")
                items.forEach { appendLine(it.lexiconLine) }
            }
        }
    }
}
