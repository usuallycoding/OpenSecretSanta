## MODIFIED Requirements

### Requirement: Delete a group
The owner SHALL be able to delete a group in any state with `DELETE /admin/groups/{id}`. Deleting SHALL remove the group together with its participants, its draw, every wishlist item, every question and every answer.

#### Scenario: Delete a Draft
- **WHEN** the owner deletes a Draft group
- **THEN** the response is 204 and the group no longer exists

#### Scenario: Pending group without wishlist items
- **WHEN** the owner deletes a Pending group in which nobody has added a wishlist item
- **THEN** the response is 204 and the group no longer exists

#### Scenario: Confirmation required
- **WHEN** the owner deletes an Active group that has a draw, wishlist items, questions and answers, without `confirm=true`
- **THEN** the response is 409 with code `confirmation_required` and the counts of wishlist items, questions and answers and whether there is a draw, and the group remains

#### Scenario: Confirmed
- **WHEN** the owner repeats the request with `confirm=true`
- **THEN** the response is 204 and the group, its draw, wishlist items, questions and answers are all gone

#### Scenario: Pending group with wishlist items
- **WHEN** the owner deletes a Pending group that has wishlist items without `confirm=true`
- **THEN** the response is 409 with code `confirmation_required` and the number of wishlist items

#### Scenario: Archived group
- **WHEN** the owner deletes an Archived group with `confirm=true`
- **THEN** the response is 204 and nothing of it remains

#### Scenario: Someone else's group
- **WHEN** an administrator who does not own the group deletes it, with or without `confirm=true`
- **THEN** the response is 404 and the group remains

#### Scenario: Missing group
- **WHEN** the group does not exist
- **THEN** the response is 404

## ADDED Requirements

### Requirement: Deleting a group with linked data needs confirmation
When the group has linked data (a draw, a wishlist item, a question or an answer), the server SHALL refuse the request without `confirm=true` with HTTP 409, the code `confirmation_required` and the counts of what would be deleted, and SHALL delete only when the request carries `confirm=true`. A group without linked data SHALL be deleted by the plain request.

#### Scenario: Counts in the refusal
- **WHEN** a group with three wishlist items and a draw is deleted without `confirm=true`
- **THEN** the answer is 409 `confirmation_required` with those counts and nothing is deleted
