package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.SheetTextField

/**
 * The search field and the state chips above a groups list, driven by [filter]. The field has a control that empties it
 * while it holds text, and the keyboard's search action only puts the keyboard away: the list is narrowed as the person types.
 */
@Composable
fun GroupSearchBar(filter: GroupListFilter, modifier: Modifier = Modifier) {
    val query = filter.query
    val states = filter.states
    val focus = LocalFocusManager.current

    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetTextField(
            value = query,
            onValueChange = filter::setQuery,
            label = Strings.GROUPS_SEARCH,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { filter.setQuery("") }) {
                        Icon(Icons.Filled.Close, contentDescription = Strings.GROUPS_SEARCH_CLEAR)
                    }
                }
            } else {
                null
            },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (chip in filter.chips) {
                FilterChip(selected = chip.key in states, onClick = { filter.toggle(chip.key) }, label = { Text(chip.label) })
            }
        }
    }
}

/** What the list says when there are groups but the search and the chips match none of them; it scrolls, so that pulling down still reloads. */
@Composable
fun NoMatchingGroups(onClear: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(Strings.GROUPS_NO_MATCH, style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onClear) { Text(Strings.GROUPS_NO_MATCH_CLEAR) }
    }
}

/** [groups] narrowed by [filter], recomputed only when the groups, the search or the chips change. */
@Composable
fun <T : GroupListItem> GroupListFilter.shown(groups: List<T>): List<T> {
    val query = query
    val states = states
    return remember(groups, query, states) { filterGroups(groups, query, states) }
}
