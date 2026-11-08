package com.usuallycoding.opensecretsanta.ui.gift

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.load.LazyPullPage
import com.usuallycoding.opensecretsanta.ui.load.loadItems

/** The questions the person's Santa asked and the person's answers, as a page of their own. Nothing here says who the Santa is. */
@Composable
fun SantaQuestionsSection(viewModel: SantaQuestionsViewModel) {
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    LazyPullPage(loading = state.loading, onRefresh = { viewModel.refresh() }) {
        val data = state.value
        if (data != null && !data.canAnswer) {
            item(key = "closed") {
                Text(Strings.THREAD_CLOSED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        ui.notice?.let { notice ->
            item(key = "notice") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(notice.text(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::dismissNotice) { Text(Strings.OK) }
                }
            }
        }
        loadItems(state, onRetry = { viewModel.refresh() }) { loaded ->
            if (loaded.questions.isEmpty()) {
                item(key = "none") { Text(Strings.NO_SANTA_QUESTIONS, style = MaterialTheme.typography.bodyLarge) }
            } else {
                items(loaded.questions, key = { it.number }) { question ->
                    QuestionCard(question, needsAnswer = loaded.canAnswer && question.answer == null, action = if (loaded.canAnswer) {
                        {
                            TextButton(onClick = { viewModel.openAnswer(question.number) }) {
                                Text(if (question.answer == null) Strings.ANSWER else Strings.CHANGE_ANSWER)
                            }
                        }
                    } else {
                        null
                    })
                }
            }
        }
    }
    ui.editor?.let { editor ->
        TextSheet(Strings.ANSWER, Strings.YOUR_ANSWER, editor, viewModel::edit, onSave = { viewModel.save() }, onDismiss = viewModel::dismissEditor)
    }
}
