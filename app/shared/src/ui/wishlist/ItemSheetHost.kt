package com.usuallycoding.opensecretsanta.ui.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.usuallycoding.opensecretsanta.ui.form.AppBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistLimits
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.FormColumn
import com.usuallycoding.opensecretsanta.ui.form.MultiLineTextField
import com.usuallycoding.opensecretsanta.ui.form.SheetTextField

/** The bottom sheet for adding and editing an item, and the confirmation before an item is deleted. */
@Composable
internal fun ItemSheetHost(viewModel: WishlistViewModel) {
    val ui by viewModel.ui.collectAsState()
    val editor = ui.editor
    if (editor != null) {
        AppBottomSheet(onDismissRequest = viewModel::dismissEditor) {
            FormColumn(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (editor.adding) Strings.ADD_ITEM_TITLE else Strings.EDIT_ITEM_TITLE, style = MaterialTheme.typography.titleLarge)
                SheetField(Strings.FIELD_TITLE, editor.title, editor.errors[WishField.Title]) { viewModel.edit(WishField.Title, it) }
                MultiLineTextField(
                    value = editor.note,
                    onValueChange = { viewModel.edit(WishField.Note, it) },
                    label = Strings.FIELD_NOTE,
                    error = editor.errors[WishField.Note],
                )
                LinkFields(editor, viewModel)
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
private fun SheetField(label: String, value: String, error: String?, onChange: (String) -> Unit) {
    SheetTextField(value = value, onValueChange = onChange, label = label, error = error)
}

/** One field per link, each with its own remove button, and the "Add link" button with how many of the most are in use. */
@Composable
private fun LinkFields(editor: ItemEditor, viewModel: WishlistViewModel) {
    editor.urls.forEachIndexed { index, url ->
        SheetTextField(
            value = url,
            onValueChange = { viewModel.editLink(index, it) },
            label = Strings.FIELD_URL,
            error = editor.linkErrors[index],
            trailingIcon = {
                IconButton(onClick = { viewModel.removeLink(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = Strings.REMOVE_LINK)
                }
            },
        )
    }
    editor.linksError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = viewModel::addLink, enabled = editor.canAddLink) { Text(Strings.ADD_LINK) }
        Text(
            if (editor.canAddLink) Strings.linksCount(editor.urls.size, WishlistLimits.MAX_LINKS) else Strings.linksAtMost(WishlistLimits.MAX_LINKS),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}
