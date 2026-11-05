# Tasks

## 1. Storage and domain

- [x] 1.1 Add migration `V010` that creates `group_delivery_info`, copies each account's saved profile into a row for every group the person is in, and drops the five columns from `users`; verify a migration test seeds accounts with and without profiles in several groups and finds the right rows and no columns left, and the schema-version assertions read 10
- [x] 1.2 Rename `Profile` to `DeliveryInfo` (same checks), remove it from `User` and `UserRepository`, add a delivery-info repository with a SQLite implementation and the in-memory fake, and replace `UpdateProfile` with `GetDeliveryInfo` and `UpdateDeliveryInfo` (participant of a non-Draft group; update only while Pending or Active); verify domain tests cover read, update, clearing, an invalid email, 501 characters, Draft and non-participant refused, Archived refused, and one group not touching another
- [x] 1.3 Read the receiver's and the Santa's details, the owner's flags and full names, and the draw and announcement recipients and receiver name from the group's delivery info (`GiftVisibility`, `GroupViewer`, `DrawEmails`, `SendAnnouncement`); verify the existing tests are moved to it and cover a participant who set nothing, a different email per group, and the Draft flags being false

## 2. Server routes and contract

- [x] 2.1 Add `GET` and `PUT /groups/{id}/delivery-info` with their DTOs and route constant, remove `PUT /me` and the five fields from `AccountDto` and `AdminUserDto`, and drop the unused `ProfileUpdateRequest`; verify route tests cover own read and update, 404 for a stranger and a Draft, 400 naming `email` and `address`, 409 when Archived, 403 before the password change, `PUT /me` answering 405 and `GET /me` without the fields
- [x] 2.2 Check the receiver, Santa, admin group view, activation and announcement routes end to end with per-group details; verify the existing route tests pass with details set through the new route, `./kotlin test` passes for the server and core modules

## 3. Client page and navigation

- [x] 3.1 Add the delivery-info calls to `ApiClient` and a Delivery Info view model and page ported from the Profile tab (five fields, same checks, explanation, multi-line fields, loading and error, "Saved."), with `DeliveryInfoRoute(groupId)` and the address `#/groups/{id}/delivery-info`; verify view-model tests cover load, save, clear, validation, a 409 closed message and a failed load, and a web address test covers the route both ways
- [x] 3.2 Make the own "Delivery Info" sheet load the group's delivery info, make "Edit Delivery Info" close the sheet and open the page (not offered when Archived), and remove the Profile tab, `ProfileRoute`, `#/profile`, the screen, its view model and tests, and `fullName` from the session `Account`; verify the participant bar shows Groups and Settings, an old `#/profile` address names no screen, and `TabScreensTest`, `WebAddressesTest` and the pair view model tests are updated and pass
- [x] 3.3 Look at the page, the sheet and the bar on a Pending, an Active and an Archived group; verify "Edit Delivery Info" opens the page and a save shows in the sheet, the Archived group has no button, and back returns to the group

## 4. Administrator's views

- [x] 4.1 Drop the full name and the personal fields from the Users list, the user detail and the user sheet, search the Users tab by username only, mark only "no account" in a Draft and "no account or delivery info incomplete" in a Pending group, and change the two legends; verify admin view-model tests cover each and the participant search by full name still works from the group's data

## 5. Docs and checks

- [x] 5.1 Update the README sections on the profile, the group page, the API table, the participant and admin views and the emails; verify no mention of the Profile tab, `PUT /me` or account profile fields remains
- [x] 5.2 Edit the Purpose lines of the specs that name the profile, run `./kotlin test` and `openspec validate --specs --strict`; verify all pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`

## Workflow follow-up

- Archive the change after trying the page and the sheet on a Pending, an Active and an Archived group, and after the server and the apps are released together.
