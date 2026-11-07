# Proposal

## Why

The snow falls in front of everything, so a flake can cross a word, a button or a card and take the eye from what the person is reading. Behind the interface the snow is still there, as a backdrop, and the content stays clean.

## What Changes

- The snow falls **behind** the app's content on every screen, the sign-in screens included: it shows in the open space of a screen and passes behind cards and other filled elements.
- The top bar, the bottom tab bar and, on a wide window, the sidebar are opaque and hide the flakes behind them, as cards do.
- Bottom sheets, dialogs and pickers stay over the snow, as before.
- **Every outlined text field on a screen is filled** with the theme's background colour, so a flake never shows inside a field behind what is typed: the sign-in fields, the search fields, the form fields and the password fields on screens get the same fill, from one shared definition. Nothing is coloured inside bottom sheets and dialogs: they are opaque and over the snow, so their fields, and the sheets and dialogs themselves, keep their usual look.
- Nothing else about the snow changes: the switch and its default (on), the colours, sizes, speed, sway, opacity and number of flakes are as they were, and it still takes no tap, scroll or key.
- The snowflake pattern on the cards is a separate feature and is not touched.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-appearance`: "Snow falls unless it is switched off" says the snow falls behind the content, "The snow stays out of the way" says it is behind everything, including the bars, and that sheets and dialogs are over it, and a new requirement, "Text fields are filled", says every outlined field on a screen has a fill so the snow does not show in it, and that sheets and dialogs are left as they are.

## Impact

- `app/shared/src/ui/form/FormComponents.kt` (a shared field colour definition and `SheetTextField`, which `MultiLineTextField` and the search fields use), and the other outlined fields: `PasswordField.kt`, `LoginScreen.kt`, `LetterScreens.kt`, `AdminGroupFormScreen.kt`, `GroupPickerFields.kt` and `DeliveryInfoScreen.kt`.
- `app/shared/src/ui/App.kt` (the snow moves from a layer over the app to the first layer inside it, over the background), `app/shared/src/ui/shell/ShellScreen.kt` (the shell's own background fill and the scaffold's container colour become transparent so that the snow shows through the open space).
- No server, contract, data or setting change.
