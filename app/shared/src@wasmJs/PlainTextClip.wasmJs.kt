package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextClip(text: String): ClipEntry = ClipEntry.withPlainText(text)

@OptIn(ExperimentalWasmJsInterop::class)
actual fun clipboardAvailable(): Boolean = js("window.isSecureContext === true && !!(window.navigator.clipboard && window.navigator.clipboard.writeText)")
