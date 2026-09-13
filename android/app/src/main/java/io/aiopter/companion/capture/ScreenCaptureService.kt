package io.aiopter.companion.capture

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import io.aiopter.companion.*
import io.aiopter.companion.access.AccessPolicyStore
import io.aiopter.companion.access.ForegroundAppResolver
import io.aiopter.companion.ai.ChatMessage
import io.aiopter.companion.ai.StreamEvent
import io.aiopter.policy.AccessDecision
import io.aiopter.policy.AccessPolicy
import io.aiopter.policy.FrameSafety
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

private data class CaptureRequest(
    val prompt: String,
    val requestedAtMillis: Long = SystemClock.elapsedRealtime(),
    val approvedPackage: String? = null,
)

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private val pendingCapture = AtomicReference<CaptureRequest?>(null)
    private val awaitingApproval = AtomicReference<CaptureRequest?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val policyStore by lazy { AccessPolicyStore(this) }
    private val foregroundApps by lazy { ForegroundAppResolver(this) }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (projection == null) startProjection(intent)
            ACTION_CAPTURE_AND_ASK -> intent.getStringExtra(EXTRA_PROMPT)?.take(4000)?.takeIf(String::isNotBlank)?.let {
                awaitingApproval.set(null)
                pendingCapture.set(CaptureRequest(it))
            }
            ACTION_ALLOW_ONCE -> approveOnce(intent.getStringExtra(EXTRA_PACKAGE))
            ACTION_DENY_ONCE -> {
                awaitingApproval.set(null)
                SessionState.screen.value = ScreenState.SHARING
                broadcast("access_denied", "Screen context was not sent.")
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun approveOnce(expectedPackage: String?) {
        val request = awaitingApproval.getAndSet(null)
        val currentPackage = foregroundApps.currentPackage()
        if (request == null || !AccessPolicy.acceptsOneTimeApproval(
                expectedPackage = expectedPackage,
                currentPackage = currentPackage,
                requestedAtMillis = request.requestedAtMillis,
                approvedAtMillis = SystemClock.elapsedRealtime(),
            )) {
            SessionState.screen.value = ScreenState.BLOCKED_FOR_APP
            broadcast("blocked", "This approval expired or the foreground app changed, so no screen context was sent.")
            return
        }
        SessionState.screen.value = ScreenState.SHARING
        pendingCapture.set(request.copy(approvedPackage = expectedPackage))
    }

    private fun startProjection(intent: Intent) {
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DATA)
        if (resultData == null) { fail("Screen permission was not received."); return }
        val manager = getSystemService(MediaProjectionManager::class.java)
        projection = manager.getMediaProjection(intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED), resultData)
        projection?.registerCallback(
            object : MediaProjection.Callback() { override fun onStop() { stopSelf() } },
            Handler(Looper.getMainLooper()),
        )
        val metrics = resources.displayMetrics
        val sourceWidth = metrics.widthPixels
        val sourceHeight = metrics.heightPixels
        val scale = minOf(1f, 720f / sourceWidth.toFloat())
        val width = (sourceWidth * scale).roundToInt().coerceAtLeast(320)
        val height = (sourceHeight * scale).roundToInt().coerceAtLeast(320)
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener({ imageReader ->
                val request = pendingCapture.getAndSet(null)
                val image = imageReader.acquireLatestImage() ?: return@setOnImageAvailableListener
                if (request == null) image.close() else scope.launch { enforcePolicyAndAnalyze(request, image, width, height) }
            }, Handler(Looper.getMainLooper()))
        }
        display = projection?.createVirtualDisplay(
            "AIopterSingleFrame", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, null,
        )
        SessionState.screen.value = ScreenState.SHARING
    }

    private suspend fun enforcePolicyAndAnalyze(request: CaptureRequest, image: Image, width: Int, height: Int) {
        val currentPackage = foregroundApps.currentPackage()
        if (currentPackage == null) {
            image.close()
            SessionState.screen.value = ScreenState.BLOCKED_FOR_APP
            broadcast("usage_access_required", "App awareness is off. Screen context stays blocked until you enable it in Apps & Access.")
            return
        }
        val rule = policyStore.ruleFor(currentPackage)
        val hasFreshApproval = AccessPolicy.acceptsOneTimeApproval(
            expectedPackage = request.approvedPackage,
            currentPackage = currentPackage,
            requestedAtMillis = request.requestedAtMillis,
            approvedAtMillis = SystemClock.elapsedRealtime(),
        )
        val decision = if (hasFreshApproval) AccessDecision.ALLOW else AccessPolicy.decide(rule, true)
        when (decision) {
            AccessDecision.BLOCK -> {
                image.close()
                SessionState.screen.value = ScreenState.BLOCKED_FOR_APP
                broadcast("blocked", "Screen access blocked for ${foregroundApps.labelFor(currentPackage)}.", currentPackage)
            }
            AccessDecision.ASK -> {
                image.close()
                awaitingApproval.set(request)
                SessionState.screen.value = ScreenState.BLOCKED_FOR_APP
                broadcast("access_request", foregroundApps.labelFor(currentPackage), currentPackage)
            }
            AccessDecision.ALLOW -> processImage(request.prompt, image, width, height)
        }
    }

    private suspend fun processImage(prompt: String, image: Image, width: Int, height: Int) = withContext(Dispatchers.Default) {
        var bitmap: Bitmap? = null
        var cropped: Bitmap? = null
        try {
            val plane = image.planes[0]
            val rowPadding = plane.rowStride - plane.pixelStride * width
            val paddedWidth = width + rowPadding / plane.pixelStride
            bitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888).also { it.copyPixelsFromBuffer(plane.buffer) }
            image.close()
            cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            if (cropped !== bitmap) { bitmap.recycle(); bitmap = null }
            if (isProbablyProtected(cropped)) {
                SessionState.screen.value = ScreenState.BLOCKED_FOR_APP
                broadcast("protected", "This screen is protected or blank, so AIopter did not send it.")
                return@withContext
            }
            val jpeg = ByteArrayOutputStream().use { output ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 74, output)
                output.toByteArray()
            }
            SessionState.screen.value = ScreenState.SHARING
            analyze(prompt, jpeg)
        } catch (_: Exception) {
            runCatching { image.close() }
            broadcast("error", "AIopter could not safely prepare this screen.")
        } finally {
            cropped?.recycle()
            if (bitmap?.isRecycled == false) bitmap?.recycle()
        }
    }

    private fun isProbablyProtected(bitmap: Bitmap): Boolean {
        val step = maxOf(8, minOf(bitmap.width, bitmap.height) / 48)
        val sampledWidth = (bitmap.width + step - 1) / step
        val sampledHeight = (bitmap.height + step - 1) / step
        val samples = IntArray(sampledWidth * sampledHeight)
        var index = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                samples[index++] = bitmap.getPixel(x, y)
                x += step
            }
            y += step
        }
        return FrameSafety.isProbablyProtected(if (index == samples.size) samples else samples.copyOf(index))
    }

    private fun analyze(prompt: String, jpeg: ByteArray) {
        SessionState.ai.value = AiState.SENDING
        scope.launch {
            try {
                val app = application as AIopterApp
                app.backendClient.streamChat(listOf(ChatMessage("user", prompt)), jpeg) { event ->
                    when (event) {
                        is StreamEvent.Delta -> broadcast("delta", event.text)
                        is StreamEvent.Error -> broadcast("error", event.message)
                        StreamEvent.Done -> broadcast("done", "")
                        is StreamEvent.Action -> Unit
                    }
                }
            } finally {
                jpeg.fill(0)
            }
        }
    }

    private fun broadcast(type: String, text: String, packageName: String? = null) {
        sendBroadcast(
            Intent(ACTION_ASSISTANT_EVENT).setPackage(this.packageName)
                .putExtra(EXTRA_EVENT_TYPE, type)
                .putExtra(EXTRA_TEXT, text)
                .putExtra(EXTRA_PACKAGE, packageName),
        )
    }

    private fun fail(message: String) {
        SessionState.screen.value = ScreenState.ERROR
        broadcast("error", message)
        stopSelf()
    }

    override fun onDestroy() {
        SessionState.screen.value = ScreenState.STOPPING
        pendingCapture.set(null)
        awaitingApproval.set(null)
        reader?.setOnImageAvailableListener(null, null)
        display?.release()
        reader?.close()
        projection?.stop()
        display = null
        reader = null
        projection = null
        scope.cancel()
        SessionState.screen.value = ScreenState.OFF
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.capture_channel_name), NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_logo)
        .setContentTitle("Screen sharing is on")
        .setContentText("App rules are checked before AIopter sends a frame.")
        .setOngoing(true)
        .addAction(0, "Stop sharing", PendingIntent.getService(this, 10, Intent(this, ScreenCaptureService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        .build()

    companion object {
        const val ACTION_START = "io.aiopter.capture.START"
        const val ACTION_STOP = "io.aiopter.capture.STOP"
        const val ACTION_CAPTURE_AND_ASK = "io.aiopter.capture.ASK"
        const val ACTION_ALLOW_ONCE = "io.aiopter.capture.ALLOW_ONCE"
        const val ACTION_DENY_ONCE = "io.aiopter.capture.DENY_ONCE"
        const val ACTION_ASSISTANT_EVENT = "io.aiopter.capture.EVENT"
        const val EXTRA_PROMPT = "prompt"
        const val EXTRA_EVENT_TYPE = "event_type"
        const val EXTRA_TEXT = "text"
        const val EXTRA_PACKAGE = "package_name"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_DATA = "result_data"
        private const val CHANNEL = "aiopter_capture"
        private const val NOTIFICATION_ID = 42

        fun startIntent(context: Context, resultCode: Int, data: Intent) = Intent(context, ScreenCaptureService::class.java)
            .setAction(ACTION_START)
            .putExtra(EXTRA_RESULT_CODE, resultCode)
            .putExtra(EXTRA_DATA, data)
    }
}
