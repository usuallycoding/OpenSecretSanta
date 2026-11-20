package com.usuallycoding.opensecretsanta.server.data.preview

import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewImage
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Validates, decodes, and resizes fetched preview images into small server-cached thumbnails.
 * Unsupported or corrupt images return `null` so the caller can degrade cleanly to a title-only preview.
 */
object ThumbnailResizer {
    const val MAX_DIMENSION = 256
    private const val MAX_SOURCE_DIMENSION = 10_000
    private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif")

    fun resize(bytes: ByteArray, contentType: String?): LinkPreviewImage? {
        val mime = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return null
        if (mime !in ALLOWED_CONTENT_TYPES) return null

        val source = try {
            ImageIO.read(ByteArrayInputStream(bytes))
        } catch (_: Exception) {
            null
        } ?: return null

        if (source.width <= 0 || source.height <= 0 ||
            source.width > MAX_SOURCE_DIMENSION || source.height > MAX_SOURCE_DIMENSION
        ) {
            return null
        }

        val longest = max(source.width, source.height)
        val (targetW, targetH) = if (longest <= MAX_DIMENSION) {
            source.width to source.height
        } else {
            val scale = MAX_DIMENSION.toDouble() / longest.toDouble()
            max(1, (source.width * scale).roundToInt()) to max(1, (source.height * scale).roundToInt())
        }

        val hasAlpha = source.colorModel.hasAlpha()
        val imageType = if (hasAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        val resized = BufferedImage(targetW, targetH, imageType)
        val graphics = resized.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.drawImage(source, 0, 0, targetW, targetH, null)
        } finally {
            graphics.dispose()
        }

        return if (hasAlpha) {
            encodePng(resized)
        } else {
            encodeJpeg(resized)
        }
    }

    private fun encodePng(image: BufferedImage): LinkPreviewImage? {
        val out = ByteArrayOutputStream()
        if (!ImageIO.write(image, "png", out)) return null
        return LinkPreviewImage(out.toByteArray(), "image/png")
    }

    private fun encodeJpeg(image: BufferedImage): LinkPreviewImage? {
        val out = ByteArrayOutputStream()
        val writers = ImageIO.getImageWritersByFormatName("jpeg")
        if (!writers.hasNext()) return null
        val writer = writers.next()
        try {
            val param = writer.defaultWriteParam
            if (param.canWriteCompressed()) {
                param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                param.compressionQuality = 0.82f
            }
            ImageIO.createImageOutputStream(out).use { ios ->
                writer.output = ios
                writer.write(null, IIOImage(image, null, null), param)
            }
        } finally {
            writer.dispose()
        }
        return LinkPreviewImage(out.toByteArray(), "image/jpeg")
    }
}
