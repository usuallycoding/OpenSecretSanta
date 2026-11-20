package com.usuallycoding.opensecretsanta

import androidx.compose.ui.graphics.ImageBitmap

/** Decodes server-normalized PNG/JPEG bytes into a Compose [ImageBitmap], or returns null on failure. */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?
