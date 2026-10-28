## MODIFIED Requirements

### Requirement: Oversight is read-only
The oversight endpoints SHALL only read. No admin endpoint SHALL change or answer a wishlist item, a question or an answer, and the only way an administrator removes them is by deleting the whole group, which removes everything of it at once.

#### Scenario: No write endpoint
- **WHEN** an owner looks for a way to change a participant's wishlist, question or answer through admin endpoints
- **THEN** none exists

#### Scenario: Only with the group
- **WHEN** an owner looks for a way to delete one wishlist item, question or answer of a participant
- **THEN** none exists; deleting the group is the only way they go
