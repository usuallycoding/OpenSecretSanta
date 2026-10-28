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
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadContent
import com.usuallycoding.opensecretsanta.ui.load.ScrollableMessage

/** The questions the person's Santa asked and the person's answers. Nothing here says who the Santa is. */
@Composable
fun SantaQuestionsScreen(viewModel: SantaQuestionsViewModel, onBack: () -> Unit) {
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
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                val data = state.value
                Text(
                    if (data != null && !data.canAnswer) Strings.THREAD_CLOSED else "",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                    }
            ui.notice?.let { notice ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(notice.text(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::dismissNotice) { Text(Strings.OK) }
                }
            }
            LoadContent(state, onRetry = { viewModel.refresh() }, onRefresh = { viewModel.refresh() }) { data ->
                if (data.questions.isEmpty()) {
                    ScrollableMessage(Strings.NO_SANTA_QUESTIONS)
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(data.questions, key = { it.number }) { question ->
                            QuestionCard(question, action = if (data.canAnswer) {
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
        }
    }
    ui.editor?.let { editor ->
        TextSheet(Strings.ANSWER, Strings.YOUR_ANSWER, editor, viewModel::edit, onSave = { viewModel.save() }, onDismiss = viewModel::dismissEditor)
    }
}
