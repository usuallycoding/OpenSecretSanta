package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.GiftIcon
import com.usuallycoding.opensecretsanta.ui.form.PairLink
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverInfo
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.loadMessage

/** The details sheet that is open on a group page. */
enum class PairSheet { Me, Receiver, Santa }

/**
 * Who gives to whom, from the person's own side. While the group is Pending only the person themselves and a gift icon
 * for their wishlist; once the names are drawn `me -> receiver` and `Secret Santa -> me`. A username opens a sheet; the
 * pair link between the names opens the Gifter page (the first row) or the Receiver page (the second row). The Santa stays unnamed until the
 * group is Archived.
 */
@Composable
internal fun PairRows(
    phase: GroupPhase,
    me: String,
    receiver: LoadState<ReceiverInfo>?,
    onRetry: () -> Unit,
    onOpenGifter: () -> Unit,
    onOpenReceiver: () -> Unit,
    onSheet: (PairSheet) -> Unit,
) {
    when (phase) {
        GroupPhase.Pending -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            UserButton(me, onClick = { onSheet(PairSheet.Me) }, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenReceiver) { Icon(GiftIcon, contentDescription = Strings.MY_WISHLIST) }
        }
        GroupPhase.Active, GroupPhase.Archived -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                UserButton(me, onClick = { onSheet(PairSheet.Me) }, modifier = Modifier.weight(1f))
                PairLink(Strings.TITLE_GIFTER, onClick = onOpenGifter)
                val name = receiver?.value?.username
                UserButton(name ?: "...", onClick = { onSheet(PairSheet.Receiver) }, modifier = Modifier.weight(1f), enabled = name != null, align = TextAlign.End)
            }
            if (receiver?.value == null && receiver?.error != null) {
                Text(receiver.error.loadMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onRetry) { Text(Strings.TRY_AGAIN) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                UserButton(
                    Strings.SECRET_SANTA,
                    onClick = { onSheet(PairSheet.Santa) },
                    modifier = Modifier.weight(1f),
                    enabled = phase == GroupPhase.Archived,
                )
                PairLink(Strings.TITLE_RECEIVER, onClick = onOpenReceiver)
                UserButton(me, onClick = { onSheet(PairSheet.Me) }, modifier = Modifier.weight(1f), align = TextAlign.End)
            }
        }
        GroupPhase.Other -> Unit
    }
}

@Composable
private fun UserButton(label: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true, align: TextAlign = TextAlign.Start) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = align)
    }
}
