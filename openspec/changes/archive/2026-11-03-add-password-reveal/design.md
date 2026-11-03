## Context

Two places build password inputs: `LoginScreen` has its own `OutlinedTextField` with a `PasswordVisualTransformation`, and `PasswordField` in `ChangePasswordScreen.kt` serves both the password-change screen and its sheet. The project depends only on the core Material icon set, which has no visibility icons; `SwapIcon.kt` already shows how to draw one from a path.

## Goals / Non-Goals

**Goals:** an eye on every typed password, one input for all four fields, and hiding again on leaving the app.

**Non-Goals:** the temporary passwords shown for copying, remembering a choice across screens, or one switch for several fields.

## Decisions

- **One input, used by four fields.** `PasswordField` gets a trailing `IconButton` and the sign-in field uses it. It takes the label, value, change handler, error, IME action and done action it takes now, plus the sign-in field's own done action. The sign-in field has no error text, so `error` stays optional.
- **State inside the field.** `var shown by rememberSaveable { mutableStateOf(false) }` in `PasswordField`: each field has its own, a rotation keeps it, and leaving the screen drops it, so it starts hidden.
- **Hide on pause.** `LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { shown = false }`, the effect already used in `ReceiverScreen`. ON_PAUSE is chosen over ON_STOP because it is the event every target raises when the app is covered or backgrounded, so the password is hidden before the app shows in the app switcher.
- **The transformation.** `if (shown) VisualTransformation.None else PasswordVisualTransformation()`. The keyboard type stays `Password` in both states so the keyboard does not start suggesting words.
- **Icons.** Two `ImageVector`s drawn from the Material "visibility" and "visibility off" paths, built like `SwapIcon`, in `app/shared/src/ui/form/`. The extended icon set would add several MB to the web build for two icons.
- **Names.** `Strings.SHOW_PASSWORD` and `Strings.HIDE_PASSWORD` are the button's content description; the button shows the icon of what choosing it will do, not of the current state: a hidden password shows the open eye ("Show password"), a shown one the slashed eye ("Hide password").

## Risks / Trade-offs

- ON_PAUSE also fires when a system dialog covers the app, which hides a shown password. That is the safe side and harmless.
- The eye is inside the field's right edge, so a long password's end can sit under it; the field's text already stops before the trailing icon.
