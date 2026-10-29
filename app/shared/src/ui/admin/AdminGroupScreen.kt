package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import com.usuallycoding.opensecretsanta.contract.DeletionImpactDto
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.usuallycoding.opensecretsanta.ui.form.SheetBackHandler
import com.usuallycoding.opensecretsanta.ui.form.rememberAppSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.FormColumn
import com.usuallycoding.opensecretsanta.ui.form.SheetTextField
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.HeaderWithFooter
import com.usuallycoding.opensecretsanta.ui.load.LoadSection
import com.usuallycoding.opensecretsanta.ui.load.ScrollingBox
import com.usuallycoding.opensecretsanta.ui.form.GiftIcon
import com.usuallycoding.opensecretsanta.ui.form.ParticipantChip
import com.usuallycoding.opensecretsanta.ui.form.ParticipantChips
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextAlign

/** One of the administrator's groups: its details and people, and what can be done with it in its state. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AdminGroupScreen(
    viewModel: AdminGroupViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.data.collectAsState()
    val draw by viewModel.draw.collectAsState()
    val ui by viewModel.ui.collectAsState()
    // each opening of the announcement sheet starts empty: a new key gives it a new view model
    var announcing by remember { mutableStateOf(false) }
    var announceKey by remember { mutableStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(ui.deleted) { if (ui.deleted) onDeleted() }
    // Back closes an open sheet before it leaves the screen, on the platforms whose back is not already the sheet's own
    SheetBackHandler(enabled = ui.sheet != null) { viewModel.dismissSheet() }
    SheetBackHandler(enabled = announcing) {
        announcing = false
        announceKey++
    }

    if (state.isGone()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Strings.GROUP_GONE, style = MaterialTheme.typography.titleMedium)
                Button(onClick = onBack) { Text(Strings.BACK) }
            }
        }
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize()) {
            ui.notice?.let { notice ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(notice.text(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::dismissNotice) { Text(Strings.OK) }
                }
            }
            ui.emails?.let { emails ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Strings.emailSummary(emails.sent, emails.failed, emails.noAddress),
                        modifier = Modifier.weight(1f),
                        color = if (emails.failed > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = viewModel::dismissEmails) { Text(Strings.OK) }
                }
            }
            HeaderWithFooter(
                loading = state.loading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.weight(1f),
                header = { LoadSection(state, onRetry = { viewModel.refresh() }) { group -> Header(group, viewModel, ui.busy, onEdit) { announcing = true } } },
                footer = { state.value?.let { group -> People(group, viewModel, draw) } },
            )
        }
    }
    ui.participants?.let { ParticipantsSheet(it, viewModel) }
    if (announcing) {
        val name = state.value?.name.orEmpty()
        val announcement = koinViewModel<AnnouncementViewModel>(key = "announce-${viewModel.groupId}-$announceKey") { parametersOf(viewModel.groupId, name) }
        AnnouncementSheet(announcement) {
            announcing = false
            announceKey++
        }
    }
    ui.sheet?.let { GroupSheetHost(it, viewModel.groupId, viewModel.me, draw, viewModel::dismissSheet) }
    ui.confirming?.let { step -> StepDialog(step, state.value?.name.orEmpty(), viewModel) }
    ui.confirmingDelete?.let { impact -> DeleteImpactDialog(impact, viewModel) }
    ui.passwords?.let { PasswordsDialog(Strings.PASSWORDS_TITLE, Strings.PASSWORDS_BODY, it, viewModel::dismissPasswords) }
}

private fun AdminNotice.text() = when (this) {
    AdminNotice.ChangedElsewhere -> Strings.NOTICE_GROUP_CHANGED
    AdminNotice.RosterFrozen -> Strings.NOTICE_ROSTER_FROZEN
    AdminNotice.Unreachable -> Strings.SERVER_UNREACHABLE
}

private fun AdminPhase.note(): String? = when (this) {
    AdminPhase.Draft -> Strings.ADMIN_NOTE_DRAFT
    AdminPhase.Pending -> Strings.ADMIN_NOTE_PENDING
    AdminPhase.Active -> Strings.ADMIN_NOTE_ACTIVE
    AdminPhase.Archived -> Strings.ADMIN_NOTE_ARCHIVED
    AdminPhase.Other -> null
}

private fun AdminStep.label() = when (this) {
    AdminStep.MakePending -> Strings.STEP_MAKE_PENDING
    AdminStep.DrawNames -> Strings.STEP_DRAW
    AdminStep.Archive -> Strings.STEP_ARCHIVE
    AdminStep.Delete -> Strings.DELETE
}

/** The group's details and, in one row, everything that can be done with it: edit, the next step and (a Draft) delete. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(group: AdminGroup, viewModel: AdminGroupViewModel, busy: Boolean, onEdit: () -> Unit, onAnnounce: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReadOnlyText(group.name, style = MaterialTheme.typography.headlineSmall, label = Strings.GROUP_NAME, maxLines = 1, copy = false)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(group.state.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            group.phase.note()?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        group.description?.let { ReadOnlyText(it, style = MaterialTheme.typography.bodyLarge, label = Strings.DESCRIPTION_LABEL) }
        group.whenText?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        group.location?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        val step = viewModel.nextStep
        val blocked = viewModel.drawNeedsMorePeople
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (viewModel.canEditDetails) {
                OutlinedButton(onClick = onEdit) { Text(Strings.ADMIN_EDIT_DETAILS) }
            }
            if (viewModel.canAnnounce) {
                OutlinedButton(onClick = onAnnounce) { Text(Strings.ANNOUNCE_BUTTON) }
            }
            if (step != null) {
                Button(onClick = { viewModel.ask(step) }, enabled = !busy && !blocked) { Text(step.label()) }
            }
            if (viewModel.canDelete) {
                TextButton(onClick = { viewModel.ask(AdminStep.Delete) }, enabled = !busy) {
                    Text(Strings.DELETE, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (step != null && blocked) {
            Text(Strings.drawNeedsPeople(ActivateGroup.MIN_PARTICIPANTS), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Who takes part, in a box at the end that fills the rest of the page: the participants, or once drawn the pairs. */
@Composable
private fun ColumnScope.People(group: AdminGroup, viewModel: AdminGroupViewModel, draw: DrawState) {
    if (viewModel.canReadDraw) {
        if ((draw as? DrawState.Pairs)?.pairs?.isNotEmpty() == true) ParticipantSearchField(viewModel)
        PairList(
            draw,
            viewModel.search,
            onRetry = { viewModel.refresh() },
            onUser = viewModel::openUserSheet,
            onPair = { viewModel.openPairSheet(it.santa, it.receiver) },
        )
        return
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${Strings.GROUP_PARTICIPANTS} (${group.participants.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (viewModel.canEditParticipants) {
            TextButton(onClick = viewModel::openParticipants) { Text(Strings.ADMIN_EDIT_PARTICIPANTS) }
        }
    }
    if (group.participants.isEmpty()) {
        ScrollingBox { Text(Strings.ADMIN_NO_PARTICIPANTS, style = MaterialTheme.typography.bodyMedium) }
        return
    }
    val legend = Strings.ADMIN_MARK_LEGEND.takeIf { group.participants.any { it.marked() } }
    if (viewModel.canReadPendingWishlists) {
        ParticipantSearchField(viewModel)
        val shown = filterParticipants(group.participants, viewModel.search)
        ScrollingBox {
            if (shown.isEmpty()) Text(Strings.ADMIN_NO_PERSON_MATCH, style = MaterialTheme.typography.bodyMedium)
            for (participant in shown) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { viewModel.openUserSheet(participant.username) },
                        enabled = participant.hasAccount,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(participant.label(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                    IconButton(onClick = { viewModel.openWishlistSheet(participant.username) }, enabled = participant.hasAccount) {
                        Icon(GiftIcon, contentDescription = Strings.ADMIN_OPEN_WISHLIST)
                    }
                }
            }
            if (legend != null) Text(legend, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        ParticipantChips(group.participants.map { ParticipantChip(it.label()) }, legend)
    }
}

/** A participant without an account, or whose profile lacks a name or an address, is followed by one `*`. */
private fun AdminParticipant.marked() = !hasAccount || !profileComplete

private fun AdminParticipant.label() = if (marked()) "$username *" else username

/** The second confirmation of a deletion, once the server has said what else would go: the data of the group, for good. */
@Composable
private fun DeleteImpactDialog(impact: DeletionImpactDto, viewModel: AdminGroupViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::cancelDelete,
        title = { Text(Strings.DELETE_IMPACT_TITLE) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Strings.deleteImpactBody(impact.wishlistItems, impact.questions, impact.answers, impact.draw))
                Text(Strings.DELETE_IMPACT_FOREVER, style = MaterialTheme.typography.titleSmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.confirmDelete() }) { Text(Strings.DELETE_EVERYTHING, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(Strings.CANCEL) } },
    )
}

@Composable
private fun StepDialog(step: AdminStep, groupName: String, viewModel: AdminGroupViewModel) {
    val (title, body) = when (step) {
        AdminStep.MakePending -> Strings.stepTitle(Strings.STEP_MAKE_PENDING) to Strings.STEP_MAKE_PENDING_BODY
        AdminStep.DrawNames -> Strings.stepTitle(Strings.STEP_DRAW) to Strings.STEP_DRAW_BODY
        AdminStep.Archive -> Strings.stepTitle(Strings.STEP_ARCHIVE) to Strings.STEP_ARCHIVE_BODY
        AdminStep.Delete -> Strings.STEP_DELETE_TITLE to Strings.stepDeleteBody(groupName)
    }
    AlertDialog(
        onDismissRequest = viewModel::cancelStep,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = { viewModel.confirmStep() }) {
                Text(step.label(), color = if (step == AdminStep.Delete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = viewModel::cancelStep) { Text(Strings.CANCEL) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParticipantsSheet(editor: ParticipantsEditor, viewModel: AdminGroupViewModel) {
    ModalBottomSheet(onDismissRequest = viewModel::dismissParticipants, sheetState = rememberAppSheetState()) {
        FormColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(Strings.ADMIN_PARTICIPANTS_TITLE, style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetTextField(
                    value = editor.input,
                    onValueChange = viewModel::typeUsername,
                    label = Strings.ADMIN_USERNAME,
                    error = editor.error,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = viewModel::addUsername, modifier = Modifier.padding(top = 8.dp)) { Text(Strings.ADMIN_ADD) }
            }
            for (name in editor.names) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { viewModel.removeUsername(name) }) { Text(Strings.ADMIN_REMOVE) }
                }
            }
            editor.general?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = viewModel::dismissParticipants) { Text(Strings.CANCEL) }
                Button(onClick = { viewModel.saveParticipants() }, enabled = !editor.saving) { Text(if (editor.saving) Strings.SAVING else Strings.SAVE) }
            }
        }
    }
}
