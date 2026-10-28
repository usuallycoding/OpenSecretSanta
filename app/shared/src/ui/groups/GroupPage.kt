package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.ParticipantChip
import com.usuallycoding.opensecretsanta.ui.form.ParticipantChips
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import com.usuallycoding.opensecretsanta.ui.load.LoadSection
import com.usuallycoding.opensecretsanta.ui.load.PullPage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * One group: its details, the pair rows (who gives to whom, from the person's own side), and at the end everyone who
 * takes part. The rows lead to the Gifter and Receiver pages and to the sheets with a person's details.
 */
@Composable
fun GroupPage(
    viewModel: GroupDetailViewModel,
    me: String,
    onBack: () -> Unit,
    onOpenGifter: () -> Unit,
    onOpenReceiver: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    if (state.isGone()) {
        GroupGone(onBack)
        return
    }
    val phase = state.value?.phase
    // the receiver is only known once the names are drawn
    val pair = if (phase == GroupPhase.Active || phase == GroupPhase.Archived) {
        koinViewModel<PairViewModel>(key = "pair-${viewModel.groupId}") { parametersOf(viewModel.groupId) }
    } else {
        null
    }
    val receiver = pair?.receiver?.collectAsState()?.value
    var sheet by remember { mutableStateOf<PairSheet?>(null) }

    PullPage(loading = state.loading || receiver?.loading == true, onRefresh = { viewModel.refresh(); pair?.refresh() }) {
        LoadSection(state, onRetry = { viewModel.refresh() }) { detail ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Header(detail)
                PairRows(detail.phase, me, receiver, onRetry = { pair?.refresh() }, onOpenGifter, onOpenReceiver, onSheet = { sheet = it })
                Participants(detail.participants)
            }
        }
    }
    sheet?.let { PairSheets(it, viewModel.groupId, pair, onDismiss = { sheet = null }) }
}

/** What a screen of a group says when the server answers that the group does not exist (any more, for this person). */
@Composable
internal fun GroupGone(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Strings.GROUP_GONE, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onBack) { Text(Strings.BACK) }
        }
    }
}

@Composable
private fun Header(group: GroupDetail) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReadOnlyText(group.name, style = MaterialTheme.typography.headlineSmall, label = Strings.GROUP_NAME, maxLines = 1, copy = false)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(group.state.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            group.state.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        group.description?.let { ReadOnlyText(it, style = MaterialTheme.typography.bodyLarge, label = Strings.DESCRIPTION_LABEL) }
        group.whenText?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        group.location?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
    }
}

/** Everyone in the group, as chips that are part of the page: it grows and scrolls as one when there are many names. */
@Composable
private fun Participants(participants: List<ParticipantItem>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${Strings.GROUP_PARTICIPANTS} (${participants.size})", style = MaterialTheme.typography.titleMedium)
        ParticipantChips(
            participants.map { ParticipantChip(it.username, highlighted = it.isMe, spoken = if (it.isMe) "${it.username}, ${Strings.YOU}" else null) },
            boxed = false,
        )
    }
}
