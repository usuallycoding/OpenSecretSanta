# Tasks

## 1. Centered username

- [x] 1.1 Add a `centered` option to `ReadOnlyText` that starts the row with a spacer as wide as the copy icon, from one shared size, and use it for the username in `MySheet` and `ReceiverSheet`; verify the name is in the middle of both sheets, with the copy icon beside it, and that a long name is still cut and opens its dialog

## 2. Edit Delivery Info

- [x] 2.1 Add the "Edit Delivery Info" string and button at the end of `MySheet`'s loaded content, after the card or the "no profile" note, none on `ReceiverSheet`; verify it shows for a person with details and for one without, and not while loading or failed
- [x] 2.2 Add an `onEditProfile` callback to `GroupPage` that dismisses the sheet and calls `navController.openTab(ProfileRoute)` from `ShellScreen`; verify choosing the button shows the Profile tab with the tab bar marking it, and that `./kotlin test` passes apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`
- [x] 2.3 Mention the button in the README paragraph on the group page and run `openspec validate edit-profile-from-delivery-info --strict`; verify both

## Workflow follow-up

- Archive the change after trying both sheets.
