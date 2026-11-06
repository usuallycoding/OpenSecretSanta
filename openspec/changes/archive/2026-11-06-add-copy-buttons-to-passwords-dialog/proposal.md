# Proposal

## Why

The temporary passwords are handed over by hand, one person at a time, and today the only way to copy one is to long-press and select text, which is fiddly on a phone and on the web. A copy icon per row and a "Copy all" button make handing them out quick, using the copy icon the app already has everywhere else.

## What Changes

- Every row of the temporary passwords dialog gets the app's copy icon (the one with the check mark and the failure state), which copies that row as `username: password`, the way it reads.
- The dialog gets a "Copy all" button at the bottom, next to "OK", which copies every row as `username: password`, one per line.
- "Copy all" is not shown when the dialog lists fewer than two people (nobody, or the one row of "Reset password"), where it would only repeat the row's icon.
- The rows stay selectable text.
- The dialog is one component, so "Get passwords", the dialog after making a group Pending and the one after "Reset password" all get this; the passwords are still not kept anywhere once it closes.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-admin-groups`: a new requirement for copying from the temporary passwords dialog (the existing requirements keep their wording, which stays true)

## Impact

- `app/shared/src/ui/admin/PasswordsDialog.kt` (rows, "Copy all" in the dismiss slot), a pure function for the "all" text with its unit test, and `Strings` for "Copy all".
- No server, contract or domain change.
