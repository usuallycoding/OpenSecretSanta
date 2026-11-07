package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings

/**
 * A field the person types a password into, with an eye at its end that shows and hides what they typed. Each field has its
 * own state: it starts hidden, and a shown password is hidden again when the app is paused. The keyboard stays a password
 * keyboard either way so it does not start suggesting words.
 */
@Composable
internal fun PasswordField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    ime: ImeAction,
    onDone: () -> Unit = {},
) {
    var shown by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { shown = false }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { shown = !shown }) {
                // the icon shows what choosing it does, and the name says the same
                Icon(
                    if (shown) VisibilityOffIcon else VisibilityIcon,
                    contentDescription = if (shown) Strings.HIDE_PASSWORD else Strings.SHOW_PASSWORD,
                )
            }
        },
        colors = appFieldColors(),
        modifier = Modifier.fillMaxWidth().touchesOfAField(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ime),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}
