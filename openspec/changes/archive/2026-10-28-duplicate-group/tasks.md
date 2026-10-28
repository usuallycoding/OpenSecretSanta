# Tasks

## 1. The copy

- [x] 1.1 Add `copyName` (with the 100-character rule) and the duplicate state and actions to `AdminGroupsViewModel`, sending `POST /admin/groups` with the original's details and participants; cover the name boundary, the request that is sent, a failure and a cancel in unit tests

## 2. The list

- [x] 2.1 Add the "Duplicate" icon button to every card (drawn icon, spoken name), the confirmation dialog with its busy and failure states and the strings, and open the new Draft from `ShellScreen`; verify in headless Chrome with an Active group, a Draft, the 100-character group and one with a participant who has no account: the card still opens the group, cancel creates nothing, confirming creates the Draft and opens it, the original is unchanged

## 3. Check and document

- [x] 3.1 Update the README (the duplicate button on the owner's list), run `./kotlin build` and `./kotlin test` for the whole repo and note what could not be tried (phones)

## Workflow follow-up

- Archive the change after review and sync the delta spec into `openspec/specs`.
