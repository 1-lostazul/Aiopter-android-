package io.aiopter.companion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.aiopter.companion.access.AccessActivity
import io.aiopter.companion.access.ForegroundAppResolver
import io.aiopter.companion.auth.LoginActivity
import io.aiopter.companion.capture.ScreenCaptureService
import io.aiopter.companion.overlay.OverlayService
import io.aiopter.companion.ui.AIopterTheme
import io.aiopter.companion.ui.RotorLogo

class MainActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)
    private var pendingOverlayStart = false
    private var showCaptureDisclosure by mutableStateOf(false)
    private var showMicDisclosure by mutableStateOf(false)

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            SessionState.screen.value = ScreenState.STARTING
            ContextCompat.startForegroundService(this, ScreenCaptureService.startIntent(this, result.resultCode, result.data!!))
        } else SessionState.screen.value = ScreenState.OFF
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { startOverlayNow() }
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        SessionState.mic.value = if (granted) MicState.IDLE else MicState.ERROR
        if (granted) sendBroadcast(Intent(OverlayService.ACTION_START_VOICE).setPackage(packageName))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.action) {
            ACTION_CAPTURE_SCREEN -> showCaptureDisclosure = true
            ACTION_REQUEST_MIC -> showMicDisclosure = true
        }
        setContent {
            AIopterTheme {
                HomeScreen(
                    refresh = refresh,
                    onStart = ::beginOverlayFlow,
                    onStop = ::killAll,
                    onCapture = { showCaptureDisclosure = true },
                    onAppsAccess = { startActivity(Intent(this, AccessActivity::class.java)) },
                    onLogin = { startActivity(Intent(this, LoginActivity::class.java)) },
                    onLogout = { (application as AIopterApp).tokenVault.clear(); refresh++ }
                )
                if (showCaptureDisclosure) DisclosureDialog(
                    title = "Share your screen?",
                    body = "While screen sharing is on, Android may continuously provide frames internally. AIopter processes and sends one reduced-quality screen image only when you ask for screen-aware help. Frames are not permanently stored, protected screens stay protected, and Android keeps a visible sharing notification.",
                    confirm = "Continue",
                    onDismiss = { showCaptureDisclosure = false },
                    onConfirm = { showCaptureDisclosure = false; requestProjection() }
                )
                if (showMicDisclosure) DisclosureDialog(
                    title = "Use your microphone?",
                    body = "AIopter listens only after you tap the microphone and stops after speech recognition. Raw audio is not stored by AIopter.",
                    confirm = "Allow microphone",
                    onDismiss = { showMicDisclosure = false },
                    onConfirm = { showMicDisclosure = false; micPermission.launch(Manifest.permission.RECORD_AUDIO) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++
        if (pendingOverlayStart && Settings.canDrawOverlays(this)) {
            pendingOverlayStart = false
            requestNotificationThenStart()
        }
    }

    private fun beginOverlayFlow() {
        if (!Settings.canDrawOverlays(this)) {
            pendingOverlayStart = true
            SessionState.overlay.value = OverlayState.PERMISSION_MISSING
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else requestNotificationThenStart()
    }

    private fun requestNotificationThenStart() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else startOverlayNow()
    }

    private fun startOverlayNow() {
        if (!Settings.canDrawOverlays(this)) return
        ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
        SessionState.overlay.value = OverlayState.BUBBLE
    }

    private fun requestProjection() {
        SessionState.screen.value = ScreenState.REQUESTING_PERMISSION
        val manager = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun killAll() {
        // Service teardown synchronously invokes each onDestroy cancellation path.
        stopService(Intent(this, ScreenCaptureService::class.java).setAction(ScreenCaptureService.ACTION_STOP))
        stopService(Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_STOP_ALL))
        SessionState.screen.value = ScreenState.OFF
        SessionState.overlay.value = OverlayState.DISABLED
        SessionState.mic.value = MicState.IDLE
        SessionState.ai.value = AiState.CANCELLED
    }

    companion object {
        const val ACTION_CAPTURE_SCREEN = "io.aiopter.action.CAPTURE_SCREEN"
        const val ACTION_REQUEST_MIC = "io.aiopter.action.REQUEST_MIC"
    }
}

@Composable
private fun DisclosureDialog(title: String, body: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(body) }, confirmButton = { Button(onClick = onConfirm) { Text(confirm) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } })
}

@Composable
private fun HomeScreen(refresh: Int, onStart: () -> Unit, onStop: () -> Unit, onCapture: () -> Unit, onAppsAccess: () -> Unit, onLogin: () -> Unit, onLogout: () -> Unit) {
    val overlay by SessionState.overlay.collectAsStateWithLifecycle()
    val screen by SessionState.screen.collectAsStateWithLifecycle()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as AIopterApp
    val loggedIn = remember(refresh) { app.tokenVault.hasSession() }
    val appAwareness = remember(refresh) { ForegroundAppResolver(app).hasUsageAccess() }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF07101D), Color(0xFF080B12))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(28.dp))
            RotorLogo(Modifier.size(86.dp), spinning = overlay == OverlayState.BUBBLE || overlay == OverlayState.PANEL)
            Spacer(Modifier.height(14.dp))
            Text("AIOPTER", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("SEE. ASK. DO. ANYWHERE", color = Color(0xFF7E9AB8), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(34.dp))
            Surface(color = Color(0xB3141B28), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text(if (overlay == OverlayState.BUBBLE || overlay == OverlayState.PANEL) "Assistant is active" else "Ready when you are", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("A private, user-controlled assistant that stays available above your apps.", color = Color(0xFFA8B8CA))
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = if (overlay == OverlayState.BUBBLE || overlay == OverlayState.PANEL) onStop else onStart, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        Text(if (overlay == OverlayState.BUBBLE || overlay == OverlayState.PANEL) "Stop floating assistant" else "Start floating assistant")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            StatusRow("Floating overlay", if (overlay == OverlayState.BUBBLE || overlay == OverlayState.PANEL) "On" else "Off")
            StatusRow("Screen sharing", when {
                screen == ScreenState.BLOCKED_FOR_APP -> "On · current app blocked"
                screen.isSessionActive -> "On · visible notification"
                else -> "Off by default"
            })
            StatusRow("Microphone", "Tap-to-talk only")
            StatusRow("App awareness", if (appAwareness) "On · rules enforced" else "Off · frames blocked")
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onCapture, modifier = Modifier.fillMaxWidth()) { Text(if (screen.isSessionActive) "Screen sharing is active" else "Enable screen sharing") }
            OutlinedButton(onClick = onAppsAccess, modifier = Modifier.fillMaxWidth()) { Text("Manage Apps & Access") }
            Spacer(Modifier.weight(1f))
            Surface(color = Color(0x99111822), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (loggedIn) "Secure account connected" else "Connect your account", fontWeight = FontWeight.SemiBold)
                        Text(if (loggedIn) "Session stored with Android Keystore" else "Sign in to use protected AI requests", color = Color(0xFF8EA2B8), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = if (loggedIn) onLogout else onLogin) { Text(if (loggedIn) "Sign out" else "Sign in") }
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(18.dp))
        }
    }
}

@Composable private fun StatusRow(label: String, state: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(if (state.startsWith("On")) Color(0xFF4DE2B0) else Color(0xFF516276), RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(12.dp)); Text(label, Modifier.weight(1f)); Text(state, color = Color(0xFF8EA2B8), style = MaterialTheme.typography.bodySmall)
    }
}
