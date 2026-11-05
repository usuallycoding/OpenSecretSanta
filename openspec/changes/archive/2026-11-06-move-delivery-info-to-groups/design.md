# Design

## Context

The five fields are columns of `users`, held in the domain as `Profile` on `User`, edited through `PUT /me` and `UpdateProfile`, and read from the account by `GiftVisibility` (receiver and Santa), `GroupViewer` (admin flags and full name), `DrawEmails` and `SendAnnouncement` (recipient email, receiver name) and the admin user routes. On the client the Profile tab (`ProfileViewModel`/`ProfileScreen`) edits them, `MyDetailsViewModel` reads them for the own sheet, and `Account` in the session carries the full name. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- One place that holds the five fields, keyed by group and username, and every reader switched to it.
- No data loss for people who already saved a profile and are in a group.
- The new page is the old form moved, not redesigned.

**Non-Goals:**
- No prefill from another group, no sharing between groups.
- No change to who may read what: the receiver's details by their Santa and the Santa's full name after archiving stay as they are.

## Decisions

- **A table keyed by group and username, not by participant row.** `group_delivery_info(group_id, username, full_name, email, phone, address, delivery_info)` with primary key `(group_id, username)` and `ON DELETE CASCADE` to the group. A Draft may name people without accounts (`group_participants` has no foreign key to `users`), and a row here exists only once someone saves, so absent rows read as empty details. Alternative: add the five columns to `group_participants`; rejected because every participant row would carry them and the participant list is replaced wholesale when edited, which would need care not to wipe them.
- **The domain type is the old `Profile`, renamed `DeliveryInfo`.** Validation stays where it is (valid email, 500 characters, blanks to null, completeness is name and address), so the page, the server and their tests keep one definition. `User` loses its `profile`.
- **Access and state rules live in a use case.** `GetDeliveryInfo` and `UpdateDeliveryInfo` check that the actor is a participant of a non-Draft group (404 otherwise) and, for updates, that the group allows detail changes (Pending or Active, else 409 `invalid_state`), reusing `GroupState` as the wishlist does.
- **Readers look the details up by the group.** `GiftVisibility` reads the receiver's and the Santa's rows for that group; `GroupViewer` and the email use cases do the same for every participant at once with one query for the group, as `findAll` does for accounts today.
- **The DTO keeps its names.** `ParticipantDto.profileComplete` and `fullName` stay, now computed from the group's delivery info, so a client that already knows the field keeps working. In a Draft they are false and empty; the client marks only "no account" there.
- **The migration copies, then drops.** `V010` creates the table, inserts a row for every `(group, participant)` whose account has at least one of the five fields set, then drops the five columns from `users` (SQLite 3.35 or later, which the bundled driver is). Dropping in the same migration keeps one source of truth. Alternative: leave the columns unused; rejected because stale data that is never read invites a second reader.
- **`GET /me` and `Account` shrink.** `AccountDto` loses the five fields and the client's `Account` loses `fullName`. `PUT /me` is deleted rather than kept returning errors, so an old app fails loudly.
- **The page is a route, not a tab.** `DeliveryInfoRoute(groupId)` is a plain destination with its own web address, opened from the sheet after it closes, and back returns to the group. `ProfileRoute`, its tab-bar item and `#/profile` are deleted; an old `#/profile` address falls under "an address that names no screen".
- **Where the Profile tab's behaviours go.** The form, validation, explanation text, multi-line fields and loading/error handling are ported to the new page with the group's id; "Archived is read-only" is new, and the "Edit Delivery Info" button is not drawn then.
- **Administrator's account views lose personal fields.** `AdminUserDto` and the detail/Users tab drop the full name and the others; the Users tab search and its tests go back to the username only. The group views keep the full name through the participant DTO.

## Risks / Trade-offs

- [An old app against the new server] → `PUT /me` is gone and `GET /me` has no fields; the proposal says to deploy together, and the Android build is distributed to testers before the server changes.
- [People in no group lose a saved profile] → accepted and stated in the proposal; there is nowhere to keep it.
- [A person's draw email goes to nobody until they set an email in the group] → the summary already counts participants without an address; the Delivery Info page says the email is used for the draw. The migration fills it for everyone who had one.
- [The admin has no overview of who is incomplete across groups] → the group views still mark it per group.

## Migration Plan

Deploy the server (the migration runs at start, copies, and drops the columns) and release the apps in the same window. Roll back by restoring the database from before the start: the columns are dropped, so a code-only rollback is not possible after the migration has run.
