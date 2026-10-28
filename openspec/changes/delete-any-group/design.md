# Design

## Context

Everything that hangs off a group is removed by the database when the group row goes: `group_participants`, `wishlist_items`, `assignments` and, through them, `questions` (an answer is columns of its question row) all have `ON DELETE CASCADE`, and the connection runs with `PRAGMA foreign_keys = ON`. The only restriction is the rule: `DeleteDraftGroup` refuses a non-Draft with `invalid_state` and `SqliteGroupRepository.deleteDraft` deletes `WHERE state = 'draft'`. `ErrorResponse` carries `code`, `message` and an optional `field`; domain failures are `DomainException` subclasses mapped to a status in `ErrorHandling.kt`. The app reads error bodies into `ApiError.Rejected(status, code, message)`. The owner's page has `AdminStep.Delete` (`canDelete` for a Draft) with one confirmation dialog.

## Goals / Non-Goals

**Goals:**
- Delete a group in any state, removing all of its data.
- A server-enforced second confirmation whenever there is linked data, showing what will go.

**Non-Goals:**
- Soft delete, undo, an audit trail, a schema change, deleting single wishlist items or questions, or telling participants why the group went.

## Decisions

1. **What counts as linked data.** A draw (assignments exist), a wishlist item, a question or an answer. So every Active and Archived group needs the second confirmation, a Pending group only when it has wishlist items, and a Draft or an empty Pending group never does.
2. **The contract.** `DELETE /admin/groups/{id}` and `DELETE /admin/groups/{id}?confirm=true`. Without `confirm=true`, a group with linked data answers `409` `confirmation_required` and a body `{"code","message","impact":{"wishlistItems":12,"questions":7,"answers":5,"draw":true}}`. `ErrorResponse` gains `val impact: DeletionImpactDto? = null` (a `@Serializable` data class in `core/contract`), so existing clients are unaffected. With `confirm=true` the group is deleted whatever it holds; counts are not re-checked, because the person has seen them and the data may have changed in between, which the final answer already covers.
3. **Domain.** `DeleteDraftGroup` becomes `DeleteGroup(loadOwned, groups)`: `invoke(actor, id, confirmed: Boolean)` loads the owned group (404 for a stranger or a missing group), asks `groups.impact(id)` for a `DeletionImpact(wishlistItems, questions, answers, draw)`, throws a new `DomainException.ConfirmationRequired(impact)` (code `confirmation_required`, status 409) when `impact.isEmpty().not()` and not `confirmed`, otherwise `groups.delete(id)`. The impact and the delete run in the same transaction in the repository (`deleteWithImpact` is not needed: the use case reads the impact first and a change between the two only matters to a person who has just confirmed it).
4. **Repository.** `deleteDraft` becomes `delete(id)` (`DELETE FROM groups WHERE id = ?`, `NotFound` when nothing was deleted) and a new `impact(id)` runs four `COUNT(*)` queries (`wishlist_items`, `questions`, `questions WHERE answer IS NOT NULL`, `assignments`) for the group. No migration.
5. **Client API.** `ApiClient.deleteGroup(id, confirm = false)` adds `?confirm=true`. A 409 with code `confirmation_required` is read into a new `ApiError.ConfirmationRequired(impact)`, so the view model can tell it from other conflicts.
6. **Client flow.** `AdminGroupViewModel.canDelete` is true for every phase. `confirmStep()` for `Delete` calls `deleteGroup(id)`; on `ConfirmationRequired` it sets `ui.confirmingDelete = impact` instead of failing, and `confirmDelete()` calls `deleteGroup(id, confirm = true)`; `cancelDelete()` clears it. Success sets `deleted = true`, as now. The second dialog (`DeleteImpactDialog`) says "Delete this group and everything in it?", lists "12 wishlist items, 7 questions, 5 answers and the draw" (leaving out zero counts, singular for one), "This cannot be undone." and has "Delete everything" (error colour) and Cancel.
7. **Row.** "Delete" moves out of the Draft-only condition in `Header`; it stays the last button of the row.
8. **Participants.** Open pages get 404 on their next request, which they already show as "no longer available"; nothing is added for them.

## Risks / Trade-offs

- A deleted Archived group takes its history with it, with no undo; the second dialog says so and Archived always gets it.
- Counts can be stale by the time the person confirms; with `confirm=true` the server deletes what is there then. Accepted.
- The 409 body gains an optional field: older apps read it as an ordinary 409 and show their generic failure, which is safe because nothing was deleted.
