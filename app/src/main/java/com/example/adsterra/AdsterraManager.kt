package com.example.adsterra

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.lang.ref.WeakReference

object AdsterraManager {
    private const val TAG = "AdsterraManager"

    // Adsterra Direct Link (Smartlink) URL: If provided, triggers instantly on user action
    var directLinkUrl: String = ""

    // Reference to the active banner WebView to trigger Popunder via JavaScript & touch events
    private var activeWebViewRef: WeakReference<WebView>? = null

    // Reference to application context for opening ad URLs
    private var appContextRef: WeakReference<Context>? = null

    fun initContext(context: Context) {
        if (appContextRef == null || appContextRef?.get() == null) {
            appContextRef = WeakReference(context.applicationContext)
        }
    }

    fun registerWebView(webView: WebView) {
        activeWebViewRef = WeakReference(webView)
        initContext(webView.context)
    }

    fun unregisterWebView(webView: WebView) {
        if (activeWebViewRef?.get() == webView) {
            activeWebViewRef = null
        }
    }

    /**
     * Triggers the Popunder ad.
     * 1. Dispatches actual hardware MotionEvent down & up to the WebView so ad network sees a trusted touch.
     * 2. Evaluates triggerPopunder() JavaScript inside the active ad.html WebView.
     * 3. If direct link URL is set, opens it directly.
     */
    fun triggerPopunder(context: Context) {
        try {
            initContext(context)
            val webView = activeWebViewRef?.get()

            if (webView != null) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        Log.d(TAG, "Simulating hardware touch on Popunder WebView...")
                        val now = SystemClock.uptimeMillis()
                        val w = webView.width.toFloat().coerceAtLeast(100f)
                        val h = webView.height.toFloat().coerceAtLeast(40f)
                        val x = w / 2f
                        val y = h / 2f

                        // Hardware-level motion event
                        val downEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
                        val upEvent = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, x, y, 0)

                        webView.dispatchTouchEvent(downEvent)
                        webView.dispatchTouchEvent(upEvent)

                        downEvent.recycle()
                        upEvent.recycle()

                        // Also call JavaScript trigger
                        webView.evaluateJavascript("triggerPopunder();", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error evaluating triggerPopunder", e)
                    }
                }
            } else {
                Log.d(TAG, "No active WebView found for Popunder")
            }

            // Direct link fallback
            if (directLinkUrl.isNotBlank()) {
                openExternalUrl(context, directLinkUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in triggerPopunder", e)
        }
    }

    fun openExternalUrl(context: Context?, url: String) {
        if (url.isBlank() || url.contains("about:blank")) return
        try {
            val targetContext = context ?: appContextRef?.get() ?: return
            Log.d(TAG, "Opening ad URL in browser: $url")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            targetContext.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open ad URL: $url", e)
        }
    }

    class JsBridge(private val context: Context) {
        @JavascriptInterface
        fun onPopunderTriggered(url: String) {
            Log.d(TAG, "JsBridge: onPopunderTriggered received $url")
            Handler(Looper.getMainLooper()).post {
                openExternalUrl(context, url)
            }
        }
    }
}
