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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.PairLink
import com.usuallycoding.opensecretsanta.ui.form.SnowCard
import com.usuallycoding.opensecretsanta.ui.groups.DetailField
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.loadMessage
import com.usuallycoding.opensecretsanta.ui.wishlist.ItemText
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.usuallycoding.opensecretsanta.ui.gift.QuestionThread
import com.usuallycoding.opensecretsanta.ui.form.SheetFrame

/**
 * The single list of an Active or Archived group: one row per pair, `santa -> receiver`. A username opens
 * that person's sheet, the pair link between them opens the pair's sheet. Nothing here changes anything.
 */
@Composable
internal fun ColumnScope.PairList(draw: DrawState, query: String, counts: MarkCounts, onRetry: () -> Unit, onUser: (String) -> Unit, onPair: (AdminPair) -> Unit) {
    when (draw) {
        DrawState.None -> Unit
        DrawState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(Strings.ADMIN_DRAW_FAILED, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onRetry) { Text(Strings.TRY_AGAIN) }
        }
        is DrawState.Pairs -> {
            // the count is the whole group's, not the narrowed list's
            Text(Strings.adminDraw(draw.pairs.size), style = MaterialTheme.typography.titleMedium)
            MarkCountLine(counts)
            val shown = filterPairs(draw.pairs, query)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (shown.isEmpty()) Text(Strings.ADMIN_NO_PERSON_MATCH, style = MaterialTheme.typography.bodyMedium)
                for (pair in shown) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onUser(pair.santa) }, modifier = Modifier.weight(1f)) {
                            MarkedName(markedText(pair.santa, pair.santaIsMe, pair.santaMarks()), Modifier.fillMaxWidth(), TextAlign.Start)
                        }
                        PairLink(Strings.adminPair(pair.santa, pair.receiver), onClick = { onPair(pair) })
                        TextButton(onClick = { onUser(pair.receiver) }, modifier = Modifier.weight(1f)) {
                            MarkedName(markedText(pair.receiver, pair.receiverIsMe, pair.receiverMarks(), marksBefore = true), Modifier.fillMaxWidth(), TextAlign.End)
                        }
                    }
                }
                MarkLegend(legendMarks(draw.pairs.flatMap { listOf(it.santaMarks(), it.receiverMarks()) }))
            }
        }
    }
}

/** The sheet that is open on a group's detail, whichever kind it is. */
@Composable
internal fun GroupSheetHost(sheet: GroupSheet, groupId: Long, me: String, draw: DrawState, onDismiss: () -> Unit) {
    when (sheet) {
        is GroupSheet.User -> UserSheet(groupId, sheet.username, me, onDismiss)
        is GroupSheet.Pair -> {
            val pair = (draw as? DrawState.Pairs)?.pairs?.firstOrNull { it.santa == sheet.santa && it.receiver == sheet.receiver }
            val title = pairTitle(
                markedText(sheet.santa, pair?.santaIsMe == true, pair?.santaMarks() ?: ParticipantMarks()),
                markedText(sheet.receiver, pair?.receiverIsMe == true, pair?.receiverMarks() ?: ParticipantMarks(), marksBefore = true),
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

/**
 * A person's account with the reset, as on the Users tab, and below it what they told this group about where a gift can reach them.
 * The two parts load apart and both load again each time the sheet is opened.
 */
@Composable
private fun UserSheet(groupId: Long, username: String, me: String, onDismiss: () -> Unit) {
    val viewModel = koinViewModel<AdminUserViewModel>(key = "admin-user-$username") { parametersOf(username, me) }
    val delivery = koinViewModel<AdminParticipantDeliveryInfoViewModel>(key = "admin-delivery-$groupId-$username") { parametersOf(groupId, username) }
    val state by viewModel.data.collectAsState()
    val deliveryState by delivery.state.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LaunchedEffect(viewModel, delivery) {
        viewModel.refresh()
        delivery.refresh()
    }

    SheetFrame(onDismiss) {
        if (state.isGone()) {
            Unavailable(Strings.USER_GONE, onDismiss)
        } else {
            UserNoticeRow(ui, viewModel)
            Part(state, onRetry = { viewModel.refresh() }) { user -> UserDetail(user, viewModel, ui.busy) }
            DeliveryInfoPart(deliveryState, onRetry = { delivery.refresh() })
        }
    }
    UserDialogs(viewModel, ui)
}

/** The person's delivery info for the group, read-only; it fails, retries and says it is gone on its own, apart from the account part. */
@Composable
private fun DeliveryInfoPart(state: LoadState<OversightDeliveryInfo>, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Strings.DELIVERY_INFO, style = MaterialTheme.typography.titleMedium)
        when {
            state.value == null && state.isGone() -> Text(Strings.ADMIN_PARTICIPANT_GONE, style = MaterialTheme.typography.bodyMedium)
            state.notReadable() -> Text(Strings.ADMIN_NOT_READABLE, style = MaterialTheme.typography.bodyMedium)
            else -> Part(state, onRetry) { info ->
                if (info.isEmpty) {
                    Text(Strings.ADMIN_DELIVERY_EMPTY, style = MaterialTheme.typography.bodyMedium)
                } else {
                    SnowCard("delivery-${info.fullName}", Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailField(Strings.USER_FIELD_NAME, info.fullName)
                            DetailField(Strings.USER_FIELD_EMAIL, info.email)
                            DetailField(Strings.RECEIVER_PHONE, info.phone)
                            DetailField(Strings.RECEIVER_ADDRESS, info.address)
                            DetailField(Strings.RECEIVER_DELIVERY, info.deliveryInfo)
                        }
                    }
                    info.missingLine?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary) }
                }
            }
        }
    }
}

// --- the pair and wishlist sheets -------------------------------------------------------------------

/** The receiver's wishlist and what the Santa asked them, read-only. Each part loads, fails and retries on its own. */
@Composable
private fun PairSheet(groupId: Long, santa: String, receiver: String, title: AnnotatedString, onDismiss: () -> Unit) {
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
            Text(title, style = MaterialTheme.typography.titleLarge, inlineContent = markIcons())
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
        for ((index, item) in items.withIndex()) {
            SnowCard("wish-$index", Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ItemText(index + 1, item.title, item.note, item.url, item.added, item.changed)
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
    // cards like the wishlist items above, with the same padding and the same space between them
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (question in thread.questions) {
            SnowCard("question-${question.number}", Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    QuestionThread(question.number, question.text, question.asked, question.answer, question.answered, question.answerChanged)
                }
            }
        }
    }
}
