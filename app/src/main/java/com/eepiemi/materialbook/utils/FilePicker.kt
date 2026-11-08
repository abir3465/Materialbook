package com.eepiemi.materialbook.utils

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.multiplatform.webview.web.AccompanistWebChromeClient
import com.multiplatform.webview.web.PlatformWebViewParams

// src: https://github.com/KevinnZou/compose-webview-multiplatform

@Composable
fun fileChooserWebViewParams(
    allowMediaCapture: () -> Boolean = { false }
): PlatformWebViewParams {
    val context = LocalContext.current
    var fileChooserIntent by remember { mutableStateOf<Intent?>(null) }

    // Camera / microphone requests coming from the page (audio and video calls).
    var pendingPermission by remember { mutableStateOf<PermissionRequest?>(null) }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            val request = pendingPermission
            pendingPermission = null
            if (request != null) finishPermissionRequest(context, request)
        }

    val webViewChromeClient =
        remember {
            FileChoosableWebChromeClient(
                appContext = context,
                allowMediaCapture = allowMediaCapture,
                onShowFilePicker = { fileChooserIntent = it },
                onPermissionNeeded = { request, permissions ->
                    pendingPermission?.deny()
                    pendingPermission = request
                    permissionLauncher.launch(permissions)
                },
            )
        }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result: ActivityResult ->
            if (result.resultCode != Activity.RESULT_OK) {
                webViewChromeClient.cancelFileChooser()
                return@rememberLauncherForActivityResult
            }

            val intent = result.data
            if (intent == null) {
                webViewChromeClient.cancelFileChooser()
                return@rememberLauncherForActivityResult
            }

            val singleFile: Uri? = intent.data
            val multiFiles: List<Uri>? = intent.getUris()

            when {
                singleFile != null -> webViewChromeClient.onReceiveFiles(arrayOf(singleFile))
                multiFiles != null -> webViewChromeClient.onReceiveFiles(multiFiles.toTypedArray())
                else -> {
                    webViewChromeClient.cancelFileChooser()
                }
            }
        }

    LaunchedEffect(key1 = fileChooserIntent) {
        fileChooserIntent?.let {
            try {
                launcher.launch(fileChooserIntent!!)
            } catch (_: ActivityNotFoundException) {
                webViewChromeClient.cancelFileChooser()
            }
        }
    }

    return PlatformWebViewParams(chromeClient = webViewChromeClient)
}

private fun Intent.getUris(): List<Uri>? {
    val clipData = clipData ?: return null
    return (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
}

private fun androidPermissionFor(resource: String): String? = when (resource) {
    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
    else -> null
}

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun isTrustedOrigin(request: PermissionRequest): Boolean {
    val host = request.origin?.host?.lowercase() ?: return false
    return host == "facebook.com" || host.endsWith(".facebook.com") ||
        host == "messenger.com" || host.endsWith(".messenger.com")
}

/** Grants what Android already allows, denies the rest. */
private fun finishPermissionRequest(context: Context, request: PermissionRequest) {
    val allowed = request.resources.filter { resource ->
        val permission = androidPermissionFor(resource)
        permission != null && isGranted(context, permission)
    }.toTypedArray()

    if (allowed.isEmpty()) request.deny() else request.grant(allowed)
}

private class FileChoosableWebChromeClient(
    private val appContext: Context,
    private val allowMediaCapture: () -> Boolean,
    private val onShowFilePicker: (Intent) -> Unit,
    private val onPermissionNeeded: (PermissionRequest, Array<String>) -> Unit,
) : AccompanistWebChromeClient() {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onPermissionRequest(request: PermissionRequest) {
        if (!allowMediaCapture() || !isTrustedOrigin(request)) {
            request.deny()
            return
        }

        val missing = request.resources
            .mapNotNull(::androidPermissionFor)
            .distinct()
            .filter { !isGranted(appContext, it) }

        if (missing.isEmpty()) {
            finishPermissionRequest(appContext, request)
        } else {
            onPermissionNeeded(request, missing.toTypedArray())
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        this.filePathCallback = filePathCallback
        val filePickerIntent = fileChooserParams?.createIntent()

        if (filePickerIntent == null) cancelFileChooser()
        else onShowFilePicker(filePickerIntent)

        return true
    }

    fun onReceiveFiles(uris: Array<Uri>) {
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    fun cancelFileChooser() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
    }
}