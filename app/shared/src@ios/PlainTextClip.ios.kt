package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextClip(text: String): ClipEntry = ClipEntry.withPlainText(text)

actual fun clipboardAvailable(): Boolean = true
