# client-wishlist Specification

## Purpose

TBD - created by archiving change participant-wishlists. Update Purpose after archive.

## Requirements

### Requirement: My wishlist
Choosing "My wishlist" in a group SHALL open the person's own wishlist for that group, loaded with `GET /groups/{id}/wishlist`, showing each item's title, note and link in the person's own order, or a short explanation that nothing has been added yet.

#### Scenario: Items in order
- **WHEN** the person has three items
- **THEN** they are shown in the order the person chose, each with its title and, when present, its note and link

#### Scenario: Empty list
- **WHEN** the person has not added anything to this group
- **THEN** the screen says so and offers to add the first item

#### Scenario: Separate per group
- **WHEN** the person is in two groups
- **THEN** each group's wishlist screen shows only that group's items

### Requirement: Adding an item
While the group is Pending or Active, the person SHALL be able to add an item in a bottom sheet over the list, with a title, an optional note and an optional link. A successful save through `POST /groups/{id}/wishlist/items` SHALL close the sheet and show the item at the end of the list. A list holds at most 50 items; at 50 adding SHALL be unavailable with an explanation.

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
While the group is Pending or Active, choosing an item SHALL open the same bottom sheet filled with its title, note and link. A successful save through `PUT /groups/{id}/wishlist/items/{itemId}` SHALL close the sheet and show the changed item in the same place in the list.

#### Scenario: Edit an item
- **WHEN** the person changes an item's note and saves
- **THEN** the sheet closes and the item shows the new note in the same position

#### Scenario: Clear the note and link
- **WHEN** the person empties the note and the link and saves
- **THEN** the item no longer shows a note or a link

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
While the group is Archived, "My wishlist" SHALL be read-only: no add, edit, delete or reorder control is shown and the screen says the list can no longer be changed. When the group is archived while the person is editing, a refused change SHALL close the sheet, reload the list as read-only and say so.

#### Scenario: Archived group
- **WHEN** the person opens "My wishlist" of an Archived group
- **THEN** the items are shown with no way to change them and a note that the list is closed

#### Scenario: Archived meanwhile
- **WHEN** the server answers a change with 409 because the group was archived after the screen opened
- **THEN** the sheet closes, the list reloads read-only and the screen says it can no longer be changed

#### Scenario: Item deleted elsewhere
- **WHEN** the server answers an edit or delete with 404 because the item no longer exists
- **THEN** the sheet closes, the list reloads and the screen says the item no longer exists

### Requirement: Links open in the browser
A link shown on an item SHALL open in the platform's browser or default handler when chosen, and only addresses the server accepted (`http` or `https`) SHALL ever be shown as links.

#### Scenario: Open a link
- **WHEN** the person chooses the link of an item
- **THEN** it opens outside the app in the platform's browser

### Requirement: Your receiver's wishlist
While the group is Active or Archived, choosing "Your receiver's wishlist" SHALL open the list the person's receiver keeps for the group, loaded with `GET /groups/{id}/receiver/wishlist`, read-only, in the receiver's order, with each item's title, note and link, or a short explanation that nothing has been added yet. The screen SHALL NOT name the receiver and nothing the person does SHALL be visible to the receiver.

#### Scenario: Receiver's items
- **WHEN** the person opens it and the receiver has two items
- **THEN** both are shown in the receiver's order with their note and link, and no control changes them

#### Scenario: Nothing added yet
- **WHEN** the receiver has not added anything
- **THEN** the screen says the receiver has not added anything yet

#### Scenario: Latest list
- **WHEN** the receiver changed their list after the person last looked
- **THEN** refreshing or returning to the app shows the current list

### Requirement: Loading, errors and refresh
While a wishlist screen loads, the app SHALL show a progress indicator. When it cannot be loaded the app SHALL say so without technical details and offer to try again, keeping what was shown on a failed refresh. Both wishlist screens SHALL offer a refresh action and reload when the app returns to the foreground.

#### Scenario: Loading
- **WHEN** a wishlist screen is opened
- **THEN** a progress indicator is shown until the list arrives

#### Scenario: Server unreachable
- **WHEN** the list cannot be loaded because the server cannot be reached
- **THEN** the screen says so and offers "Try again"

#### Scenario: Return to the app
- **WHEN** the app returns to the foreground
- **THEN** the visible wishlist screen reloads without clearing what it shows
