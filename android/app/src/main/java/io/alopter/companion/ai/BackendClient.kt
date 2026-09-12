package io.alopter.companion.ai

import android.util.Base64
import io.alopter.companion.auth.TokenVault
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class BackendClient(private val baseUrl: String, private val tokenVault: TokenVault) {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS).callTimeout(120, TimeUnit.SECONDS).build()

    suspend fun streamChat(history: List<ChatMessage>, imageJpeg: ByteArray? = null, onEvent: (StreamEvent) -> Unit): Call =
        suspendCancellableCoroutine { continuation ->
            require(history.isNotEmpty())
            val messages = JSONArray().apply {
                history.takeLast(20).forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
            }
            val json = JSONObject()
                .put("requestId", UUID.randomUUID().toString())
                .put("messages", messages)
            imageJpeg?.let {
                json.put("image", JSONObject().put("mimeType", "image/jpeg").put("data", Base64.encodeToString(it, Base64.NO_WRAP)))
            }
            val request = Request.Builder().url("${baseUrl.trimEnd('/')}/api/v1/chat/stream")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .header("Accept", "text/event-stream")
                .apply { tokenVault.read()?.let { header("Authorization", "Bearer $it") } }
                .build()
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) {
                        if (!call.isCanceled()) onEvent(StreamEvent.Error(error.message ?: "Network unavailable"))
                        continuation.resume(call)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            if (!response.isSuccessful) {
                                val raw = response.body?.string().orEmpty()
                                val detail = runCatching { JSONObject(raw).optString("error") }.getOrNull()
                                onEvent(StreamEvent.Error(detail?.takeIf(String::isNotBlank) ?: "Alopter could not reach the assistant."))
                                return@use
                            }
                            val source = response.body?.source() ?: return@use
                            while (!source.exhausted() && !call.isCanceled()) {
                                val line = source.readUtf8Line() ?: break
                                if (!line.startsWith("data:")) continue
                                val payload = JSONObject(line.removePrefix("data:").trim())
                                when (payload.optString("type")) {
                                    "delta" -> onEvent(StreamEvent.Delta(payload.optString("text")))
                                    "action" -> onEvent(
                                        StreamEvent.Action(
                                            ProposedAction(
                                                payload.getString("id"),
                                                payload.getString("label"),
                                                payload.getString("uri"),
                                                payload.optInt("risk", 1),
                                            ),
                                        ),
                                    )
                                    "done" -> onEvent(StreamEvent.Done)
                                    "error" -> onEvent(StreamEvent.Error(payload.optString("message", "Assistant error")))
                                }
                            }
                        }
                    } catch (error: Exception) {
                        if (!call.isCanceled()) onEvent(StreamEvent.Error(error.message ?: "Invalid assistant response"))
                    } finally {
                        continuation.resume(call)
                    }
                }
            })
        }
}
