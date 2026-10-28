package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadContent
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.wishlist.ItemText

/** The group, participant or state the server says cannot be read: said plainly with a way back. */
@Composable
private fun Unavailable(message: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onBack) { Text(Strings.BACK) }
        }
    }
}

private fun LoadState<*>.notReadable(): Boolean = (error as? ApiError.Rejected)?.status == 409

/** What a participant keeps for one group, in their order. Nothing here can be changed. */
@Composable
fun AdminParticipantWishlistScreen(viewModel: AdminParticipantWishlistViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    when {
        state.isGone() -> return Unavailable(Strings.ADMIN_PARTICIPANT_GONE, onBack)
        state.value == null && state.notReadable() -> return Unavailable(Strings.ADMIN_NOT_READABLE, onBack)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.adminWishlistTitle(viewModel.username), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.refresh() }) { Text(Strings.REFRESH) }
            }
            LoadContent(state, onRetry = { viewModel.refresh() }) { items ->
                if (items.isEmpty()) {
                    Text(Strings.ADMIN_WISHLIST_EMPTY, modifier = Modifier.fillMaxWidth().padding(24.dp), style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items) { item ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ItemText(item.title, item.note, item.url)
                                    Text(Strings.adminAdded(item.added), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    item.changed?.let {
                                        Text(Strings.adminChanged(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Every thread of a group, one per Santa and receiver pair. Nothing here asks, answers or changes anything. */
@Composable
fun AdminQuestionsScreen(viewModel: AdminQuestionsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    when {
        state.isGone() -> return Unavailable(Strings.GROUP_GONE, onBack)
        state.value == null && state.notReadable() -> return Unavailable(Strings.ADMIN_NOT_READABLE, onBack)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { viewModel.refresh() }) { Text(Strings.REFRESH) }
            }
            LoadContent(state, onRetry = { viewModel.refresh() }) { threads ->
                if (threads.isEmpty()) {
                    Text(Strings.ADMIN_NO_THREADS, modifier = Modifier.fillMaxWidth().padding(24.dp), style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(threads) { ThreadCard(it) }
                    }
                }
            }
        }
    }
}

private fun you(name: String, isMe: Boolean) = if (isMe) "$name (${Strings.ADMIN_YOU})" else name

@Composable
private fun ThreadCard(thread: OversightThread) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                Strings.adminPair(you(thread.santa, thread.santaIsMe), you(thread.receiver, thread.receiverIsMe)),
                style = MaterialTheme.typography.titleMedium,
            )
            if (thread.questions.isEmpty()) {
                Text(Strings.ADMIN_THREAD_EMPTY, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            for (question in thread.questions) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${question.number}. ${question.text}", style = MaterialTheme.typography.titleSmall)
                    Text(Strings.adminAsked(question.asked), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (question.answer != null) {
                        Text(question.answer, style = MaterialTheme.typography.bodyMedium)
                        question.answered?.let {
                            Text(Strings.adminAnswered(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        question.answerChanged?.let {
                            Text(Strings.adminAnswerChanged(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(Strings.NOT_ANSWERED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** The draw section of a group's detail: every pair, with the owner's own side marked. */
@Composable
internal fun DrawSection(draw: DrawState) {
    when (draw) {
        DrawState.None -> Unit
        DrawState.Failed -> Text(Strings.ADMIN_DRAW_FAILED, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        is DrawState.Pairs -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(Strings.ADMIN_DRAW, style = MaterialTheme.typography.titleMedium)
            for (pair in draw.pairs) {
                Text(
                    Strings.adminPair(you(pair.santa, pair.santaIsMe), you(pair.receiver, pair.receiverIsMe)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
