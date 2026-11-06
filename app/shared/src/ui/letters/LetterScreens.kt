package com.usuallycoding.opensecretsanta.ui.letters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.domain.gift.LetterLimits
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.AppBottomSheet
import com.usuallycoding.opensecretsanta.ui.form.AppFab
import com.usuallycoding.opensecretsanta.ui.form.FAB_CLEARANCE
import com.usuallycoding.opensecretsanta.ui.form.FormColumn
import com.usuallycoding.opensecretsanta.ui.form.MultiLineTextField
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import com.usuallycoding.opensecretsanta.ui.form.SnowCard
import com.usuallycoding.opensecretsanta.ui.form.TimesLine
import com.usuallycoding.opensecretsanta.ui.form.touchesOfAField
import com.usuallycoding.opensecretsanta.ui.groups.GroupGone
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.PageTitle
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadSection
import com.usuallycoding.opensecretsanta.ui.load.PullPage

/** A letter in a card: its title, its text, which is cut when long and opens in full, and when it was sent. */
@Composable
internal fun LetterCard(letter: LetterItem, modifier: Modifier = Modifier) {
    SnowCard("letter-${letter.number}", modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ReadOnlyText(letter.title, style = MaterialTheme.typography.titleMedium, label = Strings.LETTER_TITLE_LABEL, maxLines = 2, copy = false)
            ReadOnlyText(letter.text, style = MaterialTheme.typography.bodyMedium, label = Strings.LETTER_TEXT_LABEL)
            TimesLine(Strings.letterSentAt(letter.sent))
        }
    }
}

/**
 * The letters the person has sent their Santa, newest first, and the round "+" that opens the sheet for a new one while the group is
 * Active and there is room. Nothing here says who the Santa is.
 */
@Composable
fun SendLetterScreen(viewModel: SendLetterViewModel, onBack: () -> Unit) {
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    if (state.isGone()) {
        GroupGone(onBack)
        return
    }
    Box(Modifier.fillMaxSize()) {
        PullPage(loading = state.loading, onRefresh = { viewModel.refresh() }, bottomSpace = FAB_CLEARANCE) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.notice?.let { notice ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(notice.text(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = viewModel::dismissNotice) { Text(Strings.OK) }
                    }
                }
                LoadSection(state, onRetry = { viewModel.refresh() }) { data ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when {
                            data.phase != GroupPhase.Active -> Text(Strings.LETTERS_CLOSED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            !viewModel.canSend -> Text(Strings.letterLimit(LetterLimits.MAX_LETTERS_PER_RECEIVER), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (data.letters.isEmpty()) Text(Strings.NO_LETTERS_SENT, style = MaterialTheme.typography.bodyLarge)
                        for (letter in data.letters) key(letter.number) { LetterCard(letter) }
                    }
                }
            }
        }
        if (state.value != null && viewModel.canSend) AppFab(Icons.Filled.Add, Strings.WRITE_LETTER, onClick = viewModel::openSheet)
    }
    ui.editor?.let { editor ->
        LetterSheet(editor, viewModel::editTitle, viewModel::editText, onSend = { viewModel.save() }, onDismiss = viewModel::dismissEditor)
    }
}

/** The bottom sheet for a new letter: a title and a description, both required, Cancel and Send. */
@Composable
internal fun LetterSheet(
    editor: LetterEditor,
    onTitle: (String) -> Unit,
    onText: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        FormColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(Strings.LETTER_SHEET_TITLE, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = editor.title,
                onValueChange = onTitle,
                label = { Text("${Strings.LETTER_TITLE_LABEL} *") },
                isError = editor.titleError != null,
                supportingText = editor.titleError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().touchesOfAField(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            MultiLineTextField(value = editor.text, onValueChange = onText, label = "${Strings.LETTER_TEXT_LABEL} *", error = editor.textError)
            editor.general?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(Strings.CANCEL) }
                Button(onClick = onSend, enabled = !editor.saving) { Text(if (editor.saving) Strings.SENDING else Strings.SEND) }
            }
        }
    }
}

/** The letters the person's receiver has sent them, newest first; read-only, with no way to reply. */
@Composable
fun InboxScreen(viewModel: InboxViewModel, onBack: () -> Unit, onTitle: (String?) -> Unit) {
    val state by viewModel.data.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    // the receiver's name once it is known; a failed refresh keeps the name that was loaded before
    PageTitle(Strings.lettersFrom(state.value?.receiver), onTitle)

    if (state.isGone()) {
        GroupGone(onBack)
        return
    }
    PullPage(loading = state.loading, onRefresh = { viewModel.refresh() }) {
        LoadSection(state, onRetry = { viewModel.refresh() }) { data ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (data.letters.isEmpty()) Text(Strings.NO_LETTERS_RECEIVED, style = MaterialTheme.typography.bodyLarge)
                for (letter in data.letters) key(letter.number) { LetterCard(letter) }
            }
        }
    }
}

internal fun LetterNotice.text(): String = when (this) {
    LetterNotice.Locked -> Strings.NOTICE_LETTERS_LOCKED
    LetterNotice.LimitReached -> Strings.NOTICE_LETTERS_LIMIT
    LetterNotice.Unreachable -> Strings.SERVER_UNREACHABLE
}
