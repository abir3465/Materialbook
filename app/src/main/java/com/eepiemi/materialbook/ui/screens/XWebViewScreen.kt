package com.eepiemi.materialbook.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri

private data class XDestination(val label: String, val url: String)

private val destinations = listOf(
    XDestination("Home", "https://x.com/home"),
    XDestination("Search", "https://x.com/explore"),
    XDestination("Alerts", "https://x.com/notifications"),
    XDestination("Messages", "https://x.com/messages")
)

@Composable
fun XWebViewScreen(startUrl: String = "https://x.com/") {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    pageProgress = newProgress
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = routeUrl(request.url.toString(), view)

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    routeUrl(url, view)
            }

            loadUrl(startUrl)
        }
    }
    var pageProgress by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf(startUrl) }
    var selectedDestination by remember { mutableIntStateOf(0) }

    // Keep progress and address state in sync with ordinary page navigations.
    webView.webViewClient = remember {
        object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                if (url != null) currentUrl = url
                pageProgress = 0
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (url != null) currentUrl = url
                pageProgress = 100
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = routeUrl(request.url.toString(), view)

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                routeUrl(url, view)
        }
    }

    BackHandler(enabled = webView.canGoBack()) {
        webView.goBack()
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("X Web", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { webView.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload page")
                    }
                    IconButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, currentUrl.toUri()))
                        }
                    }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, destination ->
                    val icon = when (index) {
                        0 -> Icons.Default.Home
                        1 -> Icons.Default.Search
                        2 -> Icons.Default.Notifications
                        else -> Icons.Default.MailOutline
                    }
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = {
                            selectedDestination = index
                            webView.loadUrl(destination.url)
                        },
                        icon = { Icon(icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )
            if (pageProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            if (pageProgress == 0) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun routeUrl(url: String, webView: WebView): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return true
    val scheme = uri.scheme?.lowercase() ?: return true
    val host = uri.host?.lowercase()

    if (scheme == "https" && host != null && isXHost(host)) return false

    // Do not hand executable or local schemes to Android intents.
    if (scheme == "https" || scheme == "http" || scheme == "mailto" || scheme == "tel") {
        return try {
            webView.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            true
        }
    }
    return true
}

private fun isXHost(host: String): Boolean =
    host == "x.com" || host.endsWith(".x.com") ||
        host == "twitter.com" || host.endsWith(".twitter.com")
