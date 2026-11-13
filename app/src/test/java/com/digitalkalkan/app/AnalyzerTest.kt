package com.digitalkalkan.app

import com.digitalkalkan.app.data.repository.UserEntry
import com.digitalkalkan.app.data.repository.UserLexiconRepository
import com.digitalkalkan.app.domain.analysis.KeywordAnalyzer
import com.digitalkalkan.app.domain.analysis.TurkishTextNormalizer
import com.digitalkalkan.app.domain.model.RiskCategory
import com.digitalkalkan.app.domain.model.RiskCategory.*
import com.digitalkalkan.app.domain.model.RuleSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** Telefon gerekmeden çalışan analiz motoru testleri:  ./gradlew testDebugUnitTest */
class AnalyzerTest {

    private val mainLexicon = File("src/main/assets/lexicon/tr_risk_lexicon.txt").readText()

    /** Kotlin normalleştirmesi Python ile birebir aynı mı? (2100+ cümle) */
    @Test
    fun normalizer_pythonIleAyni() {
        val lines = javaClass.classLoader!!.getResource("python_tokenlar.tsv")!!.readText().lines()
            .filter { it.isNotBlank() }
        var mismatches = 0
        for (line in lines) {
            val (text, folded, tr) = line.split("\t")
            val kf = TurkishTextNormalizer.tokens(text, true).joinToString(" ")
            val kt = TurkishTextNormalizer.tokens(text, false).joinToString(" ")
            if (kf != folded || kt != tr) {
                mismatches++
                if (mismatches <= 5) println("FARK: '$text'\n  py=$folded | $tr\n  kt=$kf | $kt")
            }
        }
        assertEquals("Python ile farklı normalleştirilen cümle sayısı", 0, mismatches)
    }

    @Test
    fun anaSozluk_temelOrnekler() {
        val a = KeywordAnalyzer(listOf(mainLexicon to RuleSource.ANA_SOZLUK))
        val cases = mapOf(
            "s.a.l.a.k mısın" to HAKARET,
            "@ptal" to HAKARET,
            "bu bölümde sıkıştım" to NORMAL,
            "i got it" to NORMAL,
            "amk yine lag" to KUFUR,
            "seni okul çıkışında bekleyeceğim" to TEHDIT,
            "kimse seni sevmiyor" to SIBER_ZORBALIK,
            "evde yalnız mısın" to UYGUNSUZ_YAKLASIM,
            "artık yaşamak istemiyorum" to KENDINE_ZARAR,
            "malzemeleri getir" to NORMAL
        )
        for ((text, expected) in cases) assertEquals(text, expected, a.analyze(text).category)
    }

    @Test
    fun kullaniciListesi_lakapYakalanir() {
        val user = UserLexiconRepository.format(
            listOf(UserEntry("koca kafa", HAKARET, false), UserEntry("fasulye", HAKARET, true))
        )
        val a = KeywordAnalyzer(listOf(mainLexicon to RuleSource.ANA_SOZLUK, user to RuleSource.KULLANICI))
        val r = a.analyze("bakın KOCA KAFA geldi")
        assertEquals(HAKARET, r.category)
        assertEquals(RuleSource.KULLANICI, r.matches.first().source)
        assertEquals(HAKARET, a.analyze("fasulyeler yine ağlıyor").category)   // ekli hâl
        assertEquals(NORMAL, a.analyze("koca bir kafa çizdim").category)        // ifade bölünmüş
    }

    @Test
    fun kullaniciListesi_dogrulama() {
        val existing = listOf(UserEntry("koca kafa", HAKARET, false))
        assertNotNull(UserLexiconRepository.validate("", existing))
        assertNotNull(UserLexiconRepository.validate("ab", existing))            // çok kısa
        assertNotNull(UserLexiconRepository.validate("lakap*", existing))        // yasak işaret
        assertNotNull(UserLexiconRepository.validate("KOCA   KAFA", existing))   // zaten var
        assertNull(UserLexiconRepository.validate("sivri burun", existing))
    }

    @Test
    fun kullaniciListesi_kaydetOku() {
        val entries = listOf(
            UserEntry("koca kafa", HAKARET, false),
            UserEntry("leylek", HAKARET, true),
            UserEntry("parkta bekliyorum seni", TEHDIT, false)
        )
        val back = UserLexiconRepository.parse(UserLexiconRepository.format(entries))
        assertEquals(entries.toSet(), back.toSet())
    }

    @Test
    fun oncelik_pythonIleAyni() {
        val order = RiskCategory.entries.sortedByDescending { it.priority }.map { it.name }
        assertEquals(
            listOf("KENDINE_ZARAR", "UYGUNSUZ_YAKLASIM", "TEHDIT", "SIBER_ZORBALIK", "KUFUR", "HAKARET", "NORMAL"),
            order
        )
    }
}
