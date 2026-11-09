package com.eepiemi.materialbook

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.eepiemi.materialbook.ui.screens.XWebViewScreen
import com.eepiemi.materialbook.ui.theme.XClientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val startUrl = intent?.data?.toString()?.takeIf(::isTrustedStartUrl)
            ?: "https://x.com/"

        setContent {
            XClientTheme {
                XWebViewScreen(startUrl = startUrl)
            }
        }
    }

    private fun isTrustedStartUrl(value: String): Boolean {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme == "https" && (
            host == "x.com" || host.endsWith(".x.com") ||
                host == "twitter.com" || host.endsWith(".twitter.com")
            )
    }
}
