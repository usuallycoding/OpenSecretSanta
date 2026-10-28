# Spec Delta

## MODIFIED Requirements

### Requirement: Participant wishlist
Choosing "Read wishlist" on a participant of a Pending group SHALL open a bottom sheet with that person's wishlist for the group, loaded with `GET /admin/groups/{id}/participants/{username}/wishlist`, read-only and in the order the participant chose, showing for each item its title and, when set, its note and link, and when it was added or last changed. Choosing a link SHALL open the link dialog of `client-wishlist`. There SHALL be no wishlist screen, and the sheet SHALL offer no way to add, change, delete or reorder items.

#### Scenario: List shown in order
- **WHEN** a Pending group's participant has three items in a chosen order
- **THEN** the sheet shows the three in that order

#### Scenario: Empty list
- **WHEN** the participant has no items
- **THEN** the sheet says the list is empty

#### Scenario: Read-only
- **WHEN** the wishlist sheet is shown
- **THEN** there is no control that adds, edits, deletes or moves an item

#### Scenario: Link opens
- **WHEN** the owner chooses an item's link
- **THEN** the link dialog opens, and the link opens in the browser only from its "Open in browser" button
