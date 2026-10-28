# Tasks

## 1. Server

- [x] 1.1 Add `DeletionImpact`, `DomainException.ConfirmationRequired` (409, `confirmation_required`), `GroupRepository.impact(id)` and `delete(id)` in place of `deleteDraft`, with the four counts in `SqliteGroupRepository`; cover the counts and the delete of a group in every state (with its wishlist items, questions and answers gone) in the repository tests and the domain fakes
- [x] 1.2 Turn `DeleteDraftGroup` into `DeleteGroup` with the confirmation rule, read `confirm` in the route, return the impact in `ErrorResponse` (`impact`), and cover every scenario of the `groups` spec (Draft, empty Pending, Pending with items, Active, Archived, confirmed, stranger, missing) in the use case and route tests

## 2. App

- [x] 2.1 Add `ApiClient.deleteGroup(id, confirm)` and `ApiError.ConfirmationRequired(impact)`; in `AdminGroupViewModel` make `canDelete` true for every phase and add the second confirmation (`confirmingDelete`, `confirmDelete`, `cancelDelete`); cover each scenario of the client spec in view model tests (one confirmation for a Draft, two for an Active group, cancel at the second, the group gone meanwhile)
- [x] 2.2 Show "Delete" for every group, add the impact dialog and its strings to `AdminGroupScreen`; verify in headless Chrome against a throwaway server: a Draft deletes with one dialog, the Active group shows the counts and deletes only after the second confirmation (check in the database that its wishlist items, questions and answers are gone), cancelling the second dialog deletes nothing

## 3. Check and document

- [x] 3.1 Update the README (the route, the `confirm` parameter and the owner's Delete), run `./kotlin build` and `./kotlin test` for the whole repo and note what could not be tried (phones)

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.
