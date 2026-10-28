## MODIFIED Requirements

### Requirement: Editing an item
While the group is Pending or Active, each item SHALL end with an edit button, in one column with its move up and move down buttons, and choosing it SHALL open the same bottom sheet filled with the item's title, note and link. Choosing the rest of the item SHALL NOT open the sheet. A successful save through `PUT /groups/{id}/wishlist/items/{itemId}` SHALL close the sheet and show the changed item in the same place in the list.

#### Scenario: Edit an item
- **WHEN** the person chooses an item's edit button, changes its note and saves
- **THEN** the sheet closes and the item shows the new note in the same position

#### Scenario: Clear the note and link
- **WHEN** the person empties the note and the link and saves
- **THEN** the item no longer shows a note or a link

#### Scenario: The card is not a button
- **WHEN** the person chooses the body of an item rather than its edit button
- **THEN** no sheet opens

#### Scenario: Closed list
- **WHEN** the group is Archived
- **THEN** items have no edit button
