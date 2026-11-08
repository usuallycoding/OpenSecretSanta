# client-letters Specification

## Purpose
What a participant sees and does with letters in the app: the Send letter page and its sheet for a receiver, the Inbox page for a Santa, how a letter is shown and the pages' web addresses.

## Requirements

### Requirement: The Send letter page
The "Send letter" button SHALL open a page titled "Letters to your Santa" with the letters the person has sent, loaded with `GET /groups/{id}/santa/letters`, newest first, each in a card with its title, its text and when it was sent. With none sent it SHALL say so. Nothing on the page SHALL say or imply who the person's Santa is.

#### Scenario: Letters shown
- **WHEN** the person has sent two letters and opens the page
- **THEN** both are shown, the newer first, each with title, text and time

#### Scenario: Nothing sent
- **WHEN** the person has sent no letter
- **THEN** the page says they have not sent any yet

#### Scenario: No sign of the Santa
- **WHEN** the page is shown
- **THEN** it contains no name, username or other hint of who the Santa is

#### Scenario: Back
- **WHEN** the person goes back from the page
- **THEN** the group page is shown

### Requirement: Writing a letter
While the group is Active the Send letter page SHALL have a round "+" button that opens a bottom sheet with a title field and a description field, both required, and a "Send" button. A blank title or description, a title over 100 characters or a description over 2000 SHALL be refused next to its field before anything is sent. A successful send SHALL close the sheet and show the new letter first. In an Archived group there SHALL be no "+" button.

#### Scenario: Open the sheet
- **WHEN** the person presses "+" in an Active group
- **THEN** a bottom sheet opens with an empty title and an empty description

#### Scenario: Sending
- **WHEN** the person fills in both and chooses "Send"
- **THEN** the letter is sent, the sheet closes and the letter is at the top of the page

#### Scenario: Both required
- **WHEN** the person leaves the title or the description empty and chooses "Send"
- **THEN** that field says it is required, and nothing is sent

#### Scenario: Too long
- **WHEN** the title is over 100 characters or the description over 2000
- **THEN** that field says how long it may be, and nothing is sent

#### Scenario: Letter limit reached
- **WHEN** the server refuses because 200 letters were already sent
- **THEN** the sheet says so, keeps what was typed and sends nothing

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the page shows the letters and has no "+" button

#### Scenario: Dismissing
- **WHEN** the person closes the sheet without sending
- **THEN** nothing is sent and what was typed is discarded

### Requirement: The Inbox page
The "Inbox" button SHALL open a page with the letters the person's receiver has sent them, loaded with `GET /groups/{id}/receiver/letters`, newest first, each in a card with its title, its text and when it was sent, titled "Letters from" and the receiver's username (or "Letters from your receiver" until it is known). With none it SHALL say so. The page SHALL be read-only: it SHALL have no way to reply, and no "+" button.

#### Scenario: Letters shown
- **WHEN** the person's receiver `bob` has sent two letters and the person opens the Inbox
- **THEN** both are shown, the newer first, under the title "Letters from bob"

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
A letter SHALL be shown in a card with its title in a title font, its text under it, and the time it was sent in a small muted line. The title and the text SHALL keep their line breaks, and text that is long SHALL be cut after a few lines with the dialog that opens it in full, as other long text in the app. A screen reader SHALL read the title, the text and the time with labels.

#### Scenario: A long letter
- **WHEN** a letter's text is longer than the card shows
- **THEN** it is cut with an ellipsis and choosing it opens a dialog with all of it

#### Scenario: Line breaks
- **WHEN** a letter's text has several lines
- **THEN** they are shown as lines

#### Scenario: Screen reader
- **WHEN** a screen reader reads a letter
- **THEN** it reads its title, its text and when it was sent, with their labels

### Requirement: The letter pages load, fail and refresh
Each letter page SHALL show that it is loading, SHALL say without technical details when it cannot be loaded and offer "Try again", SHALL keep what it shows while a refresh fails, SHALL reload when it is shown again and with a pull to refresh, and SHALL say when the group is not available and offer the way back.

#### Scenario: Cannot be loaded
- **WHEN** the letters cannot be loaded
- **THEN** the page says so and offers "Try again"

#### Scenario: Refresh keeps what is shown
- **WHEN** a refresh fails while letters are shown
- **THEN** the letters stay on screen with the error message

#### Scenario: Group gone
- **WHEN** the server says the group does not exist for the person
- **THEN** the page says the group is not available and offers the way back to the list

### Requirement: The letter pages have web addresses
In the web app the Send letter page SHALL have the address `#/groups/{id}/outbox` and the Inbox page `#/groups/{id}/inbox`, so that a refresh or a pasted address opens the same page. An address for a group the person is not in SHALL show the not available message.

#### Scenario: Send letter address
- **WHEN** the person opens the Send letter page of group 2 on the web
- **THEN** the address ends in `#/groups/2/outbox`

#### Scenario: Inbox address
- **WHEN** the person opens the Inbox page of group 2 on the web
- **THEN** the address ends in `#/groups/2/inbox`

#### Scenario: Refresh
- **WHEN** the person refreshes the page on `#/groups/2/inbox`
- **THEN** the Inbox of group 2 is shown again

#### Scenario: The former address
- **WHEN** the person opens `#/groups/2/letters`
- **THEN** it names no screen and the first screen of their view is shown
