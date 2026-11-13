package com.digitalkalkan.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digitalkalkan.app.data.repository.UserEntry
import com.digitalkalkan.app.domain.model.AnalysisResult
import com.digitalkalkan.app.domain.model.RiskCategory
import com.digitalkalkan.app.monitoring.notification.CapturedNotification
import com.digitalkalkan.app.ui.screens.StatusScreen
import com.digitalkalkan.app.ui.screens.TryTextScreen
import com.digitalkalkan.app.ui.screens.UserLexiconScreen

private val TABS = listOf("Bildirimler", "Metin dene", "Kelime listem")

/** Uygulamanın ana iskeleti: başlık + 3 sekme. */
@Composable
fun DijitalKalkanApp(
    listenerEnabled: Boolean,
    onOpenSettings: () -> Unit,
    captured: List<CapturedNotification>,
    onClearCaptured: () -> Unit,
    analyze: (String) -> AnalysisResult,
    userEntries: List<UserEntry>,
    onAddEntry: (String, RiskCategory, Boolean) -> String?,
    onRemoveEntry: (UserEntry) -> Unit,
    onShareEntries: () -> Unit,
    initialTab: Int = 0,
    tryTextInitial: String = "",
    tryTextInitialResult: AnalysisResult? = null
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Text(
                    "Dijital Kalkan",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                )
                PrimaryTabRow(selectedTabIndex = tab) {
                    TABS.forEachIndexed { i, title ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                    }
                }
            }
        }
    ) { inner ->
        val m = Modifier.padding(inner)
        when (tab) {
            0 -> StatusScreen(listenerEnabled, onOpenSettings, captured, onClearCaptured, m)
            1 -> TryTextScreen(analyze, m, tryTextInitial, tryTextInitialResult)
            else -> UserLexiconScreen(userEntries, onAddEntry, onRemoveEntry, onShareEntries, m)
        }
    }
}
