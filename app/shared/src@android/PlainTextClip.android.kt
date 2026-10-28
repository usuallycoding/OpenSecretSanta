package com.usuallycoding.opensecretsanta.ui.form

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun plainTextClip(text: String): ClipEntry = ClipEntry(ClipData.newPlainText("text", text))

actual fun clipboardAvailable(): Boolean = true
