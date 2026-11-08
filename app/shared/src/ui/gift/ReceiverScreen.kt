package com.usuallycoding.opensecretsanta.ui.gift

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.domain.gift.QuestionLimits
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.SnowCard
import com.usuallycoding.opensecretsanta.ui.form.startAccentBar
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.load.LazyPullPage
import com.usuallycoding.opensecretsanta.ui.load.loadItems
import androidx.compose.foundation.layout.BoxScope
import com.usuallycoding.opensecretsanta.ui.form.AppFab

/** The person's questions to their receiver and the receiver's answers, as a page of their own; [bottomSpace] keeps the last one clear of a floating button. */
@Composable
fun ReceiverQuestionsSection(viewModel: ReceiverViewModel, shown: Boolean = true, bottomSpace: Dp = 0.dp) {
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    // the answers are marked as seen once they are on screen, not when the page opens on the other tab
    LaunchedEffect(shown, state.value) { if (shown && state.value != null) viewModel.markSeen() }

    LazyPullPage(loading = state.loading, onRefresh = { viewModel.refresh() }, bottomSpace = bottomSpace) {
        ui.notice?.let { item(key = "notice") { Notice(it, onDismiss = viewModel::dismissNotice) } }
        loadItems(state, onRetry = { viewModel.refresh() }) { data ->
            if (data.phase != GroupPhase.Active || !viewModel.canAsk) item(key = "header") { QuestionsHeader(data, viewModel) }
            if (data.questions.isEmpty()) item(key = "none") { Text(Strings.NO_QUESTIONS_ASKED, style = MaterialTheme.typography.bodyLarge) }
            items(data.questions, key = { it.number }) { question -> QuestionCard(question) }
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
        AppFab(Icons.AutoMirrored.Filled.Send, Strings.ASK_QUESTION, onClick = viewModel::openAsk)
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
internal fun QuestionCard(question: Question, action: (@Composable () -> Unit)? = null, needsAnswer: Boolean = false) {
    SnowCard("question-${question.number}", Modifier.fillMaxWidth()) {
        // the accent bar along the card's start edge: for a question that needs an answer, or an answer the Santa has not read yet
        Column(Modifier.startAccentBar(needsAnswer || question.answerIsNew).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            QuestionThread(question.number, question.text, question.asked, question.answer, question.answered, question.answerChanged, needsAnswer = needsAnswer, answerIsNew = question.answerIsNew)
            action?.invoke()
        }
    }
}
