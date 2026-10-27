# Spec Delta

## Purpose

Defines the wishlist each participant keeps for a Secret Santa group in Open Secret Santa: what an item is, how the owner adds, edits, deletes and reorders items, when the list can be changed, the limits that apply, and who is allowed to read it.

## ADDED Requirements

### Requirement: One wishlist per participant per group
Each participant of a group SHALL have their own wishlist for that group, separate from their wishlists in any other group. A participant's wishlist for a group SHALL be empty until they add items.

#### Scenario: Empty to begin with
- **WHEN** a participant who has never added an item reads their wishlist for a group
- **THEN** the response is 200 with an empty list

#### Scenario: Separate lists per group
- **WHEN** a user who participates in two groups adds an item in the first
- **THEN** the item appears in their wishlist for the first group and not in their wishlist for the second

#### Scenario: Only participants have a list
- **WHEN** a user who does not participate in a group, or asks about a group that does not exist, requests that group's wishlist
- **THEN** the response is 404

#### Scenario: Drafts have no lists
- **WHEN** a participant named in a Draft group requests a wishlist for it
- **THEN** the response is 404

### Requirement: Wishlist item
An item SHALL have a title, an optional note and an optional link, and SHALL record when it was created and when it was last changed. The title SHALL be 1 to 200 characters, the note at most 1000 and the link at most 2000, with surrounding whitespace removed and blank optional values treated as empty.

#### Scenario: Title only
- **WHEN** a participant adds an item with just a title
- **THEN** the item has that title, no note and no link

#### Scenario: Blank or too long title
- **WHEN** a title is blank or longer than 200 characters
- **THEN** the response is 400 naming `title` and nothing is stored

#### Scenario: Long note or link
- **WHEN** a note is longer than 1000 characters or a link longer than 2000
- **THEN** the response is 400 naming `note` or `url` and nothing is stored

#### Scenario: Whitespace and blanks
- **WHEN** the title is `  Wool socks  ` and the note is `   `
- **THEN** the stored title is `Wool socks` and the note is empty

### Requirement: Links are web addresses only
A link SHALL be an absolute `http` or `https` address with a host; any other scheme or form SHALL be rejected so that clients can show it safely.

#### Scenario: Valid links
- **WHEN** the link is `https://shop.example/item?id=7` or `HTTP://shop.example`
- **THEN** it is accepted

#### Scenario: Unsafe or malformed links
- **WHEN** the link is `javascript:alert(1)`, `data:text/html,x`, `file:///etc/passwd`, `ftp://host/file`, `//host/path` or `not a url`
- **THEN** the response is 400 naming `url` and nothing is stored

### Requirement: Editable until the group is archived
A participant SHALL be able to add, edit, delete and reorder items while their group is Pending or Active, and SHALL be able to read their list while it is Pending, Active or Archived. Once the group is Archived the list SHALL be read-only.

#### Scenario: Preparing before the draw
- **WHEN** a participant of a Pending group adds an item
- **THEN** the item is stored

#### Scenario: Changing during the exchange
- **WHEN** a participant of an Active group edits an item
- **THEN** the change is stored

#### Scenario: Archived is read-only
- **WHEN** a participant of an Archived group adds, edits, deletes or reorders items
- **THEN** the response is 409 with code `invalid_state` and the list is unchanged, while reading it still works

### Requirement: Read your wishlist
The server SHALL expose `GET /groups/{id}/wishlist` returning the caller's items for that group in their chosen order, each with its id, title, note, link, creation time and last-changed time.

#### Scenario: Items in order
- **WHEN** a participant has three items in a group
- **THEN** the response lists them in the participant's current order with their ids

### Requirement: Add an item
The server SHALL expose `POST /groups/{id}/wishlist/items` that appends a new item at the end of the caller's list for that group and returns it with status 201. A list SHALL hold at most 50 items.

#### Scenario: Appended last
- **WHEN** a participant with two items adds a third
- **THEN** the new item is last in the list

#### Scenario: List full
- **WHEN** a participant who already has 50 items in the group adds another
- **THEN** the response is 409 with code `invalid_state` and the list is unchanged

### Requirement: Edit an item
The server SHALL expose `PUT /groups/{id}/wishlist/items/{itemId}` that replaces the title, note and link of one of the caller's items, keeping its place in the list, keeping its creation time and updating its last-changed time.

#### Scenario: Edit keeps the place
- **WHEN** a participant edits the second of three items
- **THEN** it is still second, shows the new values and a later last-changed time

#### Scenario: Clearing note and link
- **WHEN** an edit sends no note and no link
- **THEN** the item has neither

#### Scenario: Invalid edit
- **WHEN** an edit has an invalid title or link
- **THEN** the response is 400 and the item is unchanged

### Requirement: Delete an item
The server SHALL expose `DELETE /groups/{id}/wishlist/items/{itemId}` that permanently removes one of the caller's items and returns 204, leaving the other items in their order.

#### Scenario: Remaining order kept
- **WHEN** a participant deletes the middle of three items
- **THEN** the other two remain in the same relative order

### Requirement: Reorder the list
The server SHALL expose `PUT /groups/{id}/wishlist/order` taking the complete list of the caller's item ids for that group in the desired order, and SHALL apply the new order in one step only if the ids are exactly the caller's current items in that group, each once.

#### Scenario: Reordered
- **WHEN** a participant sends their three item ids in a new order
- **THEN** a later read returns the items in that order

#### Scenario: Stale or wrong list
- **WHEN** the ids omit an item, repeat one or include an id that is not the caller's item in this group
- **THEN** the response is 400 naming `itemIds` and the order is unchanged

#### Scenario: Item added elsewhere meanwhile
- **WHEN** a second device added an item after the first device read the list and the first device sends its older id list
- **THEN** the response is 400 and nothing is reordered

### Requirement: Only your own items
A participant SHALL be able to read, change, delete and reorder only their own items in the group named by the route. An id that does not exist, belongs to someone else or belongs to the caller's list in another group SHALL be answered as not found.

#### Scenario: Someone else's item
- **WHEN** a participant edits or deletes an item id that belongs to another participant
- **THEN** the response is 404 and that item is unchanged

#### Scenario: Item from another group
- **WHEN** a participant edits or deletes, through one group's route, an item that belongs to their list in a different group
- **THEN** the response is 404 and that item is unchanged

### Requirement: Wishlist is private
A wishlist SHALL be readable only by its owner and by the participant who is the owner's Santa in that group once it is Active or Archived, in the way described by the gift-visibility capability. No admin endpoint SHALL return it.

#### Scenario: Other participants
- **WHEN** a participant who is not the owner's Santa tries to read the owner's wishlist through the group
- **THEN** they can only read the wishlist of their own receiver

#### Scenario: Admin endpoints
- **WHEN** an admin uses any admin endpoint
- **THEN** no response contains wishlist items

### Requirement: Authenticated and after the password change
All wishlist routes under `/groups/{id}/wishlist` SHALL require authentication, and a user who must still change a temporary password SHALL be refused with `password_change_required`.

#### Scenario: Not signed in
- **WHEN** an unauthenticated request reaches `/groups/{id}/wishlist`
- **THEN** the response is 401

#### Scenario: Temporary password
- **WHEN** a user with a temporary password requests a wishlist
- **THEN** the response is 403 with code `password_change_required`
