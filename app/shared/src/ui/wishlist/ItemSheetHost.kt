package com.usuallycoding.opensecretsanta.ui.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings

/** The bottom sheet for adding and editing an item, and the confirmation before an item is deleted. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemSheetHost(viewModel: WishlistViewModel) {
    val ui by viewModel.ui.collectAsState()
    val editor = ui.editor
    if (editor != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissEditor,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (editor.adding) Strings.ADD_ITEM_TITLE else Strings.EDIT_ITEM_TITLE, style = MaterialTheme.typography.titleLarge)
                SheetField(Strings.FIELD_TITLE, editor.title, editor.errors[WishField.Title], singleLine = true) { viewModel.edit(WishField.Title, it) }
                SheetField(Strings.FIELD_NOTE, editor.note, editor.errors[WishField.Note], singleLine = false) { viewModel.edit(WishField.Note, it) }
                SheetField(Strings.FIELD_URL, editor.url, editor.errors[WishField.Url], singleLine = true) { viewModel.edit(WishField.Url, it) }
                editor.general?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!editor.adding) {
                        TextButton(onClick = viewModel::requestDelete, enabled = !editor.saving) { Text(Strings.DELETE, color = MaterialTheme.colorScheme.error) }
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = viewModel::dismissEditor) { Text(Strings.CANCEL) }
                        Button(onClick = { viewModel.save() }, enabled = !editor.saving) { Text(if (editor.saving) Strings.SAVING else Strings.SAVE) }
                    }
                }
            }
        }
    }
    ui.deleting?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(Strings.DELETE_ITEM_TITLE) },
            text = { Text(Strings.deleteItemBody(item.title)) },
            confirmButton = { TextButton(onClick = { viewModel.confirmDelete() }) { Text(Strings.DELETE, color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(Strings.CANCEL) } },
        )
    }
}

@Composable
private fun SheetField(label: String, value: String, error: String?, singleLine: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        maxLines = if (singleLine) 1 else 6,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
}
