package io.alopter.companion.overlay

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import io.alopter.companion.*
import io.alopter.companion.ai.ChatMessage
import io.alopter.companion.ai.ProposedAction
import io.alopter.companion.ai.StreamEvent
import io.alopter.companion.capture.ScreenCaptureService
import kotlinx.coroutines.*
import okhttp3.Call
import kotlin.math.abs

class OverlayService : Service(), RecognitionListener {
    private lateinit var windowManager: WindowManager
    private var root: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var panelResponse: TextView? = null
    private var panelInput: EditText? = null
    private var statusText: TextView? = null
    private var actionButton: Button? = null
    private var pendingAction: ProposedAction? = null
    private var expanded = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var requestJob: Job? = null
    private var activeCall: Call? = null
    private var speech: SpeechRecognizer? = null
    private val history = mutableListOf<ChatMessage>()

    private val captureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(ScreenCaptureService.EXTRA_EVENT_TYPE)) {
                "delta" -> appendResponse(intent.getStringExtra(ScreenCaptureService.EXTRA_TEXT).orEmpty())
                "done" -> { statusText?.text = "Ready"; SessionState.ai.value = AiState.COMPLETE }
                "error" -> showError(intent.getStringExtra(ScreenCaptureService.EXTRA_TEXT) ?: "Screen analysis failed")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
        windowManager = getSystemService(WindowManager::class.java)
        val filter = IntentFilter(ScreenCaptureService.ACTION_ASSISTANT_EVENT)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(captureReceiver, filter, RECEIVER_NOT_EXPORTED) else @Suppress("DEPRECATION") registerReceiver(captureReceiver, filter)
        if (!Settings.canDrawOverlays(this)) { SessionState.overlay.value = OverlayState.PERMISSION_MISSING; stopSelf(); return }
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_VOICE) startVoiceRecognition()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showBubble() {
        removeCurrent()
        expanded = false
        val size = dp(68)
        val bubble = RotorBubbleView(this)
        val savedY = getSharedPreferences("overlay", MODE_PRIVATE).getInt("y", dp(180))
        params = baseParams(size, size).apply { x = 0; y = savedY; gravity = Gravity.TOP or Gravity.START; flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE }
        attachDragBehavior(bubble)
        root = bubble
        windowManager.addView(bubble, params)
        SessionState.overlay.value = OverlayState.BUBBLE
    }

    private fun attachDragBehavior(view: View) {
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        view.setOnTouchListener { _, event ->
            val lp = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = lp.x; startY = lp.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt()
                    moved = moved || abs(dx) > dp(6) || abs(dy) > dp(6)
                    lp.x = (startX + dx).coerceIn(0, resources.displayMetrics.widthPixels - view.width)
                    lp.y = (startY + dy).coerceIn(dp(24), resources.displayMetrics.heightPixels - view.height - dp(48))
                    windowManager.updateViewLayout(view, lp); true
                }
                MotionEvent.ACTION_UP -> { if (!moved) showPanel() else snapToEdge(view); true }
                else -> false
            }
        }
    }

    private fun snapToEdge(view: View) {
        val lp = params ?: return
        val edge = if (lp.x + view.width / 2 < resources.displayMetrics.widthPixels / 2) 0 else resources.displayMetrics.widthPixels - view.width
        val start = lp.x
        android.animation.ValueAnimator.ofInt(start, edge).apply {
            duration = 220
            addUpdateListener { lp.x = it.animatedValue as Int; runCatching { windowManager.updateViewLayout(view, lp) } }
            doOnEndCompat { getSharedPreferences("overlay", MODE_PRIVATE).edit().putInt("y", lp.y).apply() }
            start()
        }
    }

    private fun showPanel() {
        removeCurrent(); expanded = true
        val panel = buildPanel()
        params = baseParams(dp(350), WindowManager.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END; x = dp(10); y = 0
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        root = panel; windowManager.addView(panel, params)
        SessionState.overlay.value = OverlayState.PANEL
    }

    private fun buildPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(16)); elevation = dp(20).toFloat()
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(19, 29, 45), Color.rgb(8, 13, 23))).apply { cornerRadius = dp(28).toFloat(); setStroke(dp(1), Color.rgb(49, 91, 132)) }
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(RotorBubbleView(this), LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(TextView(this).apply { text = "  ALOPTER"; setTextColor(Color.WHITE); textSize = 18f; typeface = android.graphics.Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(button("—") { showBubble() }, LinearLayout.LayoutParams(dp(46), dp(46)))
        header.addView(button("KILL", danger = true) { stopEverything() }, LinearLayout.LayoutParams(dp(62), dp(46)))
        panel.addView(header)
        statusText = TextView(this).apply { text = stateSummary(); setTextColor(Color.rgb(124, 181, 219)); textSize = 12f; setPadding(0, dp(8), 0, dp(8)) }
        panel.addView(statusText)
        panelResponse = TextView(this).apply { text = "What can I help with?\n\nScreen access is off until you turn it on."; setTextColor(Color.rgb(232, 241, 250)); textSize = 15f; setPadding(dp(12), dp(12), dp(12), dp(12)); setTextIsSelectable(true); background = rounded(Color.rgb(15, 23, 36), 18) }
        panel.addView(ScrollView(this).apply { addView(panelResponse) }, LinearLayout.LayoutParams(-1, dp(230)))
        actionButton = button("Review action") { pendingAction?.let(::openApprovedAction) }.apply { visibility = View.GONE }
        panel.addView(actionButton, LinearLayout.LayoutParams(-1, dp(46)))
        panelInput = EditText(this).apply { hint = "Ask anything…"; setHintTextColor(Color.rgb(104, 124, 148)); setTextColor(Color.WHITE); textSize = 15f; minLines = 1; maxLines = 4; background = rounded(Color.rgb(20, 30, 46), 18); setPadding(dp(14), dp(8), dp(14), dp(8)) }
        panel.addView(panelInput, LinearLayout.LayoutParams(-1, WindowManager.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        controls.addView(button("Mic") { requestVoice() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) })
        controls.addView(button(if (SessionState.screen.value == ScreenState.SHARING) "See: ON" else "See: OFF") { toggleScreen() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) })
        controls.addView(button("Send") { submit(SessionState.screen.value == ScreenState.SHARING) }, LinearLayout.LayoutParams(0, dp(48), 1f))
        panel.addView(controls, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(6) })
        return panel
    }

    private fun submit(withScreen: Boolean) {
        val prompt = panelInput?.text?.toString()?.trim().orEmpty()
        if (prompt.isBlank()) return
        panelInput?.text?.clear(); panelResponse?.text = ""; pendingAction = null; actionButton?.visibility = View.GONE
        history += ChatMessage("user", prompt); statusText?.text = if (withScreen) "Capturing one frame…" else "Thinking…"
        if (withScreen) {
            if (SessionState.screen.value != ScreenState.SHARING) { showError("Turn on screen sharing first."); return }
            root?.visibility = View.INVISIBLE
            scope.launch {
                delay(280)
                startService(Intent(this@OverlayService, ScreenCaptureService::class.java).setAction(ScreenCaptureService.ACTION_CAPTURE_AND_ASK).putExtra(ScreenCaptureService.EXTRA_PROMPT, prompt))
                delay(650)
                root?.visibility = View.VISIBLE
            }
        } else ask(history.toList(), null)
    }

    private fun ask(messages: List<ChatMessage>, image: ByteArray?) {
        requestJob?.cancel(); activeCall?.cancel(); SessionState.ai.value = AiState.SENDING
        var complete = ""
        requestJob = scope.launch {
            val app = application as AlopterApp
            activeCall = app.backendClient.streamChat(messages, image) { event ->
                scope.launch {
                    when (event) {
                        is StreamEvent.Delta -> { complete += event.text; appendResponse(event.text); SessionState.ai.value = AiState.STREAMING }
                        is StreamEvent.Action -> showAction(event.action)
                        is StreamEvent.Error -> showError(event.message)
                        StreamEvent.Done -> { if (complete.isNotBlank()) history += ChatMessage("assistant", complete); statusText?.text = "Ready"; SessionState.ai.value = AiState.COMPLETE }
                    }
                }
            }
        }
    }

    private fun showAction(action: ProposedAction) {
        pendingAction = action; actionButton?.text = "Review: ${action.label}"; actionButton?.visibility = View.VISIBLE
    }

    private fun openApprovedAction(action: ProposedAction) {
        AlertDialog.Builder(this).setTitle("Open another app?").setMessage("Alopter prepared “${action.label}”. Nothing happens until you approve.")
            .setNegativeButton("Cancel", null).setPositiveButton("Open") { _, _ ->
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(action.uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { showError("No compatible app is installed.") }
            }.create().also { it.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY); it.show() }
    }

    private fun toggleScreen() {
        if (SessionState.screen.value == ScreenState.SHARING) {
            stopService(Intent(this, ScreenCaptureService::class.java)); SessionState.screen.value = ScreenState.OFF; statusText?.text = stateSummary()
        } else startActivity(Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_CAPTURE_SCREEN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    private fun requestVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startVoiceRecognition()
        else startActivity(Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_REQUEST_MIC).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    private fun startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { showError("Speech recognition is unavailable on this device."); return }
        speech?.destroy(); speech = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
        SessionState.mic.value = MicState.LISTENING; statusText?.text = "Listening · tap KILL to stop"
        speech?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true))
    }

    private fun stopEverything() {
        requestJob?.cancel(); activeCall?.cancel(); speech?.cancel(); speech?.destroy(); speech = null
        stopService(Intent(this, ScreenCaptureService::class.java)); SessionState.screen.value = ScreenState.OFF; SessionState.mic.value = MicState.IDLE; SessionState.ai.value = AiState.CANCELLED
        stopSelf()
    }

    private fun appendResponse(text: String) { panelResponse?.append(text); statusText?.text = "Answering…" }
    private fun showError(message: String) { panelResponse?.text = message; statusText?.text = "Needs attention"; SessionState.ai.value = AiState.ERROR }
    private fun stateSummary() = "Screen ${if (SessionState.screen.value == ScreenState.SHARING) "ON" else "OFF"}  •  Mic ${if (SessionState.mic.value == MicState.LISTENING) "ON" else "idle"}"

    private fun button(label: String, danger: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 11f; setTextColor(Color.WHITE); isAllCaps = false; background = rounded(if (danger) Color.rgb(141, 42, 52) else Color.rgb(31, 73, 112), 14); setOnClickListener { action() }
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun baseParams(width: Int, height: Int) = WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun removeCurrent() { root?.let { runCatching { windowManager.removeView(it) } }; root = null }

    override fun onDestroy() {
        runCatching { unregisterReceiver(captureReceiver) }; removeCurrent(); requestJob?.cancel(); activeCall?.cancel(); speech?.destroy(); scope.cancel()
        SessionState.overlay.value = OverlayState.DISABLED
        super.onDestroy()
    }

    private fun createChannel() { if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.overlay_channel_name), NotificationManager.IMPORTANCE_LOW)) }
    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_logo).setContentTitle("Alopter is active").setContentText(getString(R.string.overlay_notification)).setOngoing(true).setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()

    override fun onReadyForSpeech(params: Bundle?) { statusText?.text = "Listening…" }
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { SessionState.mic.value = MicState.TRANSCRIBING; statusText?.text = "Transcribing…" }
    override fun onError(error: Int) { SessionState.mic.value = MicState.ERROR; showError("I couldn't hear that. Tap Mic to try again.") }
    override fun onResults(results: Bundle?) { val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); panelInput?.setText(text); SessionState.mic.value = MicState.IDLE; statusText?.text = "Ready" }
    override fun onPartialResults(partialResults: Bundle?) { partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { panelInput?.setText(it) } }
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    companion object { const val ACTION_START_VOICE = "io.alopter.action.START_VOICE"; private const val CHANNEL = "alopter_overlay"; private const val NOTIFICATION_ID = 41 }
}

private fun android.animation.ValueAnimator.doOnEndCompat(block: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() { override fun onAnimationEnd(animation: android.animation.Animator) = block() })
}
