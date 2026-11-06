package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.usuallycoding.opensecretsanta.ui.form.CountBadged
import com.usuallycoding.opensecretsanta.ui.form.SnowCard
import com.usuallycoding.opensecretsanta.ui.gift.countBadgeText
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverInfo
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.loadMessage

/** The details sheet that is open on a group page. */
enum class PairSheet { Me, Receiver, Santa }

/**
 * Who the person gives to: "TBD" while the group is Pending, then the receiver's username in capitals with the ways to learn
 * about them, the Gifter page on either tab and, last, the details sheet. While Pending there is nothing to press. Nothing here
 * says who the Santa is.
 */
@Composable
internal fun SecretSantaCard(
    phase: GroupPhase,
    receiver: LoadState<ReceiverInfo>?,
    onRetry: () -> Unit,
    onOpenGifter: (questions: Boolean) -> Unit,
    onOpenInbox: () -> Unit,
    onSheet: (PairSheet) -> Unit,
) {
    if (phase != GroupPhase.Pending && phase != GroupPhase.Active && phase != GroupPhase.Archived) return
    val drawn = phase != GroupPhase.Pending
    val name = receiver?.value?.username
    SnowCard("secret-santa", Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Strings.SECRET_SANTA_FOR, style = MaterialTheme.typography.titleMedium)
            when {
                !drawn -> Text(Strings.TBD, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                else -> Text(
                    // capitals are only how it is drawn; a screen reader reads the name as it is written
                    name?.uppercase() ?: "...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = name ?: "..." },
                )
            }
            Text(Strings.KEEP_IT_SECRET, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            if (drawn) {
                if (receiver?.value == null && receiver?.error != null) {
                    Text(receiver.error.loadMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = onRetry) { Text(Strings.TRY_AGAIN) }
                }
                val known = name != null
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onOpenGifter(false) }, enabled = known, modifier = Modifier.weight(1f)) { Text(Strings.SEE_WISHLIST) }
                    Button(onClick = { onOpenGifter(true) }, enabled = known, modifier = Modifier.weight(1f)) { Text(Strings.ASK_QUESTIONS) }
                }
                Button(onClick = onOpenInbox, enabled = known, modifier = Modifier.fillMaxWidth()) { Text(Strings.INBOX) }
                OutlinedButton(onClick = { onSheet(PairSheet.Receiver) }, enabled = known, modifier = Modifier.fillMaxWidth()) { Text(Strings.DELIVERY_INFO) }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOpenReceiver(false) }, modifier = Modifier.weight(1f)) { Text(Strings.EDIT_WISHLIST) }
                if (drawn) {
                    CountBadged(countBadgeText(unanswered), Modifier.weight(1f)) {
                        Button(
                            onClick = { onOpenReceiver(true) },
                            // the badge says nothing of its own, so the button says how many wait
                            modifier = Modifier.fillMaxWidth().semantics { if (unanswered > 0) contentDescription = Strings.withUnanswered(Strings.RESPOND_TO_SANTA, unanswered) },
                        ) { Text(Strings.RESPOND_TO_SANTA) }
                    }
                }
            }
            // from the draw on, the receiver can write to their Santa, who is never named
            if (drawn) Button(onClick = onSendLetter, modifier = Modifier.fillMaxWidth()) { Text(Strings.SEND_LETTER) }
            if (phase == GroupPhase.Archived) {
                OutlinedButton(onClick = { onSheet(PairSheet.Santa) }, modifier = Modifier.fillMaxWidth()) { Text(Strings.REVEAL_SANTA) }
            }
            // a person who still has to give their delivery info is asked to, right above the button that shows it
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (asksForDeliveryInfo) Text(Strings.DELIVERY_INFO_NEEDED, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { onSheet(PairSheet.Me) }, modifier = Modifier.fillMaxWidth()) { Text(Strings.DELIVERY_INFO) }
            }
        }
    }
}
