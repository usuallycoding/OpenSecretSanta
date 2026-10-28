package com.usuallycoding.opensecretsanta.ui.load

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.getPlatform
import com.usuallycoding.opensecretsanta.ui.Strings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Shows [content] once there is something to show, and the loading and failure cases of [LoadState] around it.
 * With [onRefresh] and on a platform that has the gesture, pulling the content down reloads it, with the pull
 * indicator shown only for a pull; the thin bar of a quiet reload (returning to the app) is not shown then, so a
 * pull never shows two indicators. The content has to scroll for the pull to work: lists and detail pages do,
 * and an empty screen uses [ScrollableMessage].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> LoadContent(
    state: LoadState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    val value = state.value
    when {
        value != null && onRefresh != null && getPlatform().hasPullToRefresh -> {
            var pulled by remember { mutableStateOf(false) }
            val loading by rememberUpdatedState(state.loading)
            LaunchedEffect(pulled) {
                if (pulled) {
                    // a load that finishes within a frame never shows up as loading: do not stay pulled for it
                    delay(MIN_PULL_MILLIS)
                    snapshotFlow { loading }.first { !it }
                    pulled = false
                }
            }
            PullToRefreshBox(
                isRefreshing = pulled,
                onRefresh = {
                    pulled = true
                    onRefresh()
                },
                modifier = modifier.fillMaxSize(),
            ) {
                Loaded(state, value, onRetry, content, Modifier.fillMaxSize(), showBar = !pulled)
            }
        }
        value != null -> Loaded(state, value, onRetry, content, modifier, showBar = true)
        state.loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else -> Box(modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.error?.loadMessage() ?: Strings.SERVER_FAILED, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text(Strings.TRY_AGAIN) }
            }
        }
    }
}

/** The pull indicator stays at least this long, so a reload that finishes at once still shows that it happened. */
private const val MIN_PULL_MILLIS = 600L

@Composable
private fun <T : Any> Loaded(
    state: LoadState<T>,
    value: T,
    onRetry: () -> Unit,
    content: @Composable (T) -> Unit,
    modifier: Modifier,
    showBar: Boolean,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.loading && showBar) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { error ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(error.loadMessage(), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetry) { Text(Strings.TRY_AGAIN) }
            }
        }
        content(value)
    }
}

/** A short message (an empty list) on a screen that can still be pulled down: it scrolls, so the gesture works. */
@Composable
fun ScrollableMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(24.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
