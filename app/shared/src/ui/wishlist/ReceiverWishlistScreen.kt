package com.usuallycoding.opensecretsanta.ui.wishlist

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
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.load.LoadSection

/** What the person's receiver wants, in the receiver's order, as a section of the Gifter page. Nothing here can be changed. */
@Composable
fun ReceiverWishlistSection(viewModel: ReceiverWishlistViewModel) {
    val state by viewModel.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Strings.RECEIVER_WISHLIST, style = MaterialTheme.typography.titleMedium)
        LoadSection(state, onRetry = { viewModel.refresh() }) { items ->
            if (items.isEmpty()) {
                Text(Strings.RECEIVER_WISHLIST_EMPTY, style = MaterialTheme.typography.bodyLarge)
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (item in items) {
                        Card(Modifier.fillMaxWidth()) {
                            ItemText(item.title, item.note, item.url, Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
