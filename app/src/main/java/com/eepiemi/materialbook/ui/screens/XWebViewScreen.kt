package com.eepiemi.materialbook.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri

private data class XDestination(val label: String, val url: String)

private val destinations = listOf(
    XDestination("Home", "https://x.com/home"),
    XDestination("Explore", "https://x.com/explore"),
    XDestination("Alerts", "https://x.com/notifications"),
    XDestination("Messages", "https://x.com/messages")
)

@Composable
fun XWebViewScreen(startUrl: String = "https://x.com/") {
    val context = LocalContext.current
    var pageProgress by remember { mutableIntStateOf(0) }
    var currentUrl by rememberSaveable { mutableStateOf(startUrl) }
    var selectedDestination by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var darkMode by rememberSaveable { mutableStateOf(false) }
    var allowThirdPartyCookies by rememberSaveable { mutableStateOf(false) }
    var swipeDistance by remember { mutableFloatStateOf(0f) }

    val webView = remember(context) {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true // Required by X's web app.
                domStorageEnabled = true
                loadsImagesAutomatically = true
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(this@apply, false)
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    pageProgress = newProgress
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    if (url != null) {
                        currentUrl = url
                        selectedDestination = destinationFor(url)
                    }
                    pageProgress = 0
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    if (url != null) currentUrl = url
                    pageProgress = 100
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: android.webkit.WebResourceError
                ) {
                    if (request.isForMainFrame) pageProgress = 100
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = handleNavigation(request.url, view)

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    handleNavigation(Uri.parse(url), view)
            }
        }
    }

    LaunchedEffect(webView, startUrl) {
        if (webView.url == null) webView.loadUrl(startUrl)
    }

    LaunchedEffect(allowThirdPartyCookies) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, allowThirdPartyCookies)
    }

    LaunchedEffect(darkMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webView.settings.isAlgorithmicDarkeningAllowed = darkMode
        }
    }

    BackHandler(enabled = webView.canGoBack()) { webView.goBack() }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("X Web") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(onClick = { webView.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
                if (pageProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 0.dp)
                    )
                }
            }
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { webView.loadUrl("https://x.com/compose/post") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("Post", modifier = Modifier.padding(horizontal = 14.dp))
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(selectedDestination) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            swipeDistance += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            if (swipeDistance < -90f) {
                                val next = (selectedDestination + 1).coerceAtMost(destinations.lastIndex)
                                if (next != selectedDestination) {
                                    selectedDestination = next
                                    webView.loadUrl(destinations[next].url)
                                }
                            } else if (swipeDistance > 90f) {
                                val previous = (selectedDestination - 1).coerceAtLeast(0)
                                if (previous != selectedDestination) {
                                    selectedDestination = previous
                                    webView.loadUrl(destinations[previous].url)
                                }
                            }
                            swipeDistance = 0f
                        },
                        onDragCancel = { swipeDistance = 0f }
                    )
                }
        ) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            if (pageProgress == 0) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("WebView settings") },
            text = {
                Column {
                    SettingSwitchRow(
                        title = "Darken website",
                        subtitle = "Applies Android's algorithmic darkening where supported",
                        checked = darkMode,
                        onCheckedChange = { darkMode = it }
                    )
                    SettingSwitchRow(
                        title = "Third-party cookies",
                        subtitle = "Off by default for better privacy; some sign-in flows may need this",
                        checked = allowThirdPartyCookies,
                        onCheckedChange = { allowThirdPartyCookies = it }
                    )
                    TextButton(onClick = {
                        webView.clearCache(true)
                        webView.clearHistory()
                        webView.loadUrl("https://x.com/")
                    }) {
                        Text("Clear cache and browsing history")
                    }
                    TextButton(onClick = {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        webView.clearCache(true)
                        webView.loadUrl("https://x.com/i/flow/login")
                    }) {
                        Text("Sign out / clear saved session")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

private fun destinationFor(url: String): Int = when {
    url.contains("/explore") -> 1
    url.contains("/notifications") -> 2
    url.contains("/messages") -> 3
    else -> 0
}

private fun handleNavigation(uri: Uri, webView: WebView): Boolean {
    val scheme = uri.scheme?.lowercase() ?: return true
    val host = uri.host?.lowercase()
    if (scheme == "https" && host != null && isXHost(host)) return false

    // Keep web sign-in and linked content in the WebView; only hand off supported app links.
    if (scheme == "mailto" || scheme == "tel") {
        return try {
            webView.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            true
        }
    }
    if (scheme == "https" || scheme == "http") return false
    return true
}

private fun isXHost(host: String): Boolean =
    host == "x.com" || host.endsWith(".x.com") ||
        host == "twitter.com" || host.endsWith(".twitter.com")
