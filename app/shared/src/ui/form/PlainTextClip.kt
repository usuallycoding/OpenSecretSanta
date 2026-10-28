package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.ui.platform.ClipEntry

/** A clipboard entry holding [text]; building one is the only part of copying that differs between the platforms. */
expect fun plainTextClip(text: String): ClipEntry

/** Whether the platform lets the app write to the clipboard at all; the web only does on a secure page (`https` or `localhost`). */
expect fun clipboardAvailable(): Boolean
