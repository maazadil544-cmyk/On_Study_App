package com.example.adsterra

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Message
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdsterraBannerAd(
    modifier: Modifier = Modifier,
    adAssetUrl: String = "file:///android_asset/ad.html"
) {
    val context = LocalContext.current
    var webViewInstance: WebView? = remember { null }
    val popupWebViews = remember { mutableListOf<WebView>() }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.let { wv ->
                AdsterraManager.unregisterWebView(wv)
                wv.stopLoading()
                wv.destroy()
            }
            popupWebViews.forEach { popup ->
                popup.stopLoading()
                popup.destroy()
            }
            popupWebViews.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.TRANSPARENT)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                        cacheMode = WebSettings.LOAD_DEFAULT
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mediaPlaybackRequiresUserGesture = false
                    }

                    // Bridge for JavaScript popunder window.open hooks
                    addJavascriptInterface(AdsterraManager.JsBridge(ctx), "AndroidBridge")

                    // Open links in external browser when user clicks ad
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                AdsterraManager.openExternalUrl(ctx, url)
                                return true
                            }
                            return false
                        }
                    }

                    // Handle Popunder window.open calls with strong reference retention
                    webChromeClient = object : WebChromeClient() {
                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            val tempWebView = WebView(ctx)
                            tempWebView.settings.javaScriptEnabled = true
                            popupWebViews.add(tempWebView)

                            tempWebView.webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    if (!url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://")) && !url.contains("about:blank")) {
                                        AdsterraManager.openExternalUrl(ctx, url)
                                        view?.stopLoading()
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        AdsterraManager.openExternalUrl(ctx, url)
                                        return true
                                    }
                                    return false
                                }
                            }

                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            transport?.webView = tempWebView
                            resultMsg?.sendToTarget()
                            return true
                        }
                    }

                    AdsterraManager.registerWebView(this)
                    webViewInstance = this
                    loadUrl(adAssetUrl)
                }
            }
        )
    }
}
