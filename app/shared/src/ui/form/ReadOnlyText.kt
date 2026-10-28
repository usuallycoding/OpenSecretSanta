package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import kotlinx.coroutines.launch

/** The Material "content copy" icon, drawn here because the core icon set has none and the extended set is large. */
internal val CopyIcon: ImageVector by lazy {
    ImageVector.Builder("Copy", 24.dp, 24.dp, 24f, 24f).addPath(
        PathParser().parsePathString(
            "M16,1L4,1c-1.1,0 -2,0.9 -2,2v14h2L4,3h12L16,1zM19,5L8,5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2L21,7c0,-1.1 -0.9,-2 -2,-2zM19,21L8,21L8,7h11v14z",
        ).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

/** The copy icon: puts [text], all of it, on the clipboard, then shows a check mark for a moment, or that it failed. */
@Composable
fun CopyButton(text: String, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val action = remember(clipboard) {
        CopyAction({
            // the web's clipboard does nothing, and says nothing, on a page that is not secure
            check(clipboardAvailable()) { "no clipboard" }
            clipboard.setClipEntry(plainTextClip(it))
        })
    }
    val status by action.status.collectAsState()
    IconButton(onClick = { scope.launch { action.copy(text) } }, modifier = modifier.size(36.dp)) {
        when (status) {
            CopyStatus.Idle -> Icon(CopyIcon, contentDescription = Strings.COPY, modifier = Modifier.size(18.dp))
            CopyStatus.Copied -> Icon(Icons.Filled.Check, contentDescription = Strings.COPIED, modifier = Modifier.size(18.dp))
            CopyStatus.Failed -> Icon(CopyIcon, contentDescription = Strings.COPY_FAILED, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Read-only text with a copy icon at its end (unless [copy] is false). It shows at most [maxLines] lines; when it was cut,
 * choosing it opens a dialog with the whole text ([label] is the dialog's title), and when it was not,
 * choosing it does nothing.
 */
@Composable
fun ReadOnlyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    label: String? = null,
    maxLines: Int = MULTILINE_MAX_ROWS,
    copy: Boolean = true,
) {
    var cut by remember(text) { mutableStateOf(false) }
    // when the text is cut at the end of a line (a line break, not the line's width) the platform adds no
    // ellipsis, so the shown text is shortened here and ends with one
    var shown by remember(text) { mutableStateOf(text) }
    var open by remember { mutableStateOf(false) }
    Row(modifier, verticalAlignment = Alignment.Top) {
        Text(
            shown,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result: TextLayoutResult ->
                if (!cut && result.hasVisualOverflow) {
                    cut = true
                    // a line that is cut by its width already ends in the platform's ellipsis
                    if (!result.isLineEllipsized(maxLines - 1)) {
                        shown = text.substring(0, result.getLineEnd(maxLines - 1, visibleEnd = true)).trimEnd() + "…"
                    }
                }
            },
            modifier = Modifier.weight(1f).then(if (cut) Modifier.clickable { open = true } else Modifier),
        )
        if (copy) CopyButton(text)
    }
    if (open) FullTextDialog(label, text, onDismiss = { open = false })
}

/** The whole of a text that was cut, scrollable, with the copy icon and a way to close it. */
@Composable
fun FullTextDialog(title: String?, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title.orEmpty(), modifier = Modifier.weight(1f))
                CopyButton(text)
            }
        },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { SelectionContainer { Text(text) } } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.CLOSE) } },
    )
}

/** A wishlist link: one line in the link colour with a copy icon; choosing it opens [LinkDialog], not the browser. */
@Composable
fun LinkText(url: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).clickable { open = true },
        )
        CopyButton(url)
    }
    if (open) LinkDialog(url, onDismiss = { open = false })
}

/** The whole link, with the choice to open it in the platform's browser or to cancel. */
@Composable
fun LinkDialog(url: String, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Strings.WISHLIST_LINK, modifier = Modifier.weight(1f))
                CopyButton(url)
            }
        },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { SelectionContainer { Text(url) } } },
        confirmButton = {
            TextButton(onClick = { runCatching { uriHandler.openUri(url) }; onDismiss() }) { Text(Strings.OPEN_IN_BROWSER) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.CANCEL) } },
    )
}
