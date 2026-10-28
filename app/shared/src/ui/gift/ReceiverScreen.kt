package com.usuallycoding.opensecretsanta.ui.gift

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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.domain.gift.QuestionLimits
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.load.LoadSection
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import androidx.compose.foundation.layout.BoxScope
import com.usuallycoding.opensecretsanta.ui.form.AppFab
import com.usuallycoding.opensecretsanta.ui.form.QuestionIcon

/** The person's questions to their receiver and the receiver's answers, as a section of the Gifter page. */
@Composable
fun ReceiverQuestionsSection(viewModel: ReceiverViewModel) {
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ui.notice?.let { Notice(it, onDismiss = viewModel::dismissNotice) }
        LoadSection(state, onRetry = { viewModel.refresh() }) { data ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuestionsHeader(data, viewModel)
                if (data.questions.isEmpty()) {
                    Text(Strings.NO_QUESTIONS_ASKED, style = MaterialTheme.typography.bodyLarge)
                }
                for (question in data.questions) {
                    key(question.number) { QuestionCard(question) }
                }
            }
        }
    }
    ui.editor?.let { editor ->
        TextSheet(Strings.ASK_QUESTION, Strings.YOUR_QUESTION, editor, viewModel::edit, onSave = { viewModel.save() }, onDismiss = viewModel::dismissEditor)
    }
}

/** The floating "Ask a question" button: there only while the group is Active and the person has questions left. */
@Composable
fun BoxScope.AskQuestionFab(viewModel: ReceiverViewModel) {
    val state by viewModel.data.collectAsState()
    if (state.value != null && viewModel.canAsk) {
        AppFab(QuestionIcon, Strings.ASK_QUESTION, onClick = viewModel::openAsk)
    }
}

@Composable
private fun Notice(notice: GiftNotice, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(notice.text(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onDismiss) { Text(Strings.OK) }
    }
}

@Composable
private fun QuestionsHeader(data: ReceiverData, viewModel: ReceiverViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            data.phase != GroupPhase.Active -> Text(Strings.THREAD_CLOSED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            !viewModel.canAsk -> Text(Strings.questionLimit(QuestionLimits.MAX_QUESTIONS_PER_SANTA), style = MaterialTheme.typography.bodyMedium)
            else -> Unit
        }
    }
}

@Composable
internal fun QuestionCard(question: Question, action: (@Composable () -> Unit)? = null) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ReadOnlyText("${question.number}. ${question.text}", style = MaterialTheme.typography.titleSmall, label = Strings.QUESTION_LABEL)
            if (question.answer != null) {
                ReadOnlyText(question.answer, style = MaterialTheme.typography.bodyMedium, label = Strings.ANSWER)
            } else {
                Text(Strings.NOT_ANSWERED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            action?.invoke()
        }
    }
}
