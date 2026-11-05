# Design

## Context

`GET /admin/users` returns every user the administrator manages with `temporaryPassword` while the password is still temporary, and the Users tab and the user sheets already use it. The group screen's view model (`AdminGroupViewModel`) already holds the group's participants and shows `PasswordsDialog` from `ui.passwords` when accounts are created. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- Reuse what exists: the list route, the dialog and the notice.
- Keep the passwords in memory only while the dialog is open.

**Non-Goals:**
- No new server route and no change to who may read passwords: the list already applies the administrator's scope.
- No password for a Draft (it has no accounts) or an Archived group.

## Decisions

- **Client-side filter of the existing list.** The view model calls `api.adminUsers()`, keeps the users whose username is one of the group's participants and who have a temporary password, in the group's participant order, and puts them in `ui.passwords`. Alternative: a new `GET /admin/groups/{id}/passwords` route; rejected because it would repeat the scope check the list already makes and add a second place that returns passwords.
- **The dialog is the existing `PasswordsDialog`.** It takes a title, a body and the list, so the on-demand use passes a different body ("These people have not changed their password yet...") and, for nobody, a body that says so with an empty list. A small `ui` flag says which body applies, so the same state serves both uses.
- **Same states as the announcement.** `canAnnounce` is Pending or Active; the new button uses the same condition, in the same `FlowRow` right after the announcement button.
- **Busy while loading.** The page's `busy` flag disables the button during the call, as it does for the steps, so a second tap cannot start a second request. A failure maps to the notice the page already has for an unreachable server and opens no dialog.

## Risks / Trade-offs

- [Shoulder-surfing: the passwords appear on demand, not only once] → they are shown only to the administrator who owns the group and can already read them under Users; closing the dialog drops them.
- [A person who changed their password between the list and the dialog] → the list is read at the moment of the tap, so the dialog is as fresh as the request.
