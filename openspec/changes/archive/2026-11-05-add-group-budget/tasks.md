# Tasks

## 1. Server and domain

- [x] 1.1 Add the migration with nullable `budget` and `currency` columns on `groups`, and `budget: Int?` and `currency: String?` to `GroupDetails` with the 50..10000 and 20-character checks and the `RON` default; verify `GroupModelTest` covers a valid budget, 49, 10001, a blank currency, a currency without a budget and 21 characters, and `GroupMigrationTest` still passes
- [x] 1.2 Carry both fields through the contract DTOs, `SqliteGroupRepository` and `GroupRoutes`, for create, edit, the owner's views and the member's views; verify `GroupDtoSerializationTest`, `SqliteGroupRepositoryTest` and `GroupRoutesTest` cover a round trip, an out-of-range budget answered 400 naming `budget`, a group without a budget, and the member view
- [x] 1.3 Add "Budget: 100 RON" after the location line in the draw and announcement emails; verify `DrawEmailsTest` and `SendAnnouncementTest` cover a group with a budget and one without

## 2. Client

- [x] 2.1 Add the budget and currency to the admin and group-detail models, the group form (a digits-only "Budget (optional)" field and a "Currency" field starting with `RON`, the same checks as the server, clearing the budget clears the currency) and "Duplicate"; verify `AdminGroupFormViewModelTest` and `AdminGroupsViewModelTest` cover create, edit, the checks, clearing and duplicating
- [x] 2.2 Show "Budget? - 100 RON" under the location in `GroupInfoCard` on the group page and the admin group screen, with the label semi-bold and no line without a budget; verify `GroupDetailViewModelTest` covers the text and run the app to see the card on both screens
- [x] 2.3 Confirm the group search ignores the budget; verify `GroupFilterTest` has a case where only the budget matches and the group is not listed

## 3. Docs and checks

- [x] 3.1 Mention the budget where the README describes a group's details; verify the text reads right next to the location
- [x] 3.2 Run `./kotlin test` and `openspec validate add-group-budget --strict`; verify both pass

## Workflow follow-up

- Archive the change after trying the form, the card and a draw email.
