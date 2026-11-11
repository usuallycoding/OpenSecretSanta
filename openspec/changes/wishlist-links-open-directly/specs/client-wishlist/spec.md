## ADDED Requirements

### Requirement: Links open in the browser
Choosing a link shown on an item SHALL open it at once in the platform's browser or default handler, with no dialog or other step between. Only addresses the server accepted (`http` or `https`) SHALL ever be shown as links. When nothing can open the link, nothing SHALL happen. This applies to every link of every place a wishlist is shown.

#### Scenario: Open it
- **WHEN** the person chooses a link on an item
- **THEN** the link opens outside the app in the platform's browser and no dialog is shown

#### Scenario: Each link opens itself
- **WHEN** an item shows three links and the person chooses the second
- **THEN** the second link opens and the other two do not

#### Scenario: Nothing can open it
- **WHEN** the platform cannot open the link
- **THEN** nothing happens and the item is unchanged

### Requirement: A link is one line with a copy icon
A link SHALL be shown on one line, cut with an ellipsis when it does not fit, with a copy icon as for other read-only text that copies the whole link without opening it. A screen reader SHALL read a link as one that opens in the browser.

#### Scenario: A long link
- **WHEN** an item has a link of 300 characters
- **THEN** it is shown on one line, cut with an ellipsis, and choosing it opens the whole link

#### Scenario: Copy the whole link
- **WHEN** the person chooses the copy icon of a link of 300 characters
- **THEN** all 300 characters are copied and the link does not open

#### Scenario: Read as a link
- **WHEN** a screen reader reaches a link
- **THEN** it is read as a link that opens in the browser

## REMOVED Requirements

### Requirement: Links open in a dialog
**Reason**: A dialog before every link is an extra step for someone who only wants the page; links now open directly (see "Links open in the browser").
**Migration**: None. Choosing a link opens it; the whole link can still be copied with the copy icon next to it.
