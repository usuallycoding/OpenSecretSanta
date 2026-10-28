# client-wishlist Specification

## Purpose

TBD - created by archiving change participant-wishlists. Update Purpose after archive.

## Requirements

### Requirement: Adding an item
While the group is Pending or Active, the person SHALL be able to add an item with the floating "Add item" button, in a bottom sheet over the list, with a title, an optional note and an optional link. A successful save through `POST /groups/{id}/wishlist/items` SHALL close the sheet and show the item at the end of the list. A list holds at most 50 items; at 50 adding SHALL be unavailable with an explanation.

#### Scenario: Add an item
- **WHEN** the person opens the add sheet, enters a title and saves
- **THEN** the sheet closes and the item is the last one in the list

#### Scenario: List is full
- **WHEN** the list already has 50 items
- **THEN** the add action is unavailable and says the list is full

#### Scenario: Dismissing the sheet
- **WHEN** the person closes the sheet without saving
- **THEN** nothing is added and what they typed is discarded

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

### Requirement: Deleting an item
While the group is Pending or Active, the person SHALL be able to delete an item from its edit sheet, after a confirmation that names the item. Confirming SHALL remove it permanently with `DELETE /groups/{id}/wishlist/items/{itemId}` and leave the others in order.

#### Scenario: Confirm deletion
- **WHEN** the person chooses delete and confirms
- **THEN** the item disappears and the other items keep their order

#### Scenario: Cancel deletion
- **WHEN** the person chooses delete and cancels
- **THEN** the item stays

### Requirement: Reordering items
While the group is Pending or Active, each item SHALL have buttons to move it up and down. A move SHALL send the complete list of item ids in the new order with `PUT /groups/{id}/wishlist/order` and show the order the server returns. The first item cannot move up and the last cannot move down, and the buttons are unavailable while a move is being saved.

#### Scenario: Move an item up
- **WHEN** the person moves the second item up
- **THEN** it becomes the first and the new order is what the server returns

#### Scenario: Ends of the list
- **WHEN** an item is first or last
- **THEN** its up or its down button is unavailable

#### Scenario: List changed elsewhere
- **WHEN** the server refuses a move with a 400 naming `itemIds` because another device changed the list
- **THEN** the app reloads the list and says it was changed elsewhere

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
