# Spec Delta

## MODIFIED Requirements

### Requirement: The Send letter page
The "Send letter" button SHALL open a page titled "Letters to your Santa" with the letters the person has sent, loaded with `GET /groups/{id}/santa/letters`, newest first, each in a card with its title and its text. With none sent it SHALL say so. Nothing on the page SHALL say or imply who the person's Santa is.

#### Scenario: Letters shown
- **WHEN** the person has sent two letters and opens the page
- **THEN** both are shown, the newer first, each with title and text and no time

#### Scenario: Nothing sent
- **WHEN** the person has sent no letter
- **THEN** the page says they have not sent any yet

#### Scenario: No sign of the Santa
- **WHEN** the page is shown
- **THEN** it contains no name, username or other hint of who the Santa is

#### Scenario: Back
- **WHEN** the person goes back from the page
- **THEN** the group page is shown

### Requirement: The Inbox page
The "Inbox" button SHALL open a page with the letters the person's receiver has sent them, loaded with `GET /groups/{id}/receiver/letters`, newest first, each in a card with its title and its text, titled "Letters from" and the receiver's username (or "Letters from your receiver" until it is known). With none it SHALL say so. The page SHALL be read-only: it SHALL have no way to reply, and no "+" button.

#### Scenario: Letters shown
- **WHEN** the person's receiver `bob` has sent two letters and the person opens the Inbox
- **THEN** both are shown, the newer first, under the title "Letters from bob", with no time

#### Scenario: Nothing yet
- **WHEN** the receiver has sent no letter
- **THEN** the page says no letters have come yet

#### Scenario: Read-only
- **WHEN** the Inbox is shown, whatever the group's state
- **THEN** there is no control that replies to, changes or deletes a letter, and no "+" button

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the letters are still shown

### Requirement: Letters are cards that keep their words
A letter SHALL be shown in a card with its title in a title font and its text under it; the time it was sent is shown in a small muted line only on the owner's pair sheet in the Admin view. The title and the text SHALL keep their line breaks, and text that is long SHALL be cut after a few lines with the dialog that opens it in full, as other long text in the app. A screen reader SHALL read the title and the text with labels, and the time with its label where it is shown.

#### Scenario: A long letter
- **WHEN** a letter's text is longer than the card shows
- **THEN** it is cut with an ellipsis and choosing it opens a dialog with all of it

#### Scenario: Line breaks
- **WHEN** a letter's text has several lines
- **THEN** they are shown as lines

#### Scenario: Screen reader
- **WHEN** a screen reader reads a letter on the Send letter page or the Inbox
- **THEN** it reads its title and its text, with their labels

#### Scenario: Owner's pair sheet
- **WHEN** the owner reads a pair's letters in the Admin view
- **THEN** each card shows when the letter was sent, and a screen reader reads it with its label
