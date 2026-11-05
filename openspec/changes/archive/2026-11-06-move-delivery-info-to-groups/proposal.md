# Proposal

## Why

A person's address, phone and name are kept once, on their account, and every group's Santa reads them from there. But what a person wants a gift sent to, and which email they want the draw sent to, can differ from group to group (home for family, the office for colleagues). Keeping the details on the group lets each group have its own, and lets the Profile tab go: the details are edited where they are used.

## What Changes

- **BREAKING:** the delivery details move from the account to each group. A participant has, per group, a full name, email, phone, address and delivery information, all optional. Accounts keep only the username, password and admin flag.
- **BREAKING:** `PUT /me` is removed and `GET /me` no longer returns the five fields. New routes `GET` and `PUT /groups/{id}/delivery-info` read and replace the person's own details for a group while it is Pending or Active (Archived is read-only; a Draft is not visible to participants).
- A one-time migration copies each saved account profile into the delivery info of every group the person is in, then removes the fields from accounts.
- The receiver's details (`GET /groups/{id}/receiver`) and the Santa reveal (`GET /groups/{id}/santa`) read the delivery info of that group. The draw and announcement emails go to the email set in the group's delivery info, and name the receiver by the full name in it. The administrator's completeness flag and participant full name come from it too.
- The Profile tab, its route and its web address `#/profile` are removed. The participant's bottom bar is Groups and Settings.
- "Edit Delivery Info" in the person's own sheet now closes the sheet and opens a new Delivery Info page for that group (`#/groups/{id}/delivery-info`), a form like the old Profile tab, with the same checks and explanations. It is not offered in an Archived group.
- The person's own "Delivery Info" sheet shows the delivery info for the group.
- The administrator's view loses what lived on the account: the Users tab and the user detail no longer show a full name or other personal fields, and the Users tab search matches the username only. Group views still show the full name from the group's delivery info, and the `*` mark and its legends now say "delivery info incomplete" (in a Draft only "no account" is marked, because nobody can fill delivery info before Pending).

## Capabilities

### New Capabilities

- `delivery-info`: the per-group delivery info on the server: what it holds, who reads and changes it, and the migration of saved profiles.
- `client-delivery-info`: the Delivery Info page of a group.

### Modified Capabilities

- `user-accounts`: an account no longer holds a profile; `GET /me` and the username rule change.
- `gift-visibility`: the receiver's details and the Santa's full name come from the group's delivery info.
- `user-administration`: a managed user is returned without profile fields.
- `groups`: the owner's participant flags and full name come from the group's delivery info.
- `group-lifecycle`: incomplete delivery info does not block activation.
- `draw-emails` and `group-announcements`: emails go to the email in the group's delivery info.
- `client-group-pages`: the own sheet and "Edit Delivery Info" use the group's delivery info and open its page.
- `client-admin-groups`, `client-admin-oversight`, `client-admin-users`: marks, legends and the Users tab and user detail without personal fields.
- `client-settings`, `client-app`, `client-layout`, `client-app-links`: the Profile tab and `#/profile` are gone from the bar, the navigation rules and the addresses.
- `client-profile`: retired. Its requirements are removed and their content lives in `client-delivery-info`.

## Impact

- Server: a new migration `V010` (new table, copy, drop columns), a delivery-info repository and use cases replacing `UpdateProfile`, routes in `GroupRoutes`/`AccountRoutes`, and changes to `GiftVisibility`, `GroupViewer`, `DrawEmails` and `SendAnnouncement`.
- Contract: `AccountDto`, `ProfileUpdateRequest`, `AdminUserDto`; new delivery-info DTOs and route.
- Client: the Profile screen and view model are replaced by a Delivery Info page; `PairSheets`, `PairViewModel`, `ShellScreen`, `Routes`, `WebAddresses`, `TabScreens`, the admin Users screens, `Strings`, the session `Account`.
- **Deployment:** the server and the apps must be updated together. An app from before this change calls `PUT /me`, which no longer exists, and reads profile fields that `GET /me` no longer sends.
- README: the profile, group page and admin sections.
- A person who had saved a profile but is in no group loses it; there is no group to hold it.
