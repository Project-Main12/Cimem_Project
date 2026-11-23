package com.digitalkalkan.app

import com.digitalkalkan.app.domain.analysis.TfidfLrAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** Kotlin'deki Yöntem 2, Python (scikit-learn) ile aynı olasılıkları veriyor mu? */
class Yontem2Test {
    private val analyzer = TfidfLrAnalyzer(File("src/main/assets/models/yontem2_tfidf_lr.json").readText())

    @Test
    fun olasiliklar_pythonIleAyni() {
        val lines = javaClass.classLoader!!.getResource("yontem2_referans.tsv")!!.readText().lines().filter { it.isNotBlank() }
        var maxDiff = 0.0
        for (line in lines) {
            val (text, probs) = line.split("\t")
            val expected = probs.split(" ").map { it.toDouble() }
            val actual = analyzer.probabilities(text)
            expected.indices.forEach { maxDiff = maxOf(maxDiff, abs(expected[it] - actual[it])) }
        }
        println("Yöntem 2 – Python ile en büyük olasılık farkı: $maxDiff (${lines.size} cümle)")
        assertEquals(0.0, maxDiff, 1e-4)
    }
}
