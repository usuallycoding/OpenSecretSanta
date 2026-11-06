package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.KeyIcon
import com.usuallycoding.opensecretsanta.ui.form.PlaceIcon

private const val KEY_MARK = "mark-key"
private const val PIN_MARK = "mark-pin"

/**
 * A username as the owner reads it in a list: `alice`, then "(you)" for the owner's own, `*` when there is no account, and a key and
 * a pin icon for a temporary password and for incomplete delivery info. The icons are inline, so they wrap with the name, and each
 * carries its meaning as its alternate text, which is what a screen reader reads. With [marksBefore] the marks come before the name.
 */
internal fun markedText(name: String, isMe: Boolean, marks: ParticipantMarks, marksBefore: Boolean = false): AnnotatedString = buildAnnotatedString {
    if (marksBefore) {
        // the receiver's side of a pair: the marks are on the left of the name, in the mirror order, so they stay by the pair link
        if (marks.incompleteInfo) {
            appendInlineContent(PIN_MARK, Strings.ADMIN_MARK_INCOMPLETE)
            append(" ")
        }
        if (marks.temporaryPassword) {
            appendInlineContent(KEY_MARK, Strings.ADMIN_MARK_TEMPORARY)
            append(" ")
        }
        if (marks.noAccount) append("* ")
        append(name)
        if (isMe) append(" (${Strings.ADMIN_YOU})")
        return@buildAnnotatedString
    }
    append(name)
    if (isMe) append(" (${Strings.ADMIN_YOU})")
    if (marks.noAccount) append(" *")
    if (marks.temporaryPassword) {
        append(" ")
        appendInlineContent(KEY_MARK, Strings.ADMIN_MARK_TEMPORARY)
    }
    if (marks.incompleteInfo) {
        append(" ")
        appendInlineContent(PIN_MARK, Strings.ADMIN_MARK_INCOMPLETE)
    }
}

/** The title of a pair: `santa gives to receiver`, each name with its marks. */
internal fun pairTitle(santa: AnnotatedString, receiver: AnnotatedString): AnnotatedString = buildAnnotatedString {
    append(santa)
    append(" ${Strings.ADMIN_GIVES_TO} ")
    append(receiver)
}

/** The drawings that [markedText] refers to, to pass as the `inlineContent` of the `Text` that shows it. */
@Composable
internal fun markIcons(): Map<String, InlineTextContent> {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    return remember(tint) {
        val placeholder = Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextCenter)
        mapOf(
            KEY_MARK to InlineTextContent(placeholder) { Icon(KeyIcon, contentDescription = null, tint = tint, modifier = Modifier.fillMaxSize()) },
            PIN_MARK to InlineTextContent(placeholder) { Icon(PlaceIcon, contentDescription = null, tint = tint, modifier = Modifier.fillMaxSize()) },
        )
    }
}

/** One line of text with the marks of a username drawn inline. */
@Composable
internal fun MarkedName(text: AnnotatedString, modifier: Modifier = Modifier, textAlign: androidx.compose.ui.text.style.TextAlign? = null) {
    Text(text, modifier = modifier, textAlign = textAlign, inlineContent = markIcons())
}

/** What each mark shown in a list means, under the list: the mark and its words, only for the marks that are there. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MarkLegend(marks: List<Mark>, modifier: Modifier = Modifier) {
    if (marks.isEmpty()) return
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (mark in marks) {
            Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                when (mark) {
                    Mark.NoAccount -> Text("*", style = MaterialTheme.typography.labelMedium, color = color)
                    Mark.TemporaryPassword -> Icon(KeyIcon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    Mark.IncompleteInfo -> Icon(PlaceIcon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    when (mark) {
                        Mark.NoAccount -> Strings.ADMIN_MARK_NO_ACCOUNT
                        Mark.TemporaryPassword -> Strings.ADMIN_MARK_TEMPORARY
                        Mark.IncompleteInfo -> Strings.ADMIN_MARK_INCOMPLETE
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                )
            }
        }
    }
}

/** The plain count above a list; it only informs and takes no tap. */
@Composable
internal fun MarkCountLine(counts: MarkCounts, modifier: Modifier = Modifier) {
    val line = markCountLine(counts) ?: return
    Text(line, modifier = modifier, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
