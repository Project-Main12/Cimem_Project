package com.digitalkalkan.app

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.digitalkalkan.app.data.local.db.CategoryCount
import com.digitalkalkan.app.data.local.db.MethodStat
import com.digitalkalkan.app.data.local.db.SourceCount
import com.digitalkalkan.app.data.remote.AlertRow
import com.digitalkalkan.app.data.remote.FamilyLink
import com.digitalkalkan.app.data.repository.UserEntry
import com.digitalkalkan.app.data.repository.UserLexiconRepository
import com.digitalkalkan.app.domain.analysis.AnalysisMethod
import com.digitalkalkan.app.domain.analysis.CombinedAnalyzer
import com.digitalkalkan.app.domain.analysis.KeywordAnalyzer
import com.digitalkalkan.app.domain.analysis.TfidfLrAnalyzer
import com.digitalkalkan.app.domain.model.RiskCategory
import com.digitalkalkan.app.domain.model.RuleSource
import com.digitalkalkan.app.monitoring.notification.CapturedNotification
import com.digitalkalkan.app.ui.DijitalKalkanApp
import com.digitalkalkan.app.ui.RoleSelectScreen
import com.digitalkalkan.app.ui.child.ChildLinkActions
import com.digitalkalkan.app.ui.parent.ParentActions
import com.digitalkalkan.app.ui.parent.ParentApp
import com.digitalkalkan.app.ui.screens.SettingsActions
import com.digitalkalkan.app.ui.theme.DijitalKalkanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.OffsetDateTime

/**
 * Uygulama turu: gerçek arayüz + gerçek analiz motoru (kelime + TF-IDF birleşik) ile bir kullanım senaryosu
 * oynatır ve her adımın görüntüsünü kaydeder (build/tur/). Sunucu işlemleri örnek verilerle taklit edilir.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h760dp-xhdpi")
class UygulamaTuruTest {

    @get:Rule
    val rule = createComposeRule()

    private val outDir = File("build/tur").apply { deleteRecursively(); mkdirs() }
    private val captions = StringBuilder()
    private var frame = 0

    private fun shot(caption: String) {
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        val name = "%02d.png".format(++frame)
        File(outDir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        captions.appendLine("$name\t$caption")
        File(outDir, "aciklamalar.tsv").writeText(captions.toString())
    }

    private val lexicon = File("src/main/assets/lexicon/tr_risk_lexicon.txt").readText()
    private val tfidf = TfidfLrAnalyzer(File("src/main/assets/models/yontem2_tfidf_lr.json").readText())
    private val userEntries = mutableStateListOf<UserEntry>()
    private val parentEntries = mutableStateListOf(UserEntry("leylek bacak", RiskCategory.HAKARET, true))
    private var keyword = build()

    private fun build() = KeywordAnalyzer(
        listOf(
            lexicon to RuleSource.ANA_SOZLUK,
            UserLexiconRepository.format(userEntries) to RuleSource.KULLANICI,
            UserLexiconRepository.format(parentEntries) to RuleSource.VELI
        )
    )

    private fun analyzeAll(text: String) =
        listOf(CombinedAnalyzer(keyword, tfidf, 0.5f).analyze(text), keyword.analyze(text), tfidf.analyze(text))

    private val now = OffsetDateTime.now()
    private val link = FamilyLink("l1", "p1", "c1", "Ali", now.minusDays(1).toString())
    private val alerts = listOf(
        AlertRow(3, "c1", "TEHDIT", "YUKSEK", "com.whatsapp", now.minusMinutes(3).toString()),
        AlertRow(2, "c1", "HAKARET", "DUSUK", "com.roblox.client", now.minusHours(2).toString(), readAt = now.toString()),
        AlertRow(1, "c1", "SIBER_ZORBALIK", "YUKSEK", "dijitalkalkan.ekran", now.minusDays(1).toString(), readAt = now.toString())
    )

    @Test
    fun tur() {
        // 1) Rol seçimi
        val screenState = androidx.compose.runtime.mutableStateOf("rol")
        rule.setContent {
            DijitalKalkanTheme(darkTheme = false) {
                when (screenState.value) {
                    "rol" -> RoleSelectScreen(onSelect = {})
                    "cocuk" -> DijitalKalkanApp(
                        listenerEnabled = true, onOpenSettings = {},
                        captured = listOf(
                            CapturedNotification("14:06:02", "com.whatsapp", result = analyzeAll("seni okul çıkışında bekleyeceğim")[0], sentToParent = true),
                            CapturedNotification("14:05:40", "dijitalkalkan.ekran", result = analyzeAll("herkes sana gülüyor ezik")[0], sentToParent = true),
                            CapturedNotification("14:03:10", "com.google.android.apps.messaging", result = analyzeAll("yarın matematik ödevini getir")[0])
                        ),
                        onClearCaptured = {},
                        analyzeAll = ::analyzeAll,
                        userEntries = userEntries, parentEntries = parentEntries,
                        onAddEntry = { p, c, s ->
                            val clean = UserLexiconRepository.cleanPhrase(p)
                            UserLexiconRepository.validate(clean, userEntries)?.let { return@DijitalKalkanApp it }
                            userEntries.add(UserEntry(clean, c, s)); keyword = build(); null
                        },
                        onRemoveEntry = { userEntries.remove(it); keyword = build() },
                        onShareEntries = {},
                        childLinkActions = object : ChildLinkActions {
                            override suspend fun loadLink() = link
                            override suspend fun loadSentAlerts() = alerts
                            override suspend fun redeem(code: String, label: String) {}
                            override suspend fun unlink(link: FamilyLink) {}
                        },
                        settingsActions = object : SettingsActions {
                            override fun method() = AnalysisMethod.BIRLESIK
                            override fun setMethod(m: AnalysisMethod) {}
                            override fun bertAvailable() = true
                            override fun monitorRunning() = false
                            override fun monitorStatus() = "Kapalı"
                            override fun startMonitor(ocr: Boolean, audio: Boolean) {}
                            override fun stopMonitor() {}
                            override fun voskReady() = false
                            override suspend fun downloadVosk(progress: (Int) -> Unit) {}
                            override suspend fun stats() = Triple(
                                listOf(MethodStat("Birleşik (kelime + TF-IDF + Lojistik Regresyon)", 42, 950.0, 4100)),
                                listOf(CategoryCount("TEHDIT", 1), CategoryCount("SIBER_ZORBALIK", 1)),
                                listOf(SourceCount("BILDIRIM", 40), SourceCount("EKRAN", 2))
                            )
                            override fun exportCsv() {}
                            override suspend fun clearEvents() {}
                        }
                    )
                    else -> ParentApp(object : ParentActions {
                        override fun isSignedIn() = true
                        override fun email() = "veli@ornek.com"
                        override suspend fun signIn(email: String, password: String) {}
                        override suspend fun signUp(email: String, password: String) {}
                        override suspend fun signOut() {}
                        override suspend fun createCode() = "482913"
                        override suspend fun links() = listOf(link)
                        override suspend fun alerts() = alerts
                        override suspend fun markRead(alert: AlertRow) {}
                        override suspend fun removeLink(link: FamilyLink) {}
                        override fun sentWords(childId: String) = parentEntries.toList()
                        override suspend fun sendWord(childId: String, phrase: String, category: RiskCategory, includeSuffixes: Boolean) = "✅ Çocuğunuzun telefonuna iletildi."
                        override suspend fun removeWord(childId: String, entry: UserEntry) = "✅"
                    }, onChangeRole = {})
                }
            }
        }
        rule.waitForIdle()
        shot("İlk açılış: bu telefon veli telefonu mu, çocuk telefonu mu?")

        rule.runOnIdle { screenState.value = "cocuk" }; rule.waitForIdle()
        shot("Çocuk telefonu – Tespitler: mesaj metni hiç görünmez, yalnızca uygulama, saat ve kategori")

        rule.onNodeWithText("Metin dene").performClick(); rule.mainClock.advanceTimeBy(500)
        shot("Metin dene: bir cümle yazıp analiz edelim")

        fun tryText(t: String, caption: String) {
            rule.onNode(hasSetTextAction()).performTextClearance()
            rule.onNode(hasSetTextAction()).performTextInput(t)
            rule.onNodeWithText("Analiz et").performClick(); rule.mainClock.advanceTimeBy(500)
            shot(caption)
        }
        tryText("seni okul çıkışında bekleyeceğim", "Açık tehdit: birleşik yöntem (üstte) ve diğer yöntemler yan yana")
        tryText("sınıfta görüşürüz, hazırlıklı ol", "Örtük tehdit: kelime listesi kaçırır, makine öğrenmesi modeli yakalayabilir")
        tryText("s.a.l.a.k mısın sen", "Yazım hilesi (s.a.l.a.k): normalleştirme ile yakalanır")
        tryText("yarın matematik ödevini getirmeyi unutma", "Normal mesaj: uyarı yok")

        rule.onNodeWithText("Kelimelerim").performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNode(hasSetTextAction()).performTextInput("koca kafa")
        rule.onNodeWithText("Listeye ekle").performClick(); rule.mainClock.advanceTimeBy(500)
        shot("Kelimelerim: çocuğa takılan lakap eklendi; velinin eklediği ifadeler de görünür")

        rule.onNodeWithText("Metin dene").performClick(); rule.mainClock.advanceTimeBy(500)
        tryText("bakın koca kafa yine geldi 😂", "Eklenen lakap artık yakalanıyor (sizin listeniz)")

        rule.onNodeWithText("Veli").performClick(); rule.mainClock.advanceTimeBy(1000)
        shot("Veli sekmesi: bağlantı her zaman görünür; velisine giden uyarılar listelenir")

        rule.onNodeWithText("Ayarlar").performClick(); rule.mainClock.advanceTimeBy(1000)
        shot("Ayarlar: analiz yöntemi, ekran (OCR) ve ses analizi, ölçümler")

        rule.mainClock.autoAdvance = false   // veli ekranında 30 sn'lik yenileme döngüsü var
        rule.runOnUiThread { screenState.value = "veli"; androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications() }
        repeat(30) { rule.mainClock.advanceTimeByFrame() }; rule.mainClock.advanceTimeBy(1500)
        shot("Veli telefonu: bağlı cihaz, son 7 gün özeti, kelime listesi ve uyarılar")

        rule.onNodeWithText("➕  Çocuk cihazı ekle").performClick(); rule.mainClock.advanceTimeBy(1500)
        shot("Veli 'Çocuk cihazı ekle' dedi: çocuğun telefonuna girilecek 6 haneli kod")
    }
}
