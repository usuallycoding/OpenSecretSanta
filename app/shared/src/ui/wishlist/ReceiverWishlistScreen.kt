package com.usuallycoding.opensecretsanta.ui.wishlist

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.SnowCard
import com.usuallycoding.opensecretsanta.ui.form.startAccentBar
import com.usuallycoding.opensecretsanta.ui.readmarks.Change
import com.usuallycoding.opensecretsanta.ui.load.LoadSection

/** What the person's receiver wants, in the receiver's order, as a section of the Gifter page. Nothing here can be changed. */
@Composable
fun ReceiverWishlistSection(viewModel: ReceiverWishlistViewModel, shown: Boolean = true) {
    val state by viewModel.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    // the list is marked as seen once it is on screen, not when the page opens on the other tab
    LaunchedEffect(shown, state.value) { if (shown && state.value != null) viewModel.markSeen() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LoadSection(state, onRetry = { viewModel.refresh() }) { items ->
            if (items.isEmpty()) {
                Text(Strings.RECEIVER_WISHLIST_EMPTY, style = MaterialTheme.typography.bodyLarge)
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for ((index, item) in items.withIndex()) {
                        SnowCard("wish-$index", Modifier.fillMaxWidth()) {
                            // what is new or edited since the Santa last looked has a pill and a bar on the card's start edge
                            Column(Modifier.startAccentBar(item.change != null)) {
                                ItemText(
                                    index + 1, item.title, item.note, item.url, item.added, item.changed, Modifier.padding(16.dp),
                                    pill = when (item.change) {
                                        Change.New -> Strings.READ_NEW
                                        Change.Edited -> Strings.READ_EDITED
                                        null -> null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
