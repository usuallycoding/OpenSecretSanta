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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.domain.gift.QuestionLimits
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadContent
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText

/** The person's receiver, where to post the gift, and the questions the person asked them. */
@Composable
fun ReceiverScreen(viewModel: ReceiverViewModel, onBack: () -> Unit) {
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

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
            ui.notice?.let { Notice(it, onDismiss = viewModel::dismissNotice) }
            LoadContent(state, onRetry = { viewModel.refresh() }, onRefresh = { viewModel.refresh() }) { data ->
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Details(data.receiver) }
                    item { QuestionsHeader(data, viewModel) }
                    if (data.questions.isEmpty()) {
                        item { Text(Strings.NO_QUESTIONS_ASKED, style = MaterialTheme.typography.bodyLarge) }
                    }
                    items(data.questions, key = { it.number }) { question -> QuestionCard(question) }
                }
            }
        }
    }
    ui.editor?.let { editor ->
        TextSheet(Strings.ASK_QUESTION, Strings.YOUR_QUESTION, editor, viewModel::edit, onSave = { viewModel.save() }, onDismiss = viewModel::dismissEditor)
    }
}

@Composable
private fun Notice(notice: GiftNotice, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(notice.text(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onDismiss) { Text(Strings.OK) }
    }
}

/** Where to post the gift: the username and whatever the receiver has set. */
@Composable
private fun Details(receiver: ReceiverInfo) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadOnlyText(receiver.username, style = MaterialTheme.typography.titleMedium, label = Strings.USERNAME)
            if (!receiver.hasPostalDetails) {
                Text(Strings.NO_POSTAL_DETAILS, style = MaterialTheme.typography.bodyMedium)
            }
            Field(Strings.RECEIVER_NAME, receiver.fullName)
            Field(Strings.RECEIVER_ADDRESS, receiver.address)
            Field(Strings.RECEIVER_PHONE, receiver.phone)
            Field(Strings.RECEIVER_DELIVERY, receiver.deliveryInfo)
        }
    }
}

@Composable
private fun Field(label: String, value: String?) {
    if (value == null) return
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ReadOnlyText(value, style = MaterialTheme.typography.bodyLarge, label = label)
    }
}

@Composable
private fun QuestionsHeader(data: ReceiverData, viewModel: ReceiverViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Strings.YOUR_QUESTIONS, style = MaterialTheme.typography.titleMedium)
        when {
            data.phase != GroupPhase.Active -> Text(Strings.THREAD_CLOSED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            !viewModel.canAsk -> Text(Strings.questionLimit(QuestionLimits.MAX_QUESTIONS_PER_SANTA), style = MaterialTheme.typography.bodyMedium)
            else -> Button(onClick = viewModel::openAsk) { Text(Strings.ASK_QUESTION) }
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
