package com.digitalkalkan.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.digitalkalkan.app.data.remote.AlertRow
import com.digitalkalkan.app.data.remote.FamilyLink
import com.digitalkalkan.app.data.repository.UserEntry
import com.digitalkalkan.app.ui.child.ChildLinkActions
import com.digitalkalkan.app.ui.parent.ParentActions
import com.digitalkalkan.app.ui.parent.ParentApp
import java.time.OffsetDateTime
import com.digitalkalkan.app.domain.analysis.KeywordAnalyzer
import com.digitalkalkan.app.domain.model.RiskCategory
import com.digitalkalkan.app.domain.model.RuleSource
import com.digitalkalkan.app.monitoring.notification.CapturedNotification
import com.digitalkalkan.app.ui.DijitalKalkanApp
import com.digitalkalkan.app.ui.theme.DijitalKalkanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Telefon olmadan ekran görüntüsü üretir.
 * Çalıştırma: ./gradlew testDebugUnitTest  ->  app/build/screenshots klasöründeki PNG dosyaları
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h1400dp-xhdpi")
class MainScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val analyzer = KeywordAnalyzer(
        listOf(
            File("src/main/assets/lexicon/tr_risk_lexicon.txt").readText() to RuleSource.ANA_SOZLUK,
            "[HAKARET]\nkoca kafa\n" to RuleSource.KULLANICI
        )
    )
    private val entries = listOf(
        UserEntry("koca kafa", RiskCategory.HAKARET, false),
        UserEntry("leylek", RiskCategory.HAKARET, true),
        UserEntry("parkta bekleriz", RiskCategory.TEHDIT, false)
    )

    private val linked = FamilyLink("l1", "p1", "c1", "Ali", "2026-11-16T09:30:00+00:00")
    private val fakeLink = object : ChildLinkActions {
        override suspend fun loadLink() = linked
        override suspend fun loadSentAlerts() = listOf(
            AlertRow(2, "c1", "TEHDIT", "YUKSEK", "com.whatsapp", "2026-11-16T11:06:02+00:00"),
            AlertRow(1, "c1", "HAKARET", "DUSUK", "com.roblox.client", "2026-11-16T10:41:15+00:00")
        )
        override suspend fun redeem(code: String, label: String) {}
        override suspend fun unlink(link: FamilyLink) {}
    }

    private fun note(time: String, pkg: String, text: String, sent: Boolean = false) =
        CapturedNotification(time, pkg, result = analyzer.analyze(text), sentToParent = sent)

    private fun capture(name: String, tab: Int, tryText: String = "") {
        composeRule.setContent {
            DijitalKalkanTheme(darkTheme = false) {
                DijitalKalkanApp(
                    listenerEnabled = true,
                    onOpenSettings = {},
                    captured = listOf(
                        note("14:06:02", "com.whatsapp", "seni okul çıkışında bekleyeceğim", sent = true),
                        note("14:05:40", "com.roblox.client", "bakın koca kafa yine geldi 😂", sent = true),
                        note("14:03:10", "com.google.android.apps.messaging", "yarın matematik ödevini getir"),
                        CapturedNotification("14:01:00", "com.digitalkalkan.app", "Kendi bildirimimiz – analiz dışı")
                    ),
                    onClearCaptured = {},
                    analyze = analyzer::analyze,
                    userEntries = entries,
                    parentEntries = listOf(UserEntry("leylek bacak", RiskCategory.HAKARET, true)),
                    onAddEntry = { _, _, _ -> null },
                    onRemoveEntry = {},
                    onShareEntries = {},
                    childLinkActions = fakeLink,
                    initialTab = tab,
                    tryTextInitial = tryText,
                    tryTextInitialResult = if (tryText.isBlank()) null else analyzer.analyze(tryText)
                )
            }
        }
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val out = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun sekme1_bildirimler() = capture("sekme1_bildirimler", 0)
    @Test fun sekme2_metinDene() = capture("sekme2_metin_dene", 1, "s.a.l.a.k mısın sen, okul çıkışında bekle")
    @Test fun sekme3_kelimeListem() = capture("sekme3_kelime_listem", 2)
    @Test fun sekme4_veliBaglantisi() = capture("sekme4_veli_baglantisi", 3)

    private val fakeParent = object : ParentActions {
        private val now = OffsetDateTime.now()
        override fun isSignedIn() = true
        override fun email() = "veli@ornek.com"
        override suspend fun signIn(email: String, password: String) {}
        override suspend fun signUp(email: String, password: String) {}
        override suspend fun signOut() {}
        override suspend fun createCode() = "482913"
        override suspend fun links() = listOf(linked)
        override suspend fun alerts() = listOf(
            AlertRow(3, "c1", "TEHDIT", "YUKSEK", "com.whatsapp", now.minusMinutes(4).toString()),
            AlertRow(2, "c1", "HAKARET", "DUSUK", "com.roblox.client", now.minusHours(3).toString(), readAt = now.toString()),
            AlertRow(1, "c1", "HAKARET", "DUSUK", "com.whatsapp", now.minusDays(2).toString(), readAt = now.toString())
        )
        override suspend fun markRead(alert: AlertRow) {}
        override suspend fun removeLink(link: FamilyLink) {}
        override fun sentWords(childId: String) = listOf(UserEntry("leylek bacak", RiskCategory.HAKARET, true))
        override suspend fun sendWord(childId: String, phrase: String, category: RiskCategory, includeSuffixes: Boolean) = "✅"
        override suspend fun removeWord(childId: String, entry: UserEntry) = "✅"
    }

    @Test
    fun veli_anaEkran() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            DijitalKalkanTheme(darkTheme = false) { ParentApp(fakeParent, onChangeRole = {}) }
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val out = File("build/screenshots/veli_ana_ekran.png").apply { parentFile?.mkdirs() }
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
