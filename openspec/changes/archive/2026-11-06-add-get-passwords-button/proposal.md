# Proposal

## Why

When a group becomes Pending, the app shows the temporary passwords of the accounts it creates once, in a dialog. If the administrator closes it before copying them, or needs them again later for people who have not signed in yet, the only way back is opening each person under Users. A button on the group page that shows them again, for the whole group, saves that trip.

## What Changes

- The administrator's page for a Pending or Active group gets a "Get passwords" button after "Send announcement".
- Choosing it opens the same kind of dialog as the one shown when accounts are created, listing the group's participants who still have a temporary password as `username: password`. People who have set their own password are left out.
- When nobody in the group has a temporary password, the dialog says so.
- No server change: `GET /admin/users` already returns each managed user's temporary password while it is temporary, and the page filters it to this group's participants.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-admin-groups`: two added requirements, for the button and for its dialog. "Temporary passwords are shown once" is unchanged.

## Impact

- Client only: `AdminGroupViewModel`, `AdminGroupScreen`, `AdminModels` and `Strings`. No server, contract or database change.
- The passwords are held only while the dialog is open, as with the dialog shown on creating accounts.
- Out of scope: Draft and Archived groups, and any change to how or when passwords are created.
