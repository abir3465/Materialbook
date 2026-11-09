package com.eepiemi.materialbook

import android.os.Bundle
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

        val startUrl = intent?.data?.toString()
            ?.takeIf { it.startsWith("https://x.com/") || it == "https://x.com" }
            ?: "https://x.com/"

        setContent {
            XClientTheme {
                XWebViewScreen(startUrl = startUrl)
            }
        }
    }
}
