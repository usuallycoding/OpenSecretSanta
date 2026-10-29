package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.SheetTextField

/**
 * The search field above a group's participants or pairs. It has a control that empties it while it holds text, and the
 * keyboard's search action only puts the keyboard away: the list is narrowed as the person types.
 */
@Composable
internal fun ParticipantSearchField(viewModel: AdminGroupViewModel) {
    val query = viewModel.search
    val focus = LocalFocusManager.current
    SheetTextField(
        value = query,
        onValueChange = viewModel::setSearch,
        label = Strings.ADMIN_SEARCH_PEOPLE,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = viewModel::clearSearch) {
                    Icon(Icons.Filled.Close, contentDescription = Strings.ADMIN_SEARCH_CLEAR)
                }
            }
        } else {
            null
        },
    )
}
