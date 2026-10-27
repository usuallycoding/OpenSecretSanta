package com.usuallycoding.opensecretsanta.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings

/** The signed-in app: the participant view for everyone, the admin view and a switch for administrators. */
@Composable
fun ShellScreen(viewModel: ShellViewModel) {
    val view by viewModel.view.collectAsState()
    Column(
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(Strings.APP_NAME, style = MaterialTheme.typography.headlineMedium)
        Text(Strings.signedInAs(viewModel.account.username))
        if (viewModel.canSwitch) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = view == AppView.Participant,
                    onClick = { viewModel.select(AppView.Participant) },
                    label = { Text(Strings.PARTICIPANT_VIEW) },
                )
                FilterChip(
                    selected = view == AppView.Admin,
                    onClick = { viewModel.select(AppView.Admin) },
                    label = { Text(Strings.ADMIN_VIEW) },
                )
            }
        }
        Text(
            Strings.currentView(if (view == AppView.Admin) Strings.ADMIN_VIEW else Strings.PARTICIPANT_VIEW),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(if (view == AppView.Admin) Strings.ADMIN_PLACEHOLDER else Strings.PARTICIPANT_PLACEHOLDER)
        OutlinedButton(onClick = viewModel::signOut) { Text(Strings.SIGN_OUT) }
    }
}
