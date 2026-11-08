# Spec Delta

## MODIFIED Requirements

### Requirement: Add an item
The server SHALL expose `POST /groups/{id}/wishlist/items` that appends a new item at the end of the caller's list for that group and returns it with status 201. A list SHALL hold at most 200 items.

#### Scenario: Appended last
- **WHEN** a participant with two items adds a third
- **THEN** the new item is last in the list

#### Scenario: List full
- **WHEN** a participant who already has 200 items in the group adds another
- **THEN** the response is 409 with code `invalid_state` and the list is unchanged
