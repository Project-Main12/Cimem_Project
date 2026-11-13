package com.digitalkalkan.app

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.digitalkalkan.app.monitoring.notification.CapturedNotification
import com.digitalkalkan.app.ui.theme.DijitalKalkanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Telefon olmadan ana ekranın görüntüsünü üretir.
 * Çalıştırma: ./gradlew testDebugUnitTest  ->  app/build/screenshots klasöründeki PNG dosyaları
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class MainScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, enabled: Boolean, captured: List<CapturedNotification> = emptyList()) {
        composeRule.setContent {
            DijitalKalkanTheme(darkTheme = false) {
                MainScreen(
                    listenerEnabled = enabled,
                    onOpenSettings = {},
                    captured = captured,
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                )
            }
        }
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val out = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun anaEkran_izinKapali() = capture("ana_ekran_izin_kapali", enabled = false)

    @Test
    fun anaEkran_izinAcik() = capture(
        "ana_ekran_izin_acik", enabled = true,
        captured = listOf(
            CapturedNotification("14:05:12", "com.digitalkalkan.app", "", "", "Kendi bildirimimiz – analiz dışı"),
            CapturedNotification("14:03:40", "com.whatsapp", "Test Grubu", "Merhaba, bu bir deneme mesajıdır şğüıöç İ"),
            CapturedNotification("14:01:05", "com.android.shell", "Test Baslik", "Merhaba bu bir deneme mesajidir")
        )
    )
}
