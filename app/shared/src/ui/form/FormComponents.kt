package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** A multi-line field grows with its text up to this many rows, then stops growing and scrolls inside itself. */
const val MULTILINE_MAX_ROWS = 3

/**
 * A text field that owns its selection. The text still lives in the caller (it comes in as [value] and goes out
 * through [onValueChange]), but the cursor and selection are kept here, in a [TextFieldValue], and are not rebuilt
 * from the string that comes back after every keystroke. On iOS, in a bottom sheet, the cursor jumped to the start
 * after the first typed character when the field was driven by the string alone.
 */
@Composable
fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else MULTILINE_MAX_ROWS,
    error: String? = null,
    hint: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }
    // what the caller says the text is changed behind the field's back (cleared, loaded): take it over, cursor at the end
    LaunchedEffect(value) {
        if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    }
    OutlinedTextField(
        value = field,
        onValueChange = {
            field = it
            if (it.text != value) onValueChange(it.text)
        },
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The one multi-line text field of the app. Return is a line break (the default action of the keyboard),
 * the field is [minLines] tall while its text is short and never taller than [MULTILINE_MAX_ROWS].
 */
@Composable
fun MultiLineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
    error: String? = null,
    hint: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    SheetTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        singleLine = false,
        minLines = minLines,
        maxLines = MULTILINE_MAX_ROWS,
        error = error,
        hint = hint,
        keyboardOptions = keyboardOptions,
    )
}

/** Whether a finger is on the screen; read by the scroll connection, never observed, so it causes no recomposition. */
private class FingerTracker {
    var down = false
}

/**
 * Closes the keyboard when the person's own drag scrolled [scrollState]. Two things tell that apart from the other
 * scrolling a form does: the scroll position changes (a field scrolling its own text does not move the form), and a
 * finger is down (when typing makes a field grow, the framework scrolls the form to keep the cursor in view, and
 * reports that scroll as user input too, with no finger on the screen).
 * It must sit above the modifier that scrolls.
 */
@Composable
fun Modifier.dismissKeyboardOnDrag(scrollState: ScrollState): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val finger = remember { FingerTracker() }
    val connection = remember(scrollState, focusManager, keyboard) {
        object : NestedScrollConnection {
            private var before = 0

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                before = scrollState.value
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (finger.down && source == NestedScrollSource.UserInput && scrollState.value != before) {
                    focusManager.clearFocus()
                    keyboard?.hide()
                }
                return Offset.Zero
            }
        }
    }
    return pointerInput(finger) {
        // watches the touches without consuming them, before anything below sees them
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                finger.down = event.changes.any { it.pressed }
            }
        }
    }.nestedScroll(connection)
}

/**
 * The content of a form: lifted above the keyboard, scrolling, and the keyboard closes when the person drags it,
 * so the form's primary button can always be reached.
 */
@Composable
fun FormColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier.dismissKeyboardOnDrag(scrollState).imePadding().verticalScroll(scrollState).padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
