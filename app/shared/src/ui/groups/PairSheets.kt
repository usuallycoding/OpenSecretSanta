package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import com.usuallycoding.opensecretsanta.ui.form.SheetFrame
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverInfo
import com.usuallycoding.opensecretsanta.ui.gift.SantaRevealViewModel
import com.usuallycoding.opensecretsanta.ui.load.LoadSection
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The sheet that is open on a group page. Each loads what it shows and offers "Try again" when that fails. */
@Composable
internal fun PairSheets(sheet: PairSheet, groupId: Long, pair: PairViewModel?, onDismiss: () -> Unit) {
    when (sheet) {
        PairSheet.Me -> MySheet(onDismiss)
        PairSheet.Receiver -> if (pair != null) ReceiverSheet(pair, onDismiss)
        PairSheet.Santa -> SantaSheet(groupId, onDismiss)
    }
}

/** What a sheet says when the group is gone or the Santa is not revealed yet: said plainly with a way to close. */
@Composable
private fun Unavailable(message: String, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onDismiss) { Text(Strings.CLOSE) }
    }
}

@Composable
private fun MySheet(onDismiss: () -> Unit) {
    val viewModel = koinViewModel<MyDetailsViewModel>(key = "my-details")
    val state by viewModel.details.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    SheetFrame(onDismiss) {
        LoadSection(state, onRetry = { viewModel.refresh() }) { details ->
            ReadOnlyText(details.username, style = MaterialTheme.typography.headlineSmall, label = Strings.USERNAME)
            if (details.hasProfile) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailField(Strings.USER_FIELD_NAME, details.fullName)
                        DetailField(Strings.USER_FIELD_EMAIL, details.email)
                        DetailField(Strings.RECEIVER_PHONE, details.phone)
                        DetailField(Strings.RECEIVER_ADDRESS, details.address)
                        DetailField(Strings.RECEIVER_DELIVERY, details.deliveryInfo)
                    }
                }
            } else {
                Text(Strings.USER_NO_PROFILE, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ReceiverSheet(pair: PairViewModel, onDismiss: () -> Unit) {
    val state by pair.receiver.collectAsState()
    SheetFrame(onDismiss) {
        if (state.isGone()) {
            Unavailable(Strings.GROUP_GONE, onDismiss)
        } else {
            LoadSection(state, onRetry = { pair.refresh() }) { receiver -> ReceiverDetails(receiver) }
        }
    }
}

/** Where to post the gift: the username and whatever the receiver has set. */
@Composable
private fun ReceiverDetails(receiver: ReceiverInfo) {
    ReadOnlyText(receiver.username, style = MaterialTheme.typography.headlineSmall, label = Strings.USERNAME)
    if (!receiver.hasPostalDetails) {
        Text(Strings.NO_POSTAL_DETAILS, style = MaterialTheme.typography.bodyMedium)
    } else {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailField(Strings.RECEIVER_NAME, receiver.fullName)
                DetailField(Strings.RECEIVER_ADDRESS, receiver.address)
                DetailField(Strings.RECEIVER_PHONE, receiver.phone)
                DetailField(Strings.RECEIVER_DELIVERY, receiver.deliveryInfo)
            }
        }
    }
}

@Composable
private fun SantaSheet(groupId: Long, onDismiss: () -> Unit) {
    val viewModel = koinViewModel<SantaRevealViewModel>(key = "santa-$groupId") { parametersOf(groupId) }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    SheetFrame(onDismiss) {
        when {
            state.isGone() -> Unavailable(Strings.GROUP_GONE, onDismiss)
            viewModel.notYet -> Unavailable(Strings.SANTA_NOT_YET, onDismiss)
            else -> LoadSection(state, onRetry = { viewModel.refresh() }) { santa ->
                ReadOnlyText(santa.username, style = MaterialTheme.typography.headlineSmall, label = Strings.SANTA_USERNAME)
                santa.fullName?.let { DetailField(Strings.SANTA_FULL_NAME, it) }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String?) {
    if (value == null) return
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ReadOnlyText(value, style = MaterialTheme.typography.bodyLarge, label = label)
    }
}
