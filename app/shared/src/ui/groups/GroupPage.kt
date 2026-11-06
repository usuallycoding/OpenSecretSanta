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
import com.usuallycoding.opensecretsanta.ui.readmarks.ReadMarksViewModel
import com.usuallycoding.opensecretsanta.ui.readmarks.known
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.ParticipantChip
import com.usuallycoding.opensecretsanta.ui.form.ParticipantChips
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import com.usuallycoding.opensecretsanta.ui.gift.SantaQuestionsViewModel
import com.usuallycoding.opensecretsanta.ui.gift.unansweredCount
import com.usuallycoding.opensecretsanta.ui.load.LoadSection
import com.usuallycoding.opensecretsanta.ui.load.PullPage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * One group: its details, the two cards (who the person gives to, and their own things), and at the end everyone who
 * takes part. The cards lead to the Gifter and Receiver pages and to the sheets with a person's details.
 */
@Composable
fun GroupPage(
    viewModel: GroupDetailViewModel,
    me: String,
    onBack: () -> Unit,
    onOpenGifter: (questions: Boolean) -> Unit,
    onOpenReceiver: (questions: Boolean) -> Unit,
    onSendLetter: () -> Unit,
    onOpenInbox: () -> Unit,
    onEditDeliveryInfo: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val phase = state.value?.phase
    // the questions to count exist once the names are drawn, and only while answering is open
    val santaQuestions = if (phase == GroupPhase.Active) {
        koinViewModel<SantaQuestionsViewModel>(key = "santa-questions-${viewModel.groupId}") { parametersOf(viewModel.groupId) }
    } else {
        null
    }
    // what is new to the Santa, once the names are drawn
    val readMarks = if (phase == GroupPhase.Active || phase == GroupPhase.Archived) {
        koinViewModel<ReadMarksViewModel>(key = "read-marks-${viewModel.groupId}") { parametersOf(viewModel.groupId) }
    } else {
        null
    }
    // the person's own delivery info, which the sheet of the card shares, only while they can still fill it in
    val myDetails = if (phase == GroupPhase.Pending || phase == GroupPhase.Active) {
        koinViewModel<MyDetailsViewModel>(key = "my-details-${viewModel.groupId}") { parametersOf(viewModel.groupId, me) }
    } else {
        null
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh(); santaQuestions?.refresh(); myDetails?.refresh(); readMarks?.refresh() }

    if (state.isGone()) {
        GroupGone(onBack)
        return
    }
    // the receiver is only known once the names are drawn
    val pair = if (phase == GroupPhase.Active || phase == GroupPhase.Archived) {
        koinViewModel<PairViewModel>(key = "pair-${viewModel.groupId}") { parametersOf(viewModel.groupId) }
    } else {
        null
    }
    val receiver = pair?.receiver?.collectAsState()?.value
    // unknown, so no badge, while the questions load and when they could not be loaded
    val unanswered = santaQuestions?.data?.collectAsState()?.value
        ?.takeIf { it.error == null }?.value?.let { unansweredCount(it.questions, it.canAnswer) } ?: 0
    val unread = readMarks?.counts?.collectAsState()?.value.known()
    val asksForDeliveryInfo = showsDeliveryNudge(phase, myDetails?.details?.collectAsState()?.value)
    var sheet by remember { mutableStateOf<PairSheet?>(null) }

    PullPage(
        loading = state.loading || receiver?.loading == true,
        onRefresh = { viewModel.refresh(); pair?.refresh(); santaQuestions?.refresh(); myDetails?.refresh(); readMarks?.refresh() },
    ) {
        LoadSection(state, onRetry = { viewModel.refresh() }) { detail ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Header(detail)
                SecretSantaCard(detail.phase, receiver, unread, onRetry = { pair?.refresh() }, onOpenGifter, onOpenInbox, onSheet = { sheet = it })
                YourStuffCard(detail.phase, unanswered, asksForDeliveryInfo, onOpenReceiver, onSendLetter, onSheet = { sheet = it })
                Participants(detail.participants)
            }
        }
    }
    val editable = state.value?.phase.let { it == GroupPhase.Pending || it == GroupPhase.Active }
    sheet?.let { PairSheets(it, viewModel.groupId, me, pair, editable, onDismiss = { sheet = null }, onEditDeliveryInfo = onEditDeliveryInfo) }
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
        GroupInfoCard(group.description, group.whenText, group.location, group.budget)
    }
}

/** Everyone in the group, as chips that are part of the page: it grows and scrolls as one when there are many names. */
@Composable
private fun Participants(participants: List<ParticipantItem>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${Strings.GROUP_PARTICIPANTS} (${participants.size})", style = MaterialTheme.typography.titleMedium)
        ParticipantChips(
            participants.map { ParticipantChip(it.username, highlighted = it.isMe, spoken = if (it.isMe) "${it.username}, ${Strings.YOU}" else null) },
        )
    }
}
