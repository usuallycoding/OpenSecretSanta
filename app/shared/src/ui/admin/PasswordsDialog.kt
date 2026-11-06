package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.CopyAction
import com.usuallycoding.opensecretsanta.ui.form.CopyButton
import com.usuallycoding.opensecretsanta.ui.form.CopyStatus
import com.usuallycoding.opensecretsanta.ui.form.clipboardAvailable
import com.usuallycoding.opensecretsanta.ui.form.plainTextClip
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

/** What a row shows and its copy icon copies: `username: password`. */
internal fun passwordLine(account: CreatedAccount): String = "${account.username}: ${account.temporaryPassword}"

/** What "Copy all" puts on the clipboard: the line of each account, in the order shown. */
internal fun allPasswordsText(accounts: List<CreatedAccount>): String = accounts.joinToString("\n", transform = ::passwordLine)

/** "Copy all" is offered for two or more people; for one it would only repeat the row's own icon. */
internal fun offersCopyAll(accounts: List<CreatedAccount>): Boolean = accounts.size >= 2

/**
 * Temporary passwords to hand over, each with its username, selectable so they can be copied by hand, with a copy icon that copies
 * the row and, for two or more people, "Copy all" beside "OK". Closing it forgets them.
 */
@Composable
internal fun PasswordsDialog(title: String, body: String, accounts: List<CreatedAccount>, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (account in accounts) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            SelectionContainer(Modifier.weight(1f)) {
                                Text(passwordLine(account), style = MaterialTheme.typography.bodyLarge)
                            }
                            CopyButton(passwordLine(account))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(Strings.OK) } },
        dismissButton = if (offersCopyAll(accounts)) {
            { CopyAllButton(allPasswordsText(accounts)) }
        } else {
            null
        },
    )
}

/** A text button that copies [text] and says for a moment that it did, or that it could not. */
@Composable
private fun CopyAllButton(text: String) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val action = remember(clipboard) {
        CopyAction({
            // the web's clipboard does nothing, and says nothing, on a page that is not secure
            check(clipboardAvailable()) { "no clipboard" }
            clipboard.setClipEntry(plainTextClip(it))
        })
    }
    val status by action.status.collectAsState()
    TextButton(onClick = { scope.launch { action.copy(text) } }) {
        when (status) {
            CopyStatus.Idle -> Text(Strings.COPY_ALL)
            CopyStatus.Copied -> Text(Strings.COPIED)
            CopyStatus.Failed -> Text(Strings.COPY_FAILED, color = MaterialTheme.colorScheme.error)
        }
    }
}
