# Proposal

## Why

The server lets an administrator plan a whole exchange: create a group, set its details and participants, make it Pending (which creates the missing accounts), draw the names and archive it. The app's admin view is still a placeholder, so none of this can be done without calling the API by hand, and the participant screens built so far have nobody to run an exchange for them. This is the first of three admin changes (2a groups and their lifecycle, 2b users, 2c owner oversight); it is the one that makes an exchange possible from the app alone.

## What Changes

- **Admin groups list.** The admin view opens on the administrator's own groups (name, state, date and time, location, number of participants) with a "New group" action. A group that belongs to another administrator never appears.
- **Create and edit a group.** One form for name, description, location, date and time. Dates are typed as `2026-12-24` and times as `18:30`, with a hint under the field, and everything is checked before sending exactly as the server checks it (name 1 to 100 characters, description up to 2000, location up to 200, a real date, a real time). A new group starts as a Draft and its detail opens. Details can be edited while the group is Draft, Pending or Active, not once it is Archived.
- **Admin group detail.** Details, the state and what it means for the owner, and the participants with a note for anyone who has no account yet and for anyone whose profile is incomplete (no name or address to post a gift to). The actions on the page depend on the state.
- **Participants.** In a Draft or Pending group the owner edits the list in a bottom sheet: add a username (checked like the server: 3 to 32 characters of letters, digits, `.`, `_` or `-`, case ignored, duplicates stored once) or remove one, then save. Later the roster is frozen and shown read-only.
- **Moving a group forward.** "Make pending" on a Draft, "Draw the names" on a Pending group (it needs at least 4 participants and freezes the roster) and "Archive" on an Active one, each after a confirmation that says what it does. A Draft can also be deleted, after a confirmation.
- **Temporary passwords.** When making a group Pending, or saving a Pending group's participants, creates accounts, the app shows each new username with its temporary password once, selectable so it can be copied, and says each person must change it when first signing in. Nothing keeps the passwords after the dialog is closed.
- **Concurrent changes.** If a group changed behind the screen (archived elsewhere, deleted, the roster frozen) the app reloads it and says what happened.

The server needs no change.

Out of scope:
- Users (list, reset a password, promote): the next admin change. Owner oversight (the assignments, a participant's wishlist, the question threads): the one after.
- Setting participants while creating a group (they are added on the detail page), moving a group backwards, copying a group, notifications and bulk import.

## Capabilities

### New Capabilities
- `client-admin-groups`: how an administrator lists, creates, edits and deletes their groups in the app, edits participants, moves a group through its states and sees the temporary passwords of accounts that were created.

### Modified Capabilities
- `client-app`: the admin view now shows the admin groups screen instead of a placeholder.

## Impact

- Code: nine admin group calls in `ApiClient`, three view models and their screens (list, form, detail with the participants sheet, the confirmations and the passwords dialog) in `app/shared`, four typed routes, the admin branch of the shell, strings and the README.
- APIs: none new. The app uses the existing `/admin/groups...` routes with the contract types that already exist.
- Dependencies: none.
- Systems: none.
