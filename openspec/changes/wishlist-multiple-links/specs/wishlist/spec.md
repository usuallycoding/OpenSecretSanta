## MODIFIED Requirements

### Requirement: Wishlist item
An item SHALL have a title, an optional note and up to 10 links, and SHALL record when it was created and when it was last changed. The title SHALL be 1 to 200 characters, the note at most 1000 and each link at most 2000, with surrounding whitespace removed and blank optional values treated as empty.

#### Scenario: Title only
- **WHEN** a participant adds an item with just a title
- **THEN** the item has that title, no note and no links

#### Scenario: Blank or too long title
- **WHEN** a title is blank or longer than 200 characters
- **THEN** the response is 400 naming `title` and nothing is stored

#### Scenario: Long note or link
- **WHEN** a note is longer than 1000 characters or a link is longer than 2000
- **THEN** the response is 400 naming `note` or the place of that link, such as `urls[1]`, and nothing is stored

#### Scenario: Whitespace and blanks
- **WHEN** the title is `  Wool socks  ` and the note is `   `
- **THEN** the stored title is `Wool socks` and the note is empty

### Requirement: Links are web addresses only
A link SHALL be an absolute `http` or `https` address with a host; any other scheme or form SHALL be rejected so that clients can show it safely.

#### Scenario: Valid links
- **WHEN** the link is `https://shop.example/item?id=7` or `HTTP://shop.example`
- **THEN** it is accepted

#### Scenario: Unsafe or malformed links
- **WHEN** the second link is `javascript:alert(1)`, `data:text/html,x`, `file:///etc/passwd`, `ftp://host/file`, `//host/path` or `not a url`
- **THEN** the response is 400 naming `urls[1]` and nothing is stored

### Requirement: Read your wishlist
The server SHALL expose `GET /groups/{id}/wishlist` returning the caller's items for that group in their chosen order, each with its id, title, note, links in their order, creation time and last-changed time. For apps that predate several links, each item SHALL also carry its first link, or none, as the deprecated `url`.

#### Scenario: Items in order
- **WHEN** a participant has three items in a group
- **THEN** the response lists them in the participant's current order with their ids

#### Scenario: Several links
- **WHEN** an item has three links
- **THEN** the response lists all three in their order in `urls` and the first of them as `url`

#### Scenario: No link
- **WHEN** an item has no link
- **THEN** its `urls` is empty and it has no `url`

### Requirement: Edit an item
The server SHALL expose `PUT /groups/{id}/wishlist/items/{itemId}` that replaces the title, note and links of one of the caller's items, keeping its place in the list, keeping its creation time and updating its last-changed time. A change to the links, adding, removing or altering one, SHALL count as a change to the item.

#### Scenario: Edit keeps the place
- **WHEN** a participant edits the second of three items
- **THEN** it is still second, shows the new values and a later last-changed time

#### Scenario: Clearing note and link
- **WHEN** an edit sends no note and an empty `urls`
- **THEN** the item has neither

#### Scenario: Only the links changed
- **WHEN** an edit keeps the title and note but adds a link
- **THEN** the item shows the new link and a later last-changed time

#### Scenario: Invalid edit
- **WHEN** an edit has an invalid title or link
- **THEN** the response is 400 and the item is unchanged

## ADDED Requirements

### Requirement: Up to 10 links per item
An item SHALL hold at most 10 links, kept in the order they were sent. Blank links SHALL be dropped, and a link repeated after whitespace is removed SHALL be kept once, at its first place. A request with more than 10 links once blanks and repeats are gone SHALL be rejected.

#### Scenario: Ten links
- **WHEN** a participant adds an item with 10 different links
- **THEN** the item is stored with the 10 links in the order sent

#### Scenario: Eleven links
- **WHEN** a request has 11 different links
- **THEN** the response is 400 naming `urls` and nothing is stored

#### Scenario: Blanks dropped
- **WHEN** a request has the links `https://a.example`, `   ` and `https://b.example`
- **THEN** the item has two links, in that order

#### Scenario: Repeats removed
- **WHEN** a request has `https://a.example`, `https://b.example` and ` https://a.example `
- **THEN** the item has the first two only, and the repeated link does not count towards the 10

### Requirement: Links in requests, and apps that send one link
`POST /groups/{id}/wishlist/items` and `PUT /groups/{id}/wishlist/items/{itemId}` SHALL accept the links as `urls`. When a request has `urls`, it SHALL decide the item's links and any `url` in it SHALL be ignored. When it has only the deprecated `url`, an add SHALL store it as the item's only link, and an edit SHALL replace the item's first link with it, or remove the first link when `url` is empty, and keep the other links as they are.

#### Scenario: Urls decide
- **WHEN** an edit sends `urls` with two links and also a `url` of another link
- **THEN** the item has the two links of `urls`

#### Scenario: An older app saves an item
- **WHEN** an item has three links and an older app edits its title, sending only `url` with the first link as it was
- **THEN** the item still has all three links

#### Scenario: An older app changes the link
- **WHEN** an item has three links and an older app sends only a `url` with another address
- **THEN** the item's first link is the new address and the second and third are unchanged

#### Scenario: An older app clears the link
- **WHEN** an item has three links and an older app sends no `url` and no `urls`
- **THEN** the first link is removed and the other two remain, in order

#### Scenario: An older app adds an item
- **WHEN** an older app adds an item with a `url`
- **THEN** the item has that one link

### Requirement: Existing links keep their items
Links that items had before an item could hold several SHALL remain, each as the first and only link of its item, and items without a link SHALL stay without one.

#### Scenario: Upgrade keeps links
- **WHEN** the server starts on a database where an item has the link `https://shop.example/socks` and another item has none
- **THEN** the first item has that one link, shown as its first link, and the second still has none

#### Scenario: Other fields untouched
- **WHEN** the upgrade has run
- **THEN** every item keeps its title, note, place, creation time and last-changed time
