package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.usuallycoding.opensecretsanta.ui.load.LoadContent

/** One group: its details, what its state means and who takes part. Nothing here says who gives to whom. */
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenReceiverWishlist: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
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
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { viewModel.refresh() }) { Text(Strings.REFRESH) }
            }
            LoadContent(state, onRetry = { viewModel.refresh() }) { detail -> Detail(detail, onOpenWishlist, onOpenReceiverWishlist) }
        }
    }
}

@Composable
private fun Detail(group: GroupDetail, onOpenWishlist: () -> Unit, onOpenReceiverWishlist: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(group.name, style = MaterialTheme.typography.headlineSmall)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(group.state.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            group.state.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        group.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        group.whenText?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        group.location?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        Text("${Strings.GROUP_PARTICIPANTS} (${group.participants.size})", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (participant in group.participants) {
                Text(if (participant.isMe) "${participant.username} (${Strings.YOU})" else participant.username)
            }
        }
        Wishlists(group.phase, onOpenWishlist, onOpenReceiverWishlist)
    }
}

/** Where the person's own wishlist and, once the names are drawn, their receiver's can be opened. */
@Composable
private fun Wishlists(phase: GroupPhase, onOpenWishlist: () -> Unit, onOpenReceiverWishlist: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Strings.WISHLISTS, style = MaterialTheme.typography.titleMedium)
        WishlistEntry(Strings.MY_WISHLIST, onOpenWishlist)
        when (phase) {
            GroupPhase.Active, GroupPhase.Archived -> WishlistEntry(Strings.RECEIVER_WISHLIST, onOpenReceiverWishlist)
            GroupPhase.Pending -> Text(Strings.RECEIVER_WISHLIST_LATER, style = MaterialTheme.typography.bodyMedium)
            GroupPhase.Other -> Unit
        }
    }
}

@Composable
private fun WishlistEntry(label: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp))
    }
}
