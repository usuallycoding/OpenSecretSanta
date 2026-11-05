## MODIFIED Requirements

### Requirement: Editing an item
While the group is Pending or Active, each item SHALL end with an edit button, and choosing it SHALL open the same bottom sheet filled with the item's title, note and link. Choosing the rest of the item SHALL NOT open the sheet. A successful save through `PUT /groups/{id}/wishlist/items/{itemId}` SHALL close the sheet and show the changed item in the same place in the list.

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

## REMOVED Requirements

### Requirement: Reordering items
**Reason**: Items are now reordered by dragging a handle; the visible move up and move down buttons are gone.
**Migration**: "Reordering items by dragging" replaces it, and "Reordering with a screen reader" keeps move up and move down as accessibility actions.

## ADDED Requirements

### Requirement: Reordering items by dragging
While the group is Pending or Active, each item SHALL have a drag handle, and dragging an item by it SHALL move the item through the list, with the other items making room, using touch or a mouse. Dropping it SHALL send the complete list of item ids in the new order once, with `PUT /groups/{id}/wishlist/order`, and show the order the server returns. No move up or move down button SHALL be shown, and the handles are unavailable while a move is being saved.

#### Scenario: Drag an item to a new place
- **WHEN** the person drags the last item by its handle to the top and drops it
- **THEN** one reorder request is sent and the item is the first one, in the order the server returns

#### Scenario: Dropped where it started
- **WHEN** the person drops a dragged item back at its original place
- **THEN** nothing is sent and the order is unchanged

#### Scenario: Only the handle drags
- **WHEN** the person swipes on the body of an item rather than its handle
- **THEN** the page scrolls and the item does not move

#### Scenario: Dragging near the edge
- **WHEN** the person holds a dragged item near the top or bottom of the screen on a list longer than the screen
- **THEN** the list scrolls in that direction so the item can reach the far end

#### Scenario: Move failed
- **WHEN** the server cannot be reached or refuses the save after a drop
- **THEN** the list returns to the order it had before the drag and the failure is shown

#### Scenario: List changed elsewhere
- **WHEN** the server refuses a move with a 400 naming `itemIds` because another device changed the list
- **THEN** the app reloads the list and says it was changed elsewhere

#### Scenario: Saving a move
- **WHEN** a move is being saved
- **THEN** no item can be dragged until the server has answered

#### Scenario: Closed list
- **WHEN** the group is Archived
- **THEN** items have no drag handle

### Requirement: Reordering with a screen reader
While the group is Pending or Active, each item SHALL offer "Move up" and "Move down" as accessibility actions, so the order can be changed without dragging. An action SHALL save the new order the same way a drop does. The first item cannot move up and the last cannot move down.

#### Scenario: Move up with a screen reader
- **WHEN** a screen-reader user runs "Move up" on the second item
- **THEN** it becomes the first and the new order is what the server returns

#### Scenario: Ends of the list
- **WHEN** an item is first or last
- **THEN** it offers no "Move up" or no "Move down" respectively

#### Scenario: No visible buttons
- **WHEN** the list is shown without a screen reader
- **THEN** no move up or move down button is visible on any item

### Requirement: Wishlist items are numbered in a circle
Wherever a wishlist item is shown (the person's own list, the receiver's list on the Gifter page, and the owner's wishlist sheets), its place in the list SHALL be shown, counting from 1, in a small filled circle at the start of the item, in the style of a question's circle, and the item's text SHALL follow the circle without repeating the number. A screen reader SHALL announce the number with the item as "Item N".

#### Scenario: Numbered item
- **WHEN** the third item of a list is shown
- **THEN** a circle holding 3 is at its start and its text follows without "3."

#### Scenario: Other places the list is shown
- **WHEN** a Santa reads their receiver's list, or the owner opens a participant's wishlist sheet
- **THEN** the items are numbered the same way, in the order shown

#### Scenario: Numbers follow position
- **WHEN** an item is moved from third to first
- **THEN** it shows 1 and the two items it passed show 2 and 3

#### Scenario: Dragging
- **WHEN** the person drags an item over the one above it
- **THEN** the two exchange numbers as soon as they exchange places, and the numbers are the saved order's once the server answers

#### Scenario: Screen reader
- **WHEN** a screen reader reads the third item
- **THEN** it announces "Item 3" with the item's text
