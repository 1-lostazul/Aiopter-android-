package io.alopter.companion.capture

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import io.alopter.companion.*
import io.alopter.companion.ai.ChatMessage
import io.alopter.companion.ai.StreamEvent
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private val pendingPrompt = AtomicReference<String?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate(); createChannel(); startForeground(NOTIFICATION_ID, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (projection == null) startProjection(intent)
            ACTION_CAPTURE_AND_ASK -> {
                val prompt = intent.getStringExtra(EXTRA_PROMPT)?.take(4000)
                if (!prompt.isNullOrBlank()) pendingPrompt.set(prompt)
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startProjection(intent: Intent) {
        val resultData = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_DATA, Intent::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DATA)
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
                val prompt = pendingPrompt.getAndSet(null)
                val image = imageReader.acquireLatestImage() ?: return@setOnImageAvailableListener
                if (prompt == null) { image.close(); return@setOnImageAvailableListener }
                val plane = image.planes[0]
                val rowPadding = plane.rowStride - plane.pixelStride * width
                val paddedWidth = width + rowPadding / plane.pixelStride
                val bitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(plane.buffer); image.close()
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                if (cropped !== bitmap) bitmap.recycle()
                val out = ByteArrayOutputStream()
                cropped.compress(Bitmap.CompressFormat.JPEG, 74, out); cropped.recycle()
                analyze(prompt, out.toByteArray())
            }, null)
        }
        display = projection?.createVirtualDisplay("AlopterSingleFrame", width, height, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, null)
        SessionState.screen.value = ScreenState.SHARING
    }

    private fun analyze(prompt: String, jpeg: ByteArray) {
        SessionState.ai.value = AiState.SENDING
        scope.launch {
            val app = application as AlopterApp
            app.backendClient.streamChat(listOf(ChatMessage("user", prompt)), jpeg) { event ->
                when (event) {
                    is StreamEvent.Delta -> broadcast("delta", event.text)
                    is StreamEvent.Error -> broadcast("error", event.message)
                    StreamEvent.Done -> broadcast("done", "")
                    is StreamEvent.Action -> Unit
                }
            }
            jpeg.fill(0)
        }
    }

    private fun broadcast(type: String, text: String) {
        sendBroadcast(Intent(ACTION_ASSISTANT_EVENT).setPackage(packageName).putExtra(EXTRA_EVENT_TYPE, type).putExtra(EXTRA_TEXT, text))
    }
    private fun fail(message: String) { SessionState.screen.value = ScreenState.ERROR; broadcast("error", message); stopSelf() }

    override fun onDestroy() {
        SessionState.screen.value = ScreenState.STOPPING
        pendingPrompt.set(null); reader?.setOnImageAvailableListener(null, null); display?.release(); reader?.close(); projection?.stop()
        display = null; reader = null; projection = null; scope.cancel(); SessionState.screen.value = ScreenState.OFF
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() { if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.capture_channel_name), NotificationManager.IMPORTANCE_LOW)) }
    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_logo).setContentTitle("Screen sharing is on").setContentText("Alopter captures a frame only when you ask.").setOngoing(true)
        .addAction(0, "Stop sharing", PendingIntent.getService(this, 10, Intent(this, ScreenCaptureService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()

    companion object {
        const val ACTION_START = "io.alopter.capture.START"
        const val ACTION_STOP = "io.alopter.capture.STOP"
        const val ACTION_CAPTURE_AND_ASK = "io.alopter.capture.ASK"
        const val ACTION_ASSISTANT_EVENT = "io.alopter.capture.EVENT"
        const val EXTRA_PROMPT = "prompt"
        const val EXTRA_EVENT_TYPE = "event_type"
        const val EXTRA_TEXT = "text"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_DATA = "result_data"
        private const val CHANNEL = "alopter_capture"
        private const val NOTIFICATION_ID = 42
        fun startIntent(context: Context, resultCode: Int, data: Intent) = Intent(context, ScreenCaptureService::class.java).setAction(ACTION_START).putExtra(EXTRA_RESULT_CODE, resultCode).putExtra(EXTRA_DATA, data)
    }
}
