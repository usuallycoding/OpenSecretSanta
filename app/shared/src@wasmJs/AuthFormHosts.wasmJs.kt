package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.usuallycoding.opensecretsanta.APP_VERSION
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.download.androidDownloadAction
import com.usuallycoding.opensecretsanta.ui.login.LoginUiState
import com.usuallycoding.opensecretsanta.ui.login.LoginViewModel
import com.usuallycoding.opensecretsanta.ui.login.logoIsDark
import com.usuallycoding.opensecretsanta.ui.login.text
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordUiState
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordViewModel
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLHeadingElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLabelElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.roundToInt

private const val EYE_SVG =
    """<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>"""
private const val EYE_OFF_SVG =
    """<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>"""

private fun Color.toCssHex(): String {
    val r = (red * 255f).roundToInt().coerceIn(0, 255)
    val g = (green * 255f).roundToInt().coerceIn(0, 255)
    val b = (blue * 255f).roundToInt().coerceIn(0, 255)
    return "#" + r.toString(16).padStart(2, '0') + g.toString(16).padStart(2, '0') + b.toString(16).padStart(2, '0')
}

private fun applyTheme(root: HTMLElement, colors: ColorScheme) {
    val style = root.style
    style.setProperty("--oss-bg", colors.background.toCssHex())
    style.setProperty("--oss-surface", colors.surface.toCssHex())
    style.setProperty("--oss-surface-container", colors.surfaceContainerLowest.toCssHex())
    style.setProperty("--oss-on-surface", colors.onSurface.toCssHex())
    style.setProperty("--oss-on-surface-variant", colors.onSurfaceVariant.toCssHex())
    style.setProperty("--oss-primary", colors.primary.toCssHex())
    style.setProperty("--oss-on-primary", colors.onPrimary.toCssHex())
    style.setProperty("--oss-outline", colors.outline.toCssHex())
    style.setProperty("--oss-error", colors.error.toCssHex())
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun isDocumentHidden(): Boolean = js("document.hidden === true")

internal class WebPasswordFieldHandle(
    val container: HTMLDivElement,
    val input: HTMLInputElement,
    val toggleButton: HTMLButtonElement,
    val errorText: HTMLParagraphElement,
) {
    private var shown = false

    fun setShown(value: Boolean) {
        shown = value
        input.type = if (shown) "text" else "password"
        toggleButton.setAttribute("aria-label", if (shown) Strings.HIDE_PASSWORD else Strings.SHOW_PASSWORD)
        toggleButton.innerHTML = if (shown) EYE_OFF_SVG else EYE_SVG
    }

    fun toggle() = setShown(!shown)

    fun syncValue(value: String) {
        if (input.value != value) input.value = value
    }

    fun syncError(error: String?) {
        if (error == null) {
            errorText.style.display = "none"
            errorText.textContent = ""
            input.classList.remove("oss-input-error")
        } else {
            errorText.style.display = "block"
            errorText.textContent = error
            input.classList.add("oss-input-error")
        }
    }
}

private fun createPasswordField(
    id: String,
    label: String,
    name: String,
    autocomplete: String,
    onChange: (String) -> Unit,
): WebPasswordFieldHandle {
    val container = document.createElement("div") as HTMLDivElement
    container.className = "oss-field"

    val labelEl = document.createElement("label") as HTMLLabelElement
    labelEl.className = "oss-field-label"
    labelEl.htmlFor = id
    labelEl.textContent = label

    val wrap = document.createElement("div") as HTMLDivElement
    wrap.className = "oss-input-wrap"

    val input = document.createElement("input") as HTMLInputElement
    input.id = id
    input.name = name
    input.type = "password"
    input.className = "oss-field-input oss-input-password"
    input.setAttribute("autocomplete", autocomplete)
    input.setAttribute("autocapitalize", "none")
    input.setAttribute("spellcheck", "false")
    val syncFromDom: (Event) -> Unit = { onChange(input.value) }
    input.addEventListener("input", syncFromDom)
    input.addEventListener("change", syncFromDom)

    val eyeBtn = document.createElement("button") as HTMLButtonElement
    eyeBtn.type = "button"
    eyeBtn.className = "oss-eye-btn"

    val errorEl = document.createElement("p") as HTMLParagraphElement
    errorEl.className = "oss-error"
    errorEl.style.display = "none"

    val handle = WebPasswordFieldHandle(container, input, eyeBtn, errorEl)
    handle.setShown(false)
    eyeBtn.addEventListener("click", { handle.toggle() })

    wrap.appendChild(input)
    wrap.appendChild(eyeBtn)
    container.appendChild(labelEl)
    container.appendChild(wrap)
    container.appendChild(errorEl)
    return handle
}

internal class WebLoginFormHandle(
    val overlay: HTMLDivElement,
    val form: HTMLFormElement,
    val logo: HTMLImageElement,
    val sessionNotice: HTMLParagraphElement,
    val usernameInput: HTMLInputElement,
    val passwordField: WebPasswordFieldHandle,
    val stayCheckbox: HTMLInputElement,
    val errorText: HTMLParagraphElement,
    val submitButton: HTMLButtonElement,
    val downloadButton: HTMLButtonElement,
    private val visibilityListener: (Event) -> Unit,
) {
    var onDownload: (() -> Unit)? = null

    fun sync(state: LoginUiState, sessionEnded: Boolean, darkLogo: Boolean, downloadAction: (() -> Unit)?) {
        val desiredLogo = if (darkLogo) "app_logo_dark.png" else "app_logo_light.png"
        if (logo.getAttribute("src") != desiredLogo) logo.src = desiredLogo
        sessionNotice.style.display = if (sessionEnded) "block" else "none"
        if (usernameInput.value != state.username) usernameInput.value = state.username
        passwordField.syncValue(state.password)
        if (stayCheckbox.checked != state.staySignedIn) stayCheckbox.checked = state.staySignedIn

        val problemText = state.problem?.text()
        if (problemText == null) {
            errorText.style.display = "none"
            errorText.textContent = ""
        } else {
            errorText.style.display = "block"
            errorText.textContent = problemText
        }

        submitButton.disabled = !state.canSubmit
        submitButton.textContent = if (state.inProgress) Strings.SIGNING_IN else Strings.SIGN_IN

        onDownload = downloadAction
        downloadButton.style.display = if (downloadAction != null) "inline-block" else "none"
    }

    fun hidePasswords() = passwordField.setShown(false)

    fun dispose() {
        document.removeEventListener("visibilitychange", visibilityListener)
        overlay.remove()
    }
}

internal fun mountWebLoginForm(
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onStaySignedInChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    container: HTMLElement = document.body ?: error("Missing document.body"),
): WebLoginFormHandle {
    val overlay = document.createElement("div") as HTMLDivElement
    overlay.className = "oss-auth-overlay"

    val form = document.createElement("form") as HTMLFormElement
    form.className = "oss-auth-form"
    form.method = "post"
    form.noValidate = true

    val logo = document.createElement("img") as HTMLImageElement
    logo.className = "oss-auth-logo"
    logo.alt = Strings.APP_NAME
    logo.src = "app_logo_light.png"

    val notice = document.createElement("p") as HTMLParagraphElement
    notice.className = "oss-auth-notice"
    notice.textContent = Strings.SESSION_ENDED
    notice.style.display = "none"

    val userField = document.createElement("div") as HTMLDivElement
    userField.className = "oss-field"
    val userLabel = document.createElement("label") as HTMLLabelElement
    userLabel.className = "oss-field-label"
    userLabel.htmlFor = "oss-login-username"
    userLabel.textContent = Strings.USERNAME
    val usernameInput = document.createElement("input") as HTMLInputElement
    usernameInput.id = "oss-login-username"
    usernameInput.name = "username"
    usernameInput.type = "text"
    usernameInput.className = "oss-field-input"
    usernameInput.setAttribute("autocomplete", "username")
    usernameInput.setAttribute("autocapitalize", "none")
    usernameInput.setAttribute("spellcheck", "false")
    val syncUser: (Event) -> Unit = { onUsernameChange(usernameInput.value) }
    usernameInput.addEventListener("input", syncUser)
    usernameInput.addEventListener("change", syncUser)
    userField.appendChild(userLabel)
    userField.appendChild(usernameInput)

    val passwordField = createPasswordField(
        id = "oss-login-password",
        label = Strings.PASSWORD,
        name = "password",
        autocomplete = "current-password",
        onChange = onPasswordChange,
    )

    val stayRow = document.createElement("label") as HTMLLabelElement
    stayRow.className = "oss-checkbox-row"
    val stayCheckbox = document.createElement("input") as HTMLInputElement
    stayCheckbox.type = "checkbox"
    stayCheckbox.checked = true
    stayCheckbox.addEventListener("change", { onStaySignedInChange(stayCheckbox.checked) })
    val stayText = document.createElement("span") as HTMLElement
    stayText.textContent = Strings.STAY_SIGNED_IN
    stayRow.appendChild(stayCheckbox)
    stayRow.appendChild(stayText)

    val errorText = document.createElement("p") as HTMLParagraphElement
    errorText.className = "oss-error"
    errorText.style.display = "none"

    val submitButton = document.createElement("button") as HTMLButtonElement
    submitButton.type = "submit"
    submitButton.className = "oss-btn-primary"
    submitButton.textContent = Strings.SIGN_IN

    val downloadButton = document.createElement("button") as HTMLButtonElement
    downloadButton.type = "button"
    downloadButton.className = "oss-btn-text"
    downloadButton.textContent = Strings.DOWNLOAD_ANDROID_APP
    downloadButton.style.display = "none"

    val versionText = document.createElement("p") as HTMLParagraphElement
    versionText.className = "oss-version"
    versionText.textContent = Strings.version(APP_VERSION)

    val visibilityListener: (Event) -> Unit = {
        if (isDocumentHidden()) passwordField.setShown(false)
    }
    document.addEventListener("visibilitychange", visibilityListener)

    val handle = WebLoginFormHandle(
        overlay, form, logo, notice, usernameInput, passwordField, stayCheckbox, errorText, submitButton, downloadButton, visibilityListener,
    )
    downloadButton.addEventListener("click", { handle.onDownload?.invoke() })

    form.addEventListener("submit", { event ->
        event.preventDefault()
        // Sync any browser-autofilled values that may not have dispatched an input event yet.
        onUsernameChange(usernameInput.value)
        onPasswordChange(passwordField.input.value)
        onSubmit()
    })

    form.appendChild(logo)
    form.appendChild(notice)
    form.appendChild(userField)
    form.appendChild(passwordField.container)
    form.appendChild(stayRow)
    form.appendChild(errorText)
    form.appendChild(submitButton)
    form.appendChild(downloadButton)
    form.appendChild(versionText)
    overlay.appendChild(form)
    container.appendChild(overlay)
    return handle
}

internal class WebChangePasswordFormHandle(
    val overlay: HTMLDivElement,
    val form: HTMLFormElement,
    val hiddenUsernameInput: HTMLInputElement,
    val currentField: WebPasswordFieldHandle,
    val newField: WebPasswordFieldHandle,
    val repeatField: WebPasswordFieldHandle,
    val generalError: HTMLParagraphElement,
    val submitButton: HTMLButtonElement,
    val secondaryButton: HTMLButtonElement,
    val doneMessage: HTMLParagraphElement,
    val doneCloseButton: HTMLButtonElement,
    val fieldsWrapper: HTMLDivElement,
    val doneWrapper: HTMLDivElement,
    private val visibilityListener: (Event) -> Unit,
    private val keyListener: ((Event) -> Unit)?,
) {
    fun sync(username: String, state: ChangePasswordUiState, isSheet: Boolean) {
        if (hiddenUsernameInput.value != username) hiddenUsernameInput.value = username
        currentField.syncValue(state.current)
        currentField.syncError(state.errors.current)
        newField.syncValue(state.new)
        newField.syncError(state.errors.new)
        repeatField.syncValue(state.repeat)
        repeatField.syncError(state.errors.repeat)

        if (state.errors.general == null) {
            generalError.style.display = "none"
            generalError.textContent = ""
        } else {
            generalError.style.display = "block"
            generalError.textContent = state.errors.general
        }

        submitButton.disabled = !state.canSubmit
        submitButton.textContent = when {
            state.inProgress -> Strings.CHANGING_PASSWORD
            isSheet -> Strings.SAVE
            else -> Strings.CHANGE_PASSWORD
        }

        if (isSheet && state.done) {
            fieldsWrapper.style.display = "none"
            doneWrapper.style.display = "flex"
        } else {
            fieldsWrapper.style.display = "flex"
            doneWrapper.style.display = "none"
        }
    }

    fun hidePasswords() {
        currentField.setShown(false)
        newField.setShown(false)
        repeatField.setShown(false)
    }

    fun dispose() {
        document.removeEventListener("visibilitychange", visibilityListener)
        keyListener?.let { document.removeEventListener("keydown", it) }
        overlay.remove()
    }
}

internal fun mountWebChangePasswordForm(
    isSheet: Boolean,
    username: String,
    onCurrentChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onRepeatChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSecondary: () -> Unit,
    container: HTMLElement = document.body ?: error("Missing document.body"),
): WebChangePasswordFormHandle {
    val overlay = document.createElement("div") as HTMLDivElement
    overlay.className = if (isSheet) "oss-auth-overlay oss-auth-sheet-backdrop" else "oss-auth-overlay"

    val form = document.createElement("form") as HTMLFormElement
    form.className = if (isSheet) "oss-auth-form oss-auth-sheet-card" else "oss-auth-form"
    form.method = "post"
    form.noValidate = true

    val title = document.createElement("h1") as HTMLHeadingElement
    title.className = "oss-auth-title"
    title.textContent = if (isSheet) Strings.CHANGE_PASSWORD else Strings.CHANGE_PASSWORD_TITLE

    val hiddenUsername = document.createElement("input") as HTMLInputElement
    hiddenUsername.type = "text"
    hiddenUsername.name = "username"
    hiddenUsername.value = username
    hiddenUsername.readOnly = true
    hiddenUsername.tabIndex = -1
    hiddenUsername.className = "oss-sr-only"
    hiddenUsername.setAttribute("autocomplete", "username")
    hiddenUsername.setAttribute("aria-hidden", "true")

    val fieldsWrapper = document.createElement("div") as HTMLDivElement
    fieldsWrapper.style.display = "flex"
    fieldsWrapper.style.flexDirection = "column"
    fieldsWrapper.style.setProperty("gap", "12px")

    val intro = document.createElement("p") as HTMLParagraphElement
    intro.className = "oss-auth-intro"
    intro.textContent = if (isSheet) Strings.CHANGE_PASSWORD_SHEET_INTRO else Strings.CHANGE_PASSWORD_INTRO

    val idPrefix = if (isSheet) "oss-sheet" else "oss-change"
    val currentField = createPasswordField("$idPrefix-current", Strings.CURRENT_PASSWORD, "current-password", "current-password", onCurrentChange)
    val newField = createPasswordField("$idPrefix-new", Strings.NEW_PASSWORD, "new-password", "new-password", onNewChange)
    val repeatField = createPasswordField("$idPrefix-repeat", Strings.REPEAT_PASSWORD, "repeat-password", "new-password", onRepeatChange)

    val generalError = document.createElement("p") as HTMLParagraphElement
    generalError.className = "oss-error"
    generalError.style.display = "none"

    val submitButton = document.createElement("button") as HTMLButtonElement
    submitButton.type = "submit"
    submitButton.className = "oss-btn-primary"
    submitButton.textContent = if (isSheet) Strings.SAVE else Strings.CHANGE_PASSWORD

    val secondaryButton = document.createElement("button") as HTMLButtonElement
    secondaryButton.type = "button"
    secondaryButton.className = if (isSheet) "oss-btn-text" else "oss-btn-outlined"
    secondaryButton.textContent = if (isSheet) Strings.CANCEL else Strings.SIGN_OUT
    secondaryButton.addEventListener("click", { onSecondary() })

    val doneWrapper = document.createElement("div") as HTMLDivElement
    doneWrapper.style.display = "none"
    doneWrapper.style.flexDirection = "column"
    doneWrapper.style.setProperty("gap", "12px")
    val doneMessage = document.createElement("p") as HTMLParagraphElement
    doneMessage.className = "oss-auth-intro"
    doneMessage.textContent = Strings.PASSWORD_CHANGED
    val doneActions = document.createElement("div") as HTMLDivElement
    doneActions.className = "oss-sheet-actions"
    val doneCloseButton = document.createElement("button") as HTMLButtonElement
    doneCloseButton.type = "button"
    doneCloseButton.className = "oss-btn-primary"
    doneCloseButton.textContent = Strings.CLOSE
    doneCloseButton.addEventListener("click", { onSecondary() })
    doneActions.appendChild(doneCloseButton)
    doneWrapper.appendChild(doneMessage)
    doneWrapper.appendChild(doneActions)

    fieldsWrapper.appendChild(intro)
    fieldsWrapper.appendChild(currentField.container)
    fieldsWrapper.appendChild(newField.container)
    fieldsWrapper.appendChild(repeatField.container)
    fieldsWrapper.appendChild(generalError)
    if (isSheet) {
        val actions = document.createElement("div") as HTMLDivElement
        actions.className = "oss-sheet-actions"
        actions.appendChild(secondaryButton)
        actions.appendChild(submitButton)
        fieldsWrapper.appendChild(actions)
    } else {
        fieldsWrapper.appendChild(submitButton)
        fieldsWrapper.appendChild(secondaryButton)
    }

    form.appendChild(title)
    form.appendChild(hiddenUsername)
    form.appendChild(fieldsWrapper)
    form.appendChild(doneWrapper)

    form.addEventListener("submit", { event ->
        event.preventDefault()
        onCurrentChange(currentField.input.value)
        onNewChange(newField.input.value)
        onRepeatChange(repeatField.input.value)
        onSubmit()
    })

    if (isSheet) {
        overlay.addEventListener("click", { event ->
            if (event.target == overlay) onSecondary()
        })
    }

    val visibilityListener: (Event) -> Unit = {
        if (isDocumentHidden()) {
            currentField.setShown(false)
            newField.setShown(false)
            repeatField.setShown(false)
        }
    }
    document.addEventListener("visibilitychange", visibilityListener)

    val keyListener: ((Event) -> Unit)? = if (isSheet) {
        val listener: (Event) -> Unit = { event ->
            if ((event as? KeyboardEvent)?.key == "Escape") onSecondary()
        }
        document.addEventListener("keydown", listener)
        listener
    } else {
        null
    }

    overlay.appendChild(form)
    container.appendChild(overlay)
    return WebChangePasswordFormHandle(
        overlay, form, hiddenUsername, currentField, newField, repeatField,
        generalError, submitButton, secondaryButton, doneMessage, doneCloseButton,
        fieldsWrapper, doneWrapper, visibilityListener, keyListener,
    )
}

@Composable
internal actual fun PlatformLoginForm(viewModel: LoginViewModel, sessionEnded: Boolean) {
    // Read `.value` directly in composition so Compose subscribes to state updates (a `by` delegate
    // read only inside `SideEffect` would not trigger recomposition).
    val state = viewModel.state.collectAsState().value
    val colors = MaterialTheme.colorScheme
    val download = androidDownloadAction()
    val handle = remember(viewModel) {
        mountWebLoginForm(
            onUsernameChange = viewModel::onUsernameChange,
            onPasswordChange = viewModel::onPasswordChange,
            onStaySignedInChange = viewModel::onStaySignedInChange,
            onSubmit = { viewModel.submit() },
        )
    }
    SideEffect {
        applyTheme(handle.overlay, colors)
        handle.sync(state, sessionEnded = sessionEnded, darkLogo = logoIsDark(colors.background), downloadAction = download)
    }
    DisposableEffect(handle) {
        onDispose { handle.dispose() }
    }
}

@Composable
internal actual fun PlatformChangePasswordForm(username: String, viewModel: ChangePasswordViewModel, onSignOut: () -> Unit) {
    val state = viewModel.state.collectAsState().value
    val colors = MaterialTheme.colorScheme
    val handle = remember(viewModel) {
        mountWebChangePasswordForm(
            isSheet = false,
            username = username,
            onCurrentChange = viewModel::onCurrentChange,
            onNewChange = viewModel::onNewChange,
            onRepeatChange = viewModel::onRepeatChange,
            onSubmit = { viewModel.submit() },
            onSecondary = onSignOut,
        )
    }
    SideEffect {
        applyTheme(handle.overlay, colors)
        handle.sync(username = username, state = state, isSheet = false)
    }
    DisposableEffect(handle) {
        onDispose { handle.dispose() }
    }
}

@Composable
internal actual fun PlatformChangePasswordSheet(username: String, viewModel: ChangePasswordViewModel, onDismiss: () -> Unit) {
    val state = viewModel.state.collectAsState().value
    val colors = MaterialTheme.colorScheme
    val handle = remember(viewModel) {
        mountWebChangePasswordForm(
            isSheet = true,
            username = username,
            onCurrentChange = viewModel::onCurrentChange,
            onNewChange = viewModel::onNewChange,
            onRepeatChange = viewModel::onRepeatChange,
            onSubmit = { viewModel.submit() },
            onSecondary = onDismiss,
        )
    }
    SideEffect {
        applyTheme(handle.overlay, colors)
        handle.sync(username = username, state = state, isSheet = true)
    }
    DisposableEffect(handle, viewModel) {
        onDispose {
            handle.dispose()
            viewModel.reset()
        }
    }
}
