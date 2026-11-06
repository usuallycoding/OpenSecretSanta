# Proposal

## Why

The row of actions under an admin's group details is a row of text buttons that wraps onto a second line on a phone. Four of them are well known actions (edit, announce, passwords, delete), and icons say them in less room, leaving the one thing that moves the group forward as the only word button.

## What Changes

- On the administrator's group page, "Edit details" becomes an outlined pencil icon button, "Send announcement" a bell, "Get passwords" a key (the app's own key icon, the one that marks temporary passwords) and "Delete" a trash can, its outline and icon in the error colour.
- The row shows them in that order, each only in the states where its button shows today and with the same enabled and disabled rules.
- The step button ("Make pending", "Draw the names", "Archive") keeps its look and moves to the end of the row, after the icons; the row still wraps when it does not fit.
- Each icon button keeps its old label as the name a screen reader speaks.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-admin-groups`: "Moving a group forward" says where the step sits in the row, its "Buttons together" scenario shows the new order, and a new requirement says the four actions are icon buttons with their old labels as spoken names (the requirements of the buttons themselves keep their wording, which stays true)

## Impact

- `app/shared/src/ui/admin/AdminGroupScreen.kt` (the header row) and no new strings.
- No server, contract or domain change.
