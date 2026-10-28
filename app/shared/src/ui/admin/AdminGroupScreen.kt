package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.FormColumn
import com.usuallycoding.opensecretsanta.ui.form.SheetTextField
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadContent

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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(ui.deleted) { if (ui.deleted) onDeleted() }
    // Back closes an open sheet before it leaves the screen, on the platforms whose back is not already the sheet's own
    BackHandler(enabled = ui.sheet != null) { viewModel.dismissSheet() }

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
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { viewModel.refresh() }) { Text(Strings.REFRESH) }
            }
            ui.notice?.let { notice ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(notice.text(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::dismissNotice) { Text(Strings.OK) }
                }
            }
            LoadContent(state, onRetry = { viewModel.refresh() }) { group -> Detail(group, viewModel, ui.busy, draw, onEdit) }
        }
    }
    ui.participants?.let { ParticipantsSheet(it, viewModel) }
    ui.sheet?.let { GroupSheetHost(it, viewModel.groupId, viewModel.me, draw, viewModel::dismissSheet) }
    ui.confirming?.let { step -> StepDialog(step, state.value?.name.orEmpty(), viewModel) }
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

@Composable
private fun Detail(
    group: AdminGroup,
    viewModel: AdminGroupViewModel,
    busy: Boolean,
    draw: DrawState,
    onEdit: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(group.name, style = MaterialTheme.typography.headlineSmall)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(group.state.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            group.phase.note()?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        group.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        group.whenText?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        group.location?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        if (viewModel.canEditDetails) {
            OutlinedButton(onClick = onEdit) { Text(Strings.ADMIN_EDIT_DETAILS) }
        }
        if (viewModel.canReadDraw) {
            PairList(
                draw,
                onRetry = { viewModel.refresh() },
                onUser = viewModel::openUserSheet,
                onPair = { viewModel.openPairSheet(it.santa, it.receiver) },
            )
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${Strings.GROUP_PARTICIPANTS} (${group.participants.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (viewModel.canEditParticipants) {
                    TextButton(onClick = viewModel::openParticipants) { Text(Strings.ADMIN_EDIT_PARTICIPANTS) }
                }
            }
            if (group.participants.isEmpty()) {
                Text(Strings.ADMIN_NO_PARTICIPANTS, style = MaterialTheme.typography.bodyMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (participant in group.participants) {
                    val marks = listOfNotNull(
                        Strings.ADMIN_NO_ACCOUNT.takeIf { !participant.hasAccount },
                        Strings.ADMIN_PROFILE_INCOMPLETE.takeIf { participant.hasAccount && !participant.profileComplete },
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (marks.isEmpty()) participant.username else "${participant.username} (${marks.joinToString(", ")})",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (viewModel.canReadPendingWishlists) {
                            TextButton(onClick = { viewModel.openWishlistSheet(participant.username) }) { Text(Strings.ADMIN_OPEN_WISHLIST) }
                        }
                    }
                }
            }
        }
        val step = viewModel.nextStep
        if (step != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val blocked = viewModel.drawNeedsMorePeople
                Button(onClick = { viewModel.ask(step) }, enabled = !busy && !blocked) { Text(step.label()) }
                if (blocked) {
                    Text(Strings.drawNeedsPeople(ActivateGroup.MIN_PARTICIPANTS), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (viewModel.canDelete) {
            TextButton(onClick = { viewModel.ask(AdminStep.Delete) }, enabled = !busy) {
                Text(Strings.DELETE, color = MaterialTheme.colorScheme.error)
            }
        }
    }
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
    ModalBottomSheet(onDismissRequest = viewModel::dismissParticipants, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
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
