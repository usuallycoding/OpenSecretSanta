# Proposal

## Why

People drawing names need to know how much to spend, and today the group has nowhere to say it, so the owner has to put it in the description or announce it separately. A budget on the group puts it next to where and when, and in the emails people read when they draw.

## What Changes

- A group gets an optional **budget**: a whole number from 50 to 10000, and a **currency**: a short text of at most 20 characters. The owner edits both with the other details, in the group form.
- A budget with no currency is stored with the currency `RON`; a group with no budget has no currency either.
- The group information card, on the participant's group page and on the admin group screen, shows a line "Budget? - 100 RON" under the "Where? - " line.
- The draw email and the announcement email give "Budget: 100 RON" after the location, when the group has a budget.
- "Duplicate" copies the budget and the currency.
- The group form gets a "Budget (optional)" field and a "Currency" field prefilled with `RON`, checked the way the server checks them.
- The group search does not look at the budget, as it does not look at the location. The group list rows do not show it.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `groups`: the group record has a budget and a currency with their limits and the `RON` default; editing and the participant view carry them.
- `client-groups`: the group information card shows the budget line, with its label.
- `client-admin-groups`: the group form has the budget and currency fields and checks them; the admin card shows the line; "Duplicate" copies both.
- `draw-emails`: the draw email gives the budget when it is set.
- `group-announcements`: the announcement email gives the budget when it is set.
- `client-group-search`: the budget and currency are not searched.

## Impact

- Server: a new migration adding two nullable columns to `groups`; `SqliteGroupRepository`, `GroupRoutes` and the DTOs in `core/contract/src/GroupDtos.kt` carry the two fields. Existing groups have no budget.
- Domain: `GroupDetails` (validation and the `RON` default) and `DrawEmails.kt`.
- Client: `AdminModels.kt`, `AdminGroupFormViewModel.kt`, `AdminGroupFormScreen.kt`, `GroupDetailViewModel.kt`, `GroupInfoCard.kt`, the duplicate flow in `AdminGroupsViewModel.kt` and `Strings.kt`.
- The API stays compatible: both fields are optional in requests and absent when empty in answers, so an older client keeps working.
