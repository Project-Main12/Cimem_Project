package com.digitalkalkan.app

import com.digitalkalkan.app.domain.analysis.WordPieceTokenizer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Kotlin WordPiece parçalayıcı, Hugging Face (Python) ile aynı token kimliklerini üretiyor mu? */
class Yontem3TokenizerTest {
    private val tok = WordPieceTokenizer(File("src/main/assets/models/yontem3_vocab.txt").readText(), 64)

    @Test
    fun tokenlar_huggingFaceIleAyni() {
        val lines = javaClass.classLoader!!.getResource("yontem3_tokenlar.tsv")!!.readText().lines().filter { it.isNotBlank() }
        var mismatches = 0
        for (line in lines) {
            val (text, ids) = line.split("\t")
            val expected = ids.split(" ").filter { it.isNotEmpty() }.map { it.toInt() }
            val actual = tok.tokenIds(text)
            if (expected != actual) {
                mismatches++
                if (mismatches <= 5) println("FARK: '$text'\n  py=$expected\n  kt=$actual")
            }
        }
        println("Yöntem 3 parçalayıcı: ${lines.size - mismatches}/${lines.size} cümle aynı")
        assertEquals(0, mismatches)
    }

    @Test
    fun kodlama_clsSepVeDolgu() {
        val (ids, mask) = tok.encode("merhaba")
        assertEquals(64, ids.size)
        assertEquals(1, mask[0])
        assertEquals(0, mask[63])
    }
}
