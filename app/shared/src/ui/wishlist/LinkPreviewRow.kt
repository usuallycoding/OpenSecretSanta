package com.usuallycoding.opensecretsanta.ui.wishlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.contract.LinkPreviewDto
import com.usuallycoding.opensecretsanta.data.api.LinkPreviewImageCache
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.CopyButton
import com.usuallycoding.opensecretsanta.ui.form.LinkText
import org.koin.mp.KoinPlatform

/**
 * Renders a wishlist link as a compact horizontal preview row (thumbnail + title + site name) alongside
 * the copy button when a `ready` preview is available, falling back to the one-line [LinkText] otherwise.
 */
@Composable
internal fun WishlistLinkRow(
    url: String,
    preview: LinkPreviewDto?,
    modifier: Modifier = Modifier,
) {
    val title = preview?.takeIf { it.status == LinkPreviewDto.STATUS_READY }?.title?.takeIf { it.isNotBlank() }
    if (title == null) {
        LinkText(url, modifier)
        return
    }
    val uriHandler = LocalUriHandler.current
    val imageCache = remember { KoinPlatform.getKoinOrNull()?.getOrNull<LinkPreviewImageCache>() }
    val imagePath = preview.imagePath
    var bitmap by remember(imagePath) { mutableStateOf<ImageBitmap?>(imagePath?.let { imageCache?.get(it) }) }

    LaunchedEffect(imagePath, imageCache) {
        if (imagePath != null && bitmap == null && imageCache != null) {
            bitmap = imageCache.load(imagePath)
        }
    }

    val subtitle = preview.siteName?.takeIf { it.isNotBlank() } ?: hostOf(url)

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = Strings.OPEN_LINK_IN_BROWSER, role = Role.Button) {
                    runCatching { uriHandler.openUri(url) }
                }
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val loaded = bitmap
                if (loaded != null) {
                    Image(
                        bitmap = loaded,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = LinkPlaceholderIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        CopyButton(url)
    }
}

/** Extracts a clean display host (without leading `www.`) from an `http`/`https` URL. */
internal fun hostOf(url: String): String? {
    val sep = url.indexOf("://")
    if (sep < 0) return null
    val authority = url.substring(sep + 3).takeWhile { it != '/' && it != '?' && it != '#' }
    val host = authority.substringBefore(':').lowercase().removePrefix("www.")
    return host.takeIf { it.isNotBlank() }
}

private val LinkPlaceholderIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "LinkPlaceholder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(10f, 13f)
            arcTo(5f, 5f, 0f, false, false, 17.54f, 13.54f)
            lineTo(20.54f, 10.54f)
            arcTo(5f, 5f, 0f, false, false, 13.46f, 3.46f)
            lineTo(11.75f, 5.18f)
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(14f, 11f)
            arcTo(5f, 5f, 0f, false, false, 6.46f, 10.46f)
            lineTo(3.46f, 13.46f)
            arcTo(5f, 5f, 0f, false, false, 10.54f, 20.54f)
            lineTo(12.25f, 18.82f)
        }
    }.build()
}
