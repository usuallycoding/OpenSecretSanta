# client-wishlist Specification

## Purpose

How a participant keeps their wishlist for a group in the app: adding, editing, deleting and reordering items, with links, while the group allows it and read-only once it is archived.

## Requirements

### Requirement: Adding an item
While the group is Pending or Active, the person SHALL be able to add an item with the floating "Add item" button, in a bottom sheet over the list, with a title, an optional note and an optional link. A successful save through `POST /groups/{id}/wishlist/items` SHALL close the sheet and show the item at the end of the list. A list holds at most 200 items; at 200 adding SHALL be unavailable with an explanation.

#### Scenario: Add an item
- **WHEN** the person opens the add sheet, enters a title and saves
- **THEN** the sheet closes and the item is the last one in the list

#### Scenario: List is full
- **WHEN** the list already has 200 items
- **THEN** the add action is unavailable and says the list is full

#### Scenario: Dismissing the sheet
- **WHEN** the person closes the sheet without saving
- **THEN** nothing is added and what they typed is discarded

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

### Requirement: Deleting an item
While the group is Pending or Active, the person SHALL be able to delete an item from its edit sheet, after a confirmation that names the item. Confirming SHALL remove it permanently with `DELETE /groups/{id}/wishlist/items/{itemId}` and leave the others in order.

#### Scenario: Confirm deletion
- **WHEN** the person chooses delete and confirms
- **THEN** the item disappears and the other items keep their order

#### Scenario: Cancel deletion
- **WHEN** the person chooses delete and cancels
- **THEN** the item stays

### Requirement: Item form validation
Before sending, the sheet SHALL check what the server checks: a title of 1 to 200 characters, a note of at most 1000, and a link of at most 2000 characters that is an absolute `http` or `https` address. A problem SHALL be shown next to its field and nothing SHALL be sent. A validation error from the server naming a field SHALL be shown next to that field, and a failed save SHALL keep the sheet open with what the person typed.

#### Scenario: Missing title
- **WHEN** the person saves with an empty title
- **THEN** the title field says a title is needed and nothing is sent

#### Scenario: Not a web address
- **WHEN** the person enters a link such as `javascript:alert(1)` or `ftp://host/file`
- **THEN** the link field says it must be an http or https address and nothing is sent

#### Scenario: Server cannot be reached
- **WHEN** a save cannot reach the server
- **THEN** the sheet says so, stays open and keeps the edits so the person can try again

### Requirement: Read-only once archived
While the group is Archived, the wishlist on the Receiver page SHALL be read-only: no add, edit, delete or reorder control is shown and the page says the list can no longer be changed. When the group is archived while the person is editing, a refused change SHALL close the sheet, reload the list as read-only and say so.

#### Scenario: Archived group
- **WHEN** the person opens the Receiver page of an Archived group
- **THEN** the items are shown with no way to change them and a note that the list is closed

#### Scenario: Archived meanwhile
- **WHEN** the server answers a change with 409 because the group was archived after the page opened
- **THEN** the sheet closes, the list reloads read-only and the page says it can no longer be changed

#### Scenario: Item deleted elsewhere
- **WHEN** the server answers an edit or delete with 404 because the item no longer exists
- **THEN** the sheet closes, the list reloads and the page says the item no longer exists

### Requirement: Loading, errors and refresh
While a wishlist loads, the app SHALL show a progress indicator in its place. When it cannot be loaded the app SHALL say so without technical details and offer to try again, keeping what was shown on a failed refresh. Both wishlists (the person's own on the Receiver page and the receiver's on the Gifter page) SHALL be refreshed as described in "Pulling a screen refreshes it" and "The web has no refresh control" and reload when the app returns to the foreground.

#### Scenario: Loading
- **WHEN** a page with a wishlist is opened
- **THEN** a progress indicator is shown in the wishlist's place until the list arrives

#### Scenario: Server unreachable
- **WHEN** the list cannot be loaded because the server cannot be reached
- **THEN** the wishlist area says so and offers "Try again"

#### Scenario: Return to the app
- **WHEN** the app returns to the foreground
- **THEN** the visible page's wishlist reloads without clearing what it shows

### Requirement: Links open in a dialog
Choosing a link shown on an item SHALL open a dialog with the whole link, not cut, with an "Open in browser" button and a "Cancel" button, and a copy icon as for other read-only text. "Open in browser" SHALL open the link in the platform's browser or default handler and close the dialog; "Cancel" SHALL close it and open nothing. Only addresses the server accepted (`http` or `https`) SHALL ever be shown as links. This applies to every place a wishlist link is shown.

#### Scenario: Show the whole link
- **WHEN** the person chooses a link of 300 characters
- **THEN** a dialog shows all of it, wrapped

#### Scenario: Open it
- **WHEN** the person chooses "Open in browser" in the link dialog
- **THEN** the link opens outside the app in the platform's browser and the dialog closes

#### Scenario: Cancel
- **WHEN** the person chooses "Cancel" in the link dialog
- **THEN** the dialog closes and nothing opens

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
