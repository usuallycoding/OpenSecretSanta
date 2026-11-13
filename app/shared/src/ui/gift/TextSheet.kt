package com.usuallycoding.opensecretsanta.ui.gift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.usuallycoding.opensecretsanta.ui.form.AppBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.FormColumn
import com.usuallycoding.opensecretsanta.ui.form.MultiLineTextField

/** The bottom sheet for writing a question or an answer: one multi-line field, Cancel and Save. */
@Composable
internal fun TextSheet(
    title: String,
    label: String,
    editor: TextEditor,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        FormColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            MultiLineTextField(value = editor.text, onValueChange = onChange, label = label, error = editor.error)
            editor.general?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(Strings.CANCEL) }
                Button(onClick = onSave, enabled = !editor.saving) { Text(if (editor.saving) Strings.SAVING else Strings.SAVE) }
            }
        }
    }
}

internal fun GiftNotice.text(): String = when (this) {
    GiftNotice.Locked -> Strings.NOTICE_THREAD_LOCKED
    GiftNotice.LimitReached -> Strings.NOTICE_LIMIT_REACHED
    GiftNotice.QuestionGone -> Strings.NOTICE_QUESTION_GONE
    GiftNotice.QuestionAnswered -> Strings.NOTICE_QUESTION_ANSWERED
    GiftNotice.Unreachable -> Strings.SERVER_UNREACHABLE
    GiftNotice.Failed -> Strings.SERVER_FAILED
}
