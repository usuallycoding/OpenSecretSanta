package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A bottom sheet that scrolls its [content], for the read-only details sheets of a group. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetFrame(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberAppSheetState()) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}
