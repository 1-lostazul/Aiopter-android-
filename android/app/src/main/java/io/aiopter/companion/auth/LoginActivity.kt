package io.aiopter.companion.auth

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import io.aiopter.companion.AIopterApp
import io.aiopter.companion.BuildConfig
import io.aiopter.policy.UriSafety
import org.json.JSONObject

class LoginActivity : ComponentActivity() {
    private lateinit var mainWebView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainWebView = secureWebView().apply {
            addJavascriptInterface(AuthBridge(), "AIopterAuth")
            webChromeClient = popupChromeClient()
            loadUrl("${BuildConfig.API_BASE_URL.trimEnd('/')}/auth/mobile")
        }
        setContentView(mainWebView)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun secureWebView(allowExternalHttps: Boolean = false) = WebView(this).apply {
        setBackgroundColor(Color.rgb(8, 11, 18))
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                val allowed = if (allowExternalHttps) request.url.scheme == "https"
                    else UriSafety.isAllowed(request.url.toString(), BuildConfig.API_BASE_URL)
                return !allowed
            }
        }
    }

    private fun popupChromeClient() = object : WebChromeClient() {
        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
            if (!isUserGesture) return false
            // The OAuth popup has no JavaScript bridge; it may navigate across HTTPS identity-provider origins.
            val popup = secureWebView(allowExternalHttps = true)
            val dialog = Dialog(this@LoginActivity).apply { setContentView(popup); window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); setOnDismissListener { popup.destroy() }; show() }
            popup.webChromeClient = object : WebChromeClient() { override fun onCloseWindow(window: WebView?) { dialog.dismiss() } }
            (resultMsg?.obj as? WebView.WebViewTransport)?.webView = popup
            resultMsg?.sendToTarget(); return true
        }
    }

    inner class AuthBridge {
        @JavascriptInterface fun complete(payload: String) {
            runCatching {
                val json = JSONObject(payload); val token = json.getString("token")
                require(token.length in 16..8192)
                (application as AIopterApp).tokenVault.save(token)
                runOnUiThread { Toast.makeText(this@LoginActivity, "Account connected", Toast.LENGTH_SHORT).show(); finish() }
            }.onFailure { runOnUiThread { Toast.makeText(this@LoginActivity, "Sign-in response was invalid", Toast.LENGTH_LONG).show() } }
        }
    }

    override fun onDestroy() { mainWebView.removeJavascriptInterface("AIopterAuth"); mainWebView.destroy(); super.onDestroy() }
}
