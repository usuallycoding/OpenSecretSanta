package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import com.usuallycoding.opensecretsanta.ui.layout.STANDARD_CONTENT_WIDTH
import com.usuallycoding.opensecretsanta.ui.layout.LocalWideLayout
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.usuallycoding.opensecretsanta.ui.form.SheetBackHandler
import com.usuallycoding.opensecretsanta.ui.form.AppBottomSheet
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
import com.usuallycoding.opensecretsanta.ui.groups.GroupInfoCard
import com.usuallycoding.opensecretsanta.ui.form.FormColumn
import com.usuallycoding.opensecretsanta.ui.form.SheetTextField
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.PullPage
import com.usuallycoding.opensecretsanta.ui.load.LoadSection
import com.usuallycoding.opensecretsanta.ui.form.GiftIcon
import com.usuallycoding.opensecretsanta.ui.form.ParticipantChip
import com.usuallycoding.opensecretsanta.ui.form.ParticipantChips
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.foundation.BorderStroke
import com.usuallycoding.opensecretsanta.ui.form.GavelIcon
import com.usuallycoding.opensecretsanta.ui.form.KeyIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.style.TextAlign

/** How wide the pane beside the group page is. */
private val PANE_WIDTH = 400.dp

/** One of the administrator's groups: its details and people, and what can be done with it in its state. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AdminGroupScreen(
    viewModel: AdminGroupViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRules: () -> Unit,
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
    // on a wide window a participant's wishlist and a pair open in a pane beside the page, which keeps its place
    val wide = LocalWideLayout.current
    val pane = if (wide) ui.sheet?.takeIf { it.opensInPane() } else null
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = STANDARD_CONTENT_WIDTH).fillMaxSize()) {
                // one page that scrolls as a whole (and can be pulled down to reload): notices, details, buttons, search and people
                PullPage(loading = state.loading, onRefresh = { viewModel.refresh() }) {
                    ui.notice?.let { notice ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(notice.text(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = viewModel::dismissNotice) { Text(Strings.OK) }
                        }
                    }
                    ui.emails?.let { emails ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                Strings.emailSummary(emails.sent, emails.failed, emails.noAddress),
                                modifier = Modifier.weight(1f),
                                color = if (emails.failed > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = viewModel::dismissEmails) { Text(Strings.OK) }
                        }
                    }
                    if (ui.rulesDeleted.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(Strings.rulesDeletedNotice(ui.rulesDeleted.map { it.sentence }), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = viewModel::dismissRulesDeleted) { Text(Strings.OK) }
                        }
                    }
                    ui.drawRefused?.let { message ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { viewModel.dismissDrawRefused(); if (wide) viewModel.openRulesPane() else onRules() }) { Text(Strings.RULES_OPEN) }
                            TextButton(onClick = viewModel::dismissDrawRefused) { Text(Strings.OK) }
                        }
                    }
                    LoadSection(state, onRetry = { viewModel.refresh() }) { group ->
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Header(group, viewModel, ui.busy, onEdit, onRules = { if (wide) viewModel.openRulesPane() else onRules() }) { announcing = true }
                            People(group, viewModel, draw)
                        }
                    }
                }
            }
        }
        if (pane != null) {
            VerticalDivider()
            GroupPane(pane, viewModel.groupId, viewModel.me, draw, onDismiss = viewModel::dismissSheet, modifier = Modifier.width(PANE_WIDTH).fillMaxHeight())
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
    ui.sheet?.takeIf { pane == null || !it.opensInPane() }?.let { GroupSheetHost(it, viewModel.groupId, viewModel.me, draw, viewModel::dismissSheet) }
    ui.confirming?.let { step -> StepDialog(step, state.value?.name.orEmpty(), viewModel) }
    ui.confirmingDelete?.let { impact -> DeleteImpactDialog(impact, viewModel) }
    ui.passwords?.let { passwords ->
        val body = when {
            !ui.passwordsAskedFor -> Strings.PASSWORDS_BODY
            passwords.isEmpty() -> Strings.PASSWORDS_NOBODY
            else -> Strings.PASSWORDS_ASKED_FOR_BODY
        }
        PasswordsDialog(Strings.PASSWORDS_TITLE, body, passwords, viewModel::dismissPasswords)
    }
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

/** The group's details and, in one row, everything that can be done with it: edit, announce, passwords, rules, delete and, last, the next step. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(group: AdminGroup, viewModel: AdminGroupViewModel, busy: Boolean, onEdit: () -> Unit, onRules: () -> Unit, onAnnounce: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReadOnlyText(group.name, style = MaterialTheme.typography.headlineSmall, label = Strings.GROUP_NAME, maxLines = 1, copy = false)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(group.state.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            group.phase.note()?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        GroupInfoCard(group.description, group.whenText, group.location, group.budgetText)
        val step = viewModel.nextStep
        val blocked = viewModel.drawNeedsMorePeople
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // the four actions are icons, each spoken by its label; the step, which moves the group forward, comes last
            if (viewModel.canEditDetails) {
                OutlinedIconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = Strings.ADMIN_EDIT_DETAILS) }
            }
            if (viewModel.canAnnounce) {
                OutlinedIconButton(onClick = onAnnounce) { Icon(Icons.Filled.Notifications, contentDescription = Strings.ANNOUNCE_BUTTON) }
            }
            if (viewModel.canGetPasswords) {
                OutlinedIconButton(onClick = { viewModel.getPasswords() }, enabled = !busy) { Icon(KeyIcon, contentDescription = Strings.GET_PASSWORDS) }
            }
            if (viewModel.canOpenRules) {
                OutlinedIconButton(onClick = onRules) { Icon(GavelIcon, contentDescription = Strings.RULES_BUTTON) }
            }
            if (viewModel.canDelete) {
                OutlinedIconButton(
                    onClick = { viewModel.ask(AdminStep.Delete) },
                    enabled = !busy,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = if (busy) 0.12f else 1f)),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = Strings.DELETE, tint = MaterialTheme.colorScheme.error)
                }
            }
            if (step != null) {
                Button(onClick = { viewModel.ask(step) }, enabled = !busy && !blocked) { Text(step.label()) }
            }
        }
        if (step != null && blocked) {
            Text(Strings.drawNeedsPeople(ActivateGroup.MIN_PARTICIPANTS), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Who takes part, at the end of the page: the participants, or once drawn the pairs. */
@Composable
private fun ColumnScope.People(group: AdminGroup, viewModel: AdminGroupViewModel, draw: DrawState) {
    if (viewModel.canReadDraw) {
        if ((draw as? DrawState.Pairs)?.pairs?.isNotEmpty() == true) ParticipantSearchField(viewModel)
        PairList(
            draw,
            viewModel.search,
            markCounts(group.participants, draft = false),
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
        Text(Strings.ADMIN_NO_PARTICIPANTS, style = MaterialTheme.typography.bodyMedium)
        return
    }
    val draft = group.phase == AdminPhase.Draft
    val legend = legendMarks(group.participants.map { it.marks(draft) })
    MarkCountLine(markCounts(group.participants, draft))
    if (viewModel.canReadPendingWishlists) {
        ParticipantSearchField(viewModel)
        val shown = filterParticipants(group.participants, viewModel.search)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (shown.isEmpty()) Text(Strings.ADMIN_NO_PERSON_MATCH, style = MaterialTheme.typography.bodyMedium)
            for (participant in shown) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { viewModel.openUserSheet(participant.username) },
                        enabled = participant.hasAccount,
                        modifier = Modifier.weight(1f),
                    ) {
                        MarkedName(markedText(participant.username, isMe = false, marks = participant.marks(draft)), Modifier.fillMaxWidth(), TextAlign.Start)
                    }
                    IconButton(onClick = { viewModel.openWishlistSheet(participant.username) }, enabled = participant.hasAccount) {
                        Icon(GiftIcon, contentDescription = Strings.ADMIN_OPEN_WISHLIST)
                    }
                }
            }
            MarkLegend(legend)
        }
    } else {
        // a Draft has no account yet for anyone, so the only mark is the missing account
        ParticipantChips(
            group.participants.map { ParticipantChip(if (it.marks(draft).noAccount) "${it.username} *" else it.username) },
            Strings.ADMIN_LEGEND_NO_ACCOUNT.takeIf { Mark.NoAccount in legend },
        )
    }
}

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

@Composable
private fun ParticipantsSheet(editor: ParticipantsEditor, viewModel: AdminGroupViewModel) {
    AppBottomSheet(onDismissRequest = viewModel::dismissParticipants) {
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
