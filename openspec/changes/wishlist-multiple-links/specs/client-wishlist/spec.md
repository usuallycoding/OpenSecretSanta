## MODIFIED Requirements

### Requirement: Adding an item
While the group is Pending or Active, the person SHALL be able to add an item with the floating "Add item" button, in a bottom sheet over the list, with a title, an optional note and up to 10 optional links. A successful save through `POST /groups/{id}/wishlist/items` SHALL close the sheet and show the item at the end of the list. A list holds at most 200 items; at 200 adding SHALL be unavailable with an explanation.

#### Scenario: Add an item
- **WHEN** the person opens the add sheet, enters a title and saves
- **THEN** the sheet closes and the item is the last one in the list

#### Scenario: Add an item with links
- **WHEN** the person enters a title and two links and saves
- **THEN** the item is the last one in the list and shows both links in the order entered

#### Scenario: List is full
- **WHEN** the list already has 200 items
- **THEN** the add action is unavailable and says the list is full

#### Scenario: Dismissing the sheet
- **WHEN** the person closes the sheet without saving
- **THEN** nothing is added and what they typed is discarded

### Requirement: Editing an item
While the group is Pending or Active, each item SHALL end with an edit button, and choosing it SHALL open the same bottom sheet filled with the item's title, note and links. Choosing the rest of the item SHALL NOT open the sheet. A successful save through `PUT /groups/{id}/wishlist/items/{itemId}` SHALL close the sheet and show the changed item in the same place in the list.

#### Scenario: Edit an item
- **WHEN** the person chooses an item's edit button, changes its note and saves
- **THEN** the sheet closes and the item shows the new note in the same position

#### Scenario: Sheet shows every link
- **WHEN** the person opens the edit sheet of an item with three links
- **THEN** the sheet shows the three links in their order, each in its own field

#### Scenario: Clear the note and link
- **WHEN** the person empties the note, removes every link and saves
- **THEN** the item no longer shows a note or a link

#### Scenario: The card is not a button
- **WHEN** the person chooses the body of an item rather than its edit button
- **THEN** no sheet opens

#### Scenario: Closed list
- **WHEN** the group is Archived
- **THEN** items have no edit button

### Requirement: Item form validation
Before sending, the sheet SHALL check what the server checks: a title of 1 to 200 characters, a note of at most 1000, and links that are each at most 2000 characters and an absolute `http` or `https` address. A problem SHALL be shown next to its field, next to the link it belongs to, and nothing SHALL be sent. A validation error from the server naming a field or a link SHALL be shown next to that field or link, and a failed save SHALL keep the sheet open with what the person typed.

#### Scenario: Missing title
- **WHEN** the person saves with an empty title
- **THEN** the title field says a title is needed and nothing is sent

#### Scenario: Not a web address
- **WHEN** the person enters a link such as `javascript:alert(1)` or `ftp://host/file`
- **THEN** that link's field says it must be an http or https address and nothing is sent

#### Scenario: Only the bad link is flagged
- **WHEN** the person enters one valid link and then `ftp://host/file`
- **THEN** the message is shown next to the second link only and nothing is sent

#### Scenario: Server names a link
- **WHEN** the server answers 400 naming `urls[1]`
- **THEN** the message is shown next to the second link and the sheet stays open with what the person typed

#### Scenario: Server cannot be reached
- **WHEN** a save cannot reach the server
- **THEN** the sheet says so, stays open and keeps the edits so the person can try again

### Requirement: Links open in a dialog
Choosing a link shown on an item SHALL open a dialog with the whole link, not cut, with an "Open in browser" button and a "Cancel" button, and a copy icon as for other read-only text. "Open in browser" SHALL open the link in the platform's browser or default handler and close the dialog; "Cancel" SHALL close it and open nothing. Only addresses the server accepted (`http` or `https`) SHALL ever be shown as links. This applies to every link of every place a wishlist is shown.

#### Scenario: Show the whole link
- **WHEN** the person chooses a link of 300 characters
- **THEN** a dialog shows all of it, wrapped

#### Scenario: Open it
- **WHEN** the person chooses "Open in browser" in the link dialog
- **THEN** the link opens outside the app in the platform's browser and the dialog closes

#### Scenario: Cancel
- **WHEN** the person chooses "Cancel" in the link dialog
- **THEN** the dialog closes and nothing opens

#### Scenario: Each link has its own dialog
- **WHEN** an item shows three links and the person chooses the second
- **THEN** the dialog shows the second link only

## ADDED Requirements

### Requirement: Managing the links of an item
The add and edit sheet SHALL show the item's links one field each, in order, starting with one empty field. An "Add link" button SHALL add an empty field after the last, up to 10 fields, and each field SHALL have a button that removes it. At 10 fields "Add link" SHALL be unavailable and the sheet SHALL say that 10 is the most. The sheet SHALL show how many links it holds out of 10. Blank fields SHALL be ignored on saving.

#### Scenario: Add a link field
- **WHEN** the person chooses "Add link" with one field shown
- **THEN** a second empty field appears below the first

#### Scenario: Remove a link field
- **WHEN** the person removes the second of three links
- **THEN** the other two stay in their order and the sheet shows two fields

#### Scenario: Removing the last field
- **WHEN** the person removes the only link field
- **THEN** the sheet shows one empty field again

#### Scenario: Ten links
- **WHEN** the sheet holds 10 link fields
- **THEN** "Add link" is unavailable and says that 10 links is the most

#### Scenario: Blank fields ignored
- **WHEN** the person leaves an empty field between two links and saves
- **THEN** the item shows the two links only

### Requirement: Every link of an item is shown
An item SHALL show each of its links, one under another in their order, between its note and its times, wherever the item is shown. An item without links SHALL show none.

#### Scenario: Three links
- **WHEN** an item has three links
- **THEN** the item shows them one under another in their order

#### Scenario: No links
- **WHEN** an item has no link
- **THEN** the item shows no link
