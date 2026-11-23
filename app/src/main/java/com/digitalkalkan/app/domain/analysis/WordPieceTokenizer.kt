package com.digitalkalkan.app.domain.analysis

/**
 * BERT WordPiece parçalayıcı (dbmdz/distilbert-base-turkish-cased ile aynı ayarlar:
 * küçük harfe çevirme YOK, aksan silme YOK). Hugging Face "BertTokenizerFast" ile birebir
 * aynı sonucu vermesi birim testiyle doğrulanır.
 */
class WordPieceTokenizer(vocabText: String, private val maxLen: Int = 64) {

    private val vocab: Map<String, Int> =
        vocabText.lineSequence().withIndex().filter { it.value.isNotEmpty() }.associate { it.value to it.index }
    private val unk = vocab.getValue("[UNK]")
    private val cls = vocab.getValue("[CLS]")
    private val sep = vocab.getValue("[SEP]")
    val padId = vocab["[PAD]"] ?: 0

    private fun isPunct(c: Char): Boolean {
        val cp = c.code
        if (cp in 33..47 || cp in 58..64 || cp in 91..96 || cp in 123..126) return true
        return when (Character.getType(c).toByte()) {
            Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
            Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
            Character.OTHER_PUNCTUATION -> true
            else -> false
        }
    }

    private fun isWhitespace(c: Char) =
        c == ' ' || c == '\t' || c == '\n' || c == '\r' || Character.getType(c).toByte() == Character.SPACE_SEPARATOR

    private fun isControl(c: Char): Boolean {
        if (c == '\t' || c == '\n' || c == '\r') return false
        val t = Character.getType(c).toByte()
        return t == Character.CONTROL || t == Character.FORMAT || t == Character.UNASSIGNED || t == Character.PRIVATE_USE
    }

    private fun isCjk(cp: Int) = cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x20000..0x2A6DF ||
        cp in 0x2A700..0x2B73F || cp in 0x2B740..0x2B81F || cp in 0x2B820..0x2CEAF ||
        cp in 0xF900..0xFAFF || cp in 0x2F800..0x2FA1F

    /** Temizleme + boşluk/noktalama ile bölme. */
    fun basicTokens(text: String): List<String> {
        val cleaned = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val chars = Character.toChars(cp)
            val c = chars[0]
            when {
                cp == 0 || cp == 0xFFFD || (chars.size == 1 && isControl(c)) -> {}
                chars.size == 1 && isWhitespace(c) -> cleaned.append(' ')
                isCjk(cp) -> cleaned.append(' ').append(chars).append(' ')
                else -> cleaned.append(chars)
            }
            i += chars.size
        }
        val out = ArrayList<String>()
        for (word in cleaned.split(' ')) {
            if (word.isEmpty()) continue
            val cur = StringBuilder()
            var j = 0
            while (j < word.length) {
                val cp = word.codePointAt(j)
                val chars = Character.toChars(cp)
                if (chars.size == 1 && isPunct(chars[0])) {
                    if (cur.isNotEmpty()) { out.add(cur.toString()); cur.clear() }
                    out.add(String(chars))
                } else cur.append(chars)
                j += chars.size
            }
            if (cur.isNotEmpty()) out.add(cur.toString())
        }
        return out
    }

    private fun wordPiece(word: String): List<Int> {
        if (word.codePointCount(0, word.length) > 100) return listOf(unk)
        val ids = ArrayList<Int>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var found: Int? = null
            while (start < end) {
                val sub = (if (start > 0) "##" else "") + word.substring(start, end)
                val id = vocab[sub]
                if (id != null) { found = id; break }
                end--
                if (end > start && Character.isLowSurrogate(word[end])) end--
            }
            if (found == null) return listOf(unk)
            ids.add(found)
            start = end
        }
        return ids
    }

    fun tokenIds(text: String): List<Int> = basicTokens(text).flatMap { wordPiece(it) }

    /** [CLS] … [SEP], maxLen'e kırpılmış ve doldurulmuş. Dönen: (input_ids, attention_mask). */
    fun encode(text: String): Pair<IntArray, IntArray> {
        val body = tokenIds(text).take(maxLen - 2)
        val ids = IntArray(maxLen) { padId }
        val mask = IntArray(maxLen)
        val seq = listOf(cls) + body + listOf(sep)
        seq.forEachIndexed { k, id -> ids[k] = id; mask[k] = 1 }
        return ids to mask
    }
}
