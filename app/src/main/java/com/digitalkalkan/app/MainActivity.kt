package com.digitalkalkan.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.digitalkalkan.app.data.local.AppPrefs
import com.digitalkalkan.app.data.local.AppRole
import com.digitalkalkan.app.data.remote.AlertRow
import com.digitalkalkan.app.data.remote.Backend
import com.digitalkalkan.app.data.remote.FamilyLink
import com.digitalkalkan.app.data.repository.UserEntry
import com.digitalkalkan.app.data.repository.UserLexiconRepository
import com.digitalkalkan.app.domain.analysis.AnalyzerProvider
import com.digitalkalkan.app.monitoring.notification.CapturedNotificationLog
import com.digitalkalkan.app.notifications.ParentMessagingService
import com.digitalkalkan.app.notifications.TestNotificationHelper
import com.digitalkalkan.app.permissions.NotificationAccess
import com.digitalkalkan.app.ui.DijitalKalkanApp
import com.digitalkalkan.app.ui.RoleSelectScreen
import com.digitalkalkan.app.ui.child.ChildLinkActions
import com.digitalkalkan.app.ui.parent.ParentActions
import com.digitalkalkan.app.ui.parent.ParentApp
import com.digitalkalkan.app.ui.theme.DijitalKalkanTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPrefs
    private var role by mutableStateOf<AppRole?>(null)
    private var sessionReady by mutableStateOf(false)
    private var listenerEnabled by mutableStateOf(false)
    private val userEntries = mutableStateListOf<UserEntry>()
    private lateinit var userLexicon: UserLexiconRepository

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = AppPrefs(this)
        role = prefs.role
        TestNotificationHelper.createChannel(this)
        ParentMessagingService.createChannel(this)
        userLexicon = UserLexiconRepository(this)
        userEntries.addAll(userLexicon.load())

        // Kayıtlı oturumu yükle (veli e-posta hesabı veya çocuğun anonim hesabı)
        lifecycleScope.launch {
            runCatching { Backend.awaitSession() }
            sessionReady = true
            if (role == AppRole.VELI && Backend.currentUserId() != null) onParentSignedIn()
        }

        setContent {
            DijitalKalkanTheme {
                when (role) {
                    null -> RoleSelectScreen(onSelect = { selectRole(it) })
                    AppRole.VELI -> if (sessionReady) ParentApp(parentActions, onChangeRole = { changeRole() })
                    AppRole.COCUK -> DijitalKalkanApp(
                        listenerEnabled = listenerEnabled,
                        onOpenSettings = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        captured = CapturedNotificationLog.items,
                        onClearCaptured = { CapturedNotificationLog.clear() },
                        analyze = { AnalyzerProvider.get(this).analyze(it) },
                        userEntries = userEntries,
                        onAddEntry = { phrase, category, suffixes ->
                            userLexicon.add(phrase, category, suffixes).also { error ->
                                if (error == null) refreshUserLexicon()
                            }
                        },
                        onRemoveEntry = { entry ->
                            userLexicon.remove(entry)
                            refreshUserLexicon()
                        },
                        onShareEntries = { shareUserLexicon() },
                        childLinkActions = childLinkActions
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        listenerEnabled = NotificationAccess.isGranted(this)
    }

    private fun selectRole(selected: AppRole) {
        prefs.role = selected
        role = selected
    }

    /** Rol değişince oturum kapatılır; çocuk bağlıysa önce bağlantı kaldırılır. */
    private fun changeRole() {
        lifecycleScope.launch {
            if (prefs.role == AppRole.COCUK && prefs.childLinked) {
                runCatching { Backend.childLink()?.let { Backend.removeLink(it.id) } }
            }
            runCatching { Backend.signOut() }
            prefs.childLinked = false
            prefs.role = null
            role = null
        }
    }

    private fun onParentSignedIn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        ParentMessagingService.refreshToken()
    }

    private val parentActions = object : ParentActions {
        override fun isSignedIn() = Backend.currentUserId() != null
        override fun email() = Backend.currentEmail()
        override suspend fun signIn(email: String, password: String) {
            Backend.parentSignIn(email, password); onParentSignedIn()
        }
        override suspend fun signUp(email: String, password: String) {
            Backend.parentSignUp(email, password); onParentSignedIn()
        }
        override suspend fun signOut() = Backend.signOut()
        override suspend fun createCode() = Backend.createPairingCode()
        override suspend fun links() = Backend.listLinks()
        override suspend fun alerts() = Backend.listAlerts()
        override suspend fun markRead(alert: AlertRow) = Backend.markAlertRead(alert.id)
        override suspend fun removeLink(link: FamilyLink) = Backend.removeLink(link.id)
    }

    private val childLinkActions = object : ChildLinkActions {
        override suspend fun loadLink(): FamilyLink? {
            Backend.childEnsureSession()
            return Backend.childLink().also { prefs.childLinked = it != null }
        }
        override suspend fun loadSentAlerts() = Backend.listAlerts()
        override suspend fun redeem(code: String, label: String) {
            Backend.childEnsureSession()
            Backend.redeemPairingCode(code, label)
            prefs.childLinked = true
        }
        override suspend fun unlink(link: FamilyLink) {
            Backend.removeLink(link.id)
            prefs.childLinked = false
        }
    }

    private fun refreshUserLexicon() {
        userEntries.clear()
        userEntries.addAll(userLexicon.load())
        AnalyzerProvider.reload(this)
    }

    private fun shareUserLexicon() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Dijital Kalkan – kelime listem")
            putExtra(Intent.EXTRA_TEXT, UserLexiconRepository.format(userEntries))
        }
        startActivity(Intent.createChooser(send, "Listeyi gönder"))
    }
}
