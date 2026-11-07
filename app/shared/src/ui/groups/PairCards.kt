package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import com.usuallycoding.opensecretsanta.ui.form.GiftIcon
import com.usuallycoding.opensecretsanta.ui.form.MailIcon
import com.usuallycoding.opensecretsanta.ui.form.PlaceIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.SnowCard
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverInfo
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.readmarks.UnreadCounts
import com.usuallycoding.opensecretsanta.ui.load.loadMessage

/** The details sheet that is open on a group page. */
enum class PairSheet { Me, Receiver, Santa }

/**
 * Who the person gives to, once the names are drawn: the receiver's username in capitals with the ways to learn about them, the
 * Gifter page on either tab and, last, the details sheet. A Pending group has no such card. Nothing here says who the Santa is.
 */
@Composable
internal fun SecretSantaCard(
    phase: GroupPhase,
    receiver: LoadState<ReceiverInfo>?,
    unread: UnreadCounts?,
    onRetry: () -> Unit,
    onOpenGifter: (questions: Boolean) -> Unit,
    onOpenInbox: () -> Unit,
    onSheet: (PairSheet) -> Unit,
) {
    // before the draw there is no receiver to tell about
    if (phase != GroupPhase.Active && phase != GroupPhase.Archived) return
    val name = receiver?.value?.username
    SnowCard("secret-santa", Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Strings.SECRET_SANTA_FOR, style = MaterialTheme.typography.titleMedium)
            Text(
                // capitals are only how it is drawn; a screen reader reads the name as it is written
                name?.uppercase() ?: "...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clearAndSetSemantics { contentDescription = name ?: "..." },
            )
            Text(Strings.KEEP_IT_SECRET, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            if (receiver?.value == null && receiver?.error != null) {
                Text(receiver.error.loadMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onRetry) { Text(Strings.TRY_AGAIN) }
            }
            val known = name != null
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UnreadIconButton(GiftIcon, Strings.SEE_WISHLIST, unread?.wishlist ?: 0, Strings::withNewOrEdited, { onOpenGifter(false) }, known)
                UnreadIconButton(Icons.AutoMirrored.Filled.Send, Strings.ASK_QUESTIONS, unread?.answers ?: 0, Strings::withNew, { onOpenGifter(true) }, known)
                UnreadIconButton(MailIcon, Strings.INBOX, unread?.letters ?: 0, Strings::withNew, onOpenInbox, known)
                CardIconButton(PlaceIcon, Strings.DELIVERY_INFO, { onSheet(PairSheet.Receiver) }, enabled = known)
            }
        }
    }
}

/**
 * What is the person's own: the Receiver page on either tab and, last, their details sheet. "Respond to Santa" carries the number
 * of questions [unanswered] when there are some, and [asksForDeliveryInfo] puts a message above "Delivery Info". The questions tab exists from Active on,
 * and the Santa can be revealed only once the group is Archived, so nothing earlier offers either.
 */
@Composable
internal fun YourStuffCard(
    phase: GroupPhase,
    unanswered: Int,
    asksForDeliveryInfo: Boolean,
    onOpenReceiver: (questions: Boolean) -> Unit,
    onSendLetter: () -> Unit,
    onSheet: (PairSheet) -> Unit,
) {
    if (phase != GroupPhase.Pending && phase != GroupPhase.Active && phase != GroupPhase.Archived) return
    val drawn = phase != GroupPhase.Pending
    SnowCard("your-stuff", Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Strings.YOUR_STUFF, style = MaterialTheme.typography.titleMedium)
            Text(Strings.TELL_YOUR_SANTA, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            // the Santa can be revealed only once the group is Archived; the text button stays above the row of icons
            if (phase == GroupPhase.Archived) {
                OutlinedButton(onClick = { onSheet(PairSheet.Santa) }, modifier = Modifier.fillMaxWidth()) { Text(Strings.REVEAL_SANTA) }
            }
            // a person who still has to give their delivery info is asked to, right above the row, whose Delivery Info button turns red with it
            if (asksForDeliveryInfo) Text(Strings.DELIVERY_INFO_NEEDED, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardIconButton(GiftIcon, Strings.EDIT_WISHLIST, { onOpenReceiver(false) })
                if (drawn) {
                    UnreadIconButton(Icons.AutoMirrored.Filled.Send, Strings.RESPOND_TO_SANTA, unanswered, Strings::withUnanswered, { onOpenReceiver(true) })
                    // from the draw on, the receiver can write to their Santa, who is never named
                    CardIconButton(MailIcon, Strings.SEND_LETTER, onSendLetter)
                }
                CardIconButton(PlaceIcon, Strings.DELIVERY_INFO, { onSheet(PairSheet.Me) }, error = asksForDeliveryInfo)
            }
        }
    }
}
