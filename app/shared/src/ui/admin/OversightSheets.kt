package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.loadMessage
import com.usuallycoding.opensecretsanta.ui.wishlist.ItemText
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import com.usuallycoding.opensecretsanta.ui.form.TimesLine
import com.usuallycoding.opensecretsanta.ui.form.SheetFrame
import com.usuallycoding.opensecretsanta.ui.load.ScrollingBox

/** A username as the owner reads it in the list: `alice`, `alice (you)`, `alice *` or `alice (you) *`. */
internal fun usernameLabel(name: String, isMe: Boolean, incomplete: Boolean): String =
    buildString {
        append(name)
        if (isMe) append(" (${Strings.ADMIN_YOU})")
        if (incomplete) append(" *")
    }

/**
 * The single list of an Active or Archived group: one row per pair, `santa -> receiver`. A username opens
 * that person's sheet, the arrow opens the pair's sheet. Nothing here changes anything.
 */
@Composable
internal fun ColumnScope.PairList(draw: DrawState, onRetry: () -> Unit, onUser: (String) -> Unit, onPair: (AdminPair) -> Unit) {
    when (draw) {
        DrawState.None -> Unit
        DrawState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(Strings.ADMIN_DRAW_FAILED, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onRetry) { Text(Strings.TRY_AGAIN) }
        }
        is DrawState.Pairs -> {
            Text(Strings.ADMIN_DRAW, style = MaterialTheme.typography.titleMedium)
            ScrollingBox {
                for (pair in draw.pairs) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onUser(pair.santa) }, modifier = Modifier.weight(1f)) {
                            Text(usernameLabel(pair.santa, pair.santaIsMe, pair.santaIncomplete), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        }
                        IconButton(onClick = { onPair(pair) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = Strings.adminPair(pair.santa, pair.receiver))
                        }
                        TextButton(onClick = { onUser(pair.receiver) }, modifier = Modifier.weight(1f)) {
                            Text(usernameLabel(pair.receiver, pair.receiverIsMe, pair.receiverIncomplete), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                        }
                    }
                }
                if (draw.pairs.any { it.santaIncomplete || it.receiverIncomplete }) {
                    Text(Strings.ADMIN_INCOMPLETE_LEGEND, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** The sheet that is open on a group's detail, whichever kind it is. */
@Composable
internal fun GroupSheetHost(sheet: GroupSheet, groupId: Long, me: String, draw: DrawState, onDismiss: () -> Unit) {
    when (sheet) {
        is GroupSheet.User -> UserSheet(sheet.username, me, onDismiss)
        is GroupSheet.Pair -> {
            val pair = (draw as? DrawState.Pairs)?.pairs?.firstOrNull { it.santa == sheet.santa && it.receiver == sheet.receiver }
            val title = Strings.adminPair(
                usernameLabel(sheet.santa, pair?.santaIsMe == true, pair?.santaIncomplete == true),
                usernameLabel(sheet.receiver, pair?.receiverIsMe == true, pair?.receiverIncomplete == true),
            )
            PairSheet(groupId, sheet.santa, sheet.receiver, title, onDismiss)
        }
        is GroupSheet.Wishlist -> WishlistSheet(groupId, sheet.username, onDismiss)
    }
}

/** What the sheet says when what it shows cannot be read at all: said plainly with a way to close. */
@Composable
private fun Unavailable(message: String, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onDismiss) { Text(Strings.ADMIN_SHEET_CLOSE) }
    }
}

private fun LoadState<*>.notReadable(): Boolean = value == null && (error as? ApiError.Rejected)?.status == 409

/** One part of a sheet that loads by itself: progress, the failure with "Try again" (keeping what was shown), or the content. */
@Composable
private fun <T : Any> Part(state: LoadState<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    val value = state.value
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.error?.let { error ->
            Text(error.loadMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onRetry) { Text(Strings.TRY_AGAIN) }
        }
        when {
            value != null -> content(value)
            state.loading -> CircularProgressIndicator(Modifier.padding(8.dp))
        }
    }
}

// --- the user sheet --------------------------------------------------------------------------------

/** A person's details with the reset, as on the Users tab. It loads again each time it is opened. */
@Composable
private fun UserSheet(username: String, me: String, onDismiss: () -> Unit) {
    val viewModel = koinViewModel<AdminUserViewModel>(key = "admin-user-$username") { parametersOf(username, me) }
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }

    SheetFrame(onDismiss) {
        if (state.isGone()) {
            Unavailable(Strings.USER_GONE, onDismiss)
        } else {
            UserNoticeRow(ui, viewModel)
            Part(state, onRetry = { viewModel.refresh() }) { user -> UserDetail(user, viewModel, ui.busy) }
        }
    }
    UserDialogs(viewModel, ui)
}

// --- the pair and wishlist sheets -------------------------------------------------------------------

/** The receiver's wishlist and what the Santa asked them, read-only. Each part loads, fails and retries on its own. */
@Composable
private fun PairSheet(groupId: Long, santa: String, receiver: String, title: String, onDismiss: () -> Unit) {
    val wishlist = koinViewModel<AdminParticipantWishlistViewModel>(key = "admin-wishlist-$groupId-$receiver") { parametersOf(groupId, receiver) }
    val thread = koinViewModel<AdminPairThreadViewModel>(key = "admin-thread-$groupId-$santa-$receiver") { parametersOf(groupId, santa, receiver) }
    val wishes by wishlist.state.collectAsState()
    val asked by thread.state.collectAsState()
    LaunchedEffect(wishlist, thread) {
        wishlist.refresh()
        thread.refresh()
    }

    SheetFrame(onDismiss) {
        val unavailable = when {
            wishes.isGone() || asked.isGone() -> Strings.ADMIN_PARTICIPANT_GONE
            wishes.notReadable() || asked.notReadable() -> Strings.ADMIN_NOT_READABLE
            else -> null
        }
        if (unavailable != null) {
            Unavailable(unavailable, onDismiss)
        } else {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(Strings.adminWishlistTitle(receiver), style = MaterialTheme.typography.titleMedium)
            Part(wishes, onRetry = { wishlist.refresh() }) { items -> WishlistItems(items) }
            Text(Strings.ADMIN_QUESTIONS_TITLE, style = MaterialTheme.typography.titleMedium)
            Part(asked, onRetry = { thread.refresh() }) { found -> ThreadQuestions(found) }
        }
    }
}

/** One participant's wishlist in a Pending group, read-only. */
@Composable
private fun WishlistSheet(groupId: Long, username: String, onDismiss: () -> Unit) {
    val wishlist = koinViewModel<AdminParticipantWishlistViewModel>(key = "admin-wishlist-$groupId-$username") { parametersOf(groupId, username) }
    val wishes by wishlist.state.collectAsState()
    LaunchedEffect(wishlist) { wishlist.refresh() }

    SheetFrame(onDismiss) {
        when {
            wishes.isGone() -> Unavailable(Strings.ADMIN_PARTICIPANT_GONE, onDismiss)
            wishes.notReadable() -> Unavailable(Strings.ADMIN_NOT_READABLE, onDismiss)
            else -> {
                Text(Strings.adminWishlistTitle(username), style = MaterialTheme.typography.titleLarge)
                Part(wishes, onRetry = { wishlist.refresh() }) { items -> WishlistItems(items) }
            }
        }
    }
}

@Composable
private fun WishlistItems(items: List<OversightItem>) {
    if (items.isEmpty()) {
        Text(Strings.ADMIN_WISHLIST_EMPTY, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (item in items) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ItemText(item.title, item.note, item.url, item.added, item.changed)
                }
            }
        }
    }
}

@Composable
private fun ThreadQuestions(thread: PairThread) {
    if (thread.questions.isEmpty()) {
        Text(Strings.ADMIN_THREAD_EMPTY, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (question in thread.questions) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ReadOnlyText("${question.number}. ${question.text}", style = MaterialTheme.typography.titleSmall, label = Strings.QUESTION_LABEL)
                TimesLine(Strings.asked(question.asked))
                if (question.answer != null) {
                    ReadOnlyText(question.answer, style = MaterialTheme.typography.bodyMedium, label = Strings.ANSWER)
                    TimesLine(question.answered?.let(Strings::answered), question.answerChanged?.let(Strings::answerChanged))
                } else {
                    Text(Strings.NOT_ANSWERED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
