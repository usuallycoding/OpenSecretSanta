# client-read-marks Specification

## Purpose
What a Santa sees in the app of what is new from their receiver: badges on three buttons of the group page, and a pill and an accent bar on each new or edited wishlist item, new answer and new letter, cleared the next time the page is opened.

## Requirements

### Requirement: Badges on the Santa's buttons
From Active on, the "See Wishlist", "Ask Questions" and "Inbox" buttons of the "You're Secret Santa for:" card SHALL each carry a badge on its corner in the theme's tertiary colour, from `GET /groups/{id}/receiver/reads`: the number of new or edited wishlist items, of new answers and of new letters, "9+" above 9. With none, while the marks load or when loading fails, a button SHALL carry no badge. A screen reader SHALL hear the count with the button's name and not the badge on its own.

#### Scenario: Counts shown
- **WHEN** the receiver has added two items and edited one, answered a question and sent three letters since the Santa last looked
- **THEN** "See Wishlist" shows 3, "Ask Questions" shows 1 and "Inbox" shows 3

#### Scenario: Nothing new
- **WHEN** nothing is new on a page
- **THEN** its button has no badge

#### Scenario: Cleared after looking
- **WHEN** the Santa opens the Inbox, and goes back to the group page
- **THEN** the Inbox badge is gone

#### Scenario: Many
- **WHEN** twelve letters are new
- **THEN** the badge shows "9+"

#### Scenario: Not known
- **WHEN** the marks have not loaded or could not be loaded
- **THEN** no badge is shown and the page shows no message about it

#### Scenario: Screen reader
- **WHEN** a screen reader reads "Inbox" while two letters are new
- **THEN** it reads "Inbox, 2 new" and not the badge on its own

#### Scenario: Pending group
- **WHEN** the group is Pending
- **THEN** the buttons are not there, so no badge is

### Requirement: New and edited wishlist items are marked
On the receiver's wishlist page, pane or column an item created after the baseline SHALL show a "New" pill and an item created before it and changed after it an "Edited" pill, and each marked item's card SHALL have an accent bar on its start edge, the pills and the bar in the theme's tertiary colour. The baseline SHALL be the time seen up to that the server held when the page was opened, and SHALL not change while the page stays open.

#### Scenario: New item
- **WHEN** the receiver added an item after the Santa last looked
- **THEN** the item shows "New" and has the accent bar

#### Scenario: Edited item
- **WHEN** the receiver edited an older item after the Santa last looked
- **THEN** the item shows "Edited" and has the accent bar

#### Scenario: New and edited
- **WHEN** an item was created and then changed after the Santa last looked
- **THEN** it shows "New" only

#### Scenario: Unchanged item
- **WHEN** an item has not changed since the Santa last looked
- **THEN** it has no pill and no bar

#### Scenario: Refreshing on the page
- **WHEN** the Santa pulls to refresh while the page is open
- **THEN** the marks stay, and an item that arrived meanwhile is marked too

#### Scenario: Never looked
- **WHEN** the Santa opens the wishlist for the first time
- **THEN** every item is marked "New"

### Requirement: New answers are marked
On the questions page, pane or column of the Gifter side a question whose answer was given or changed after the baseline SHALL show a "New" pill with the answer and its card SHALL have the accent bar, in the theme's tertiary colour. Unanswered questions and answers seen before SHALL look as they do now.

#### Scenario: New answer
- **WHEN** the receiver answered a question after the Santa last looked
- **THEN** the question shows "New" with the answer and the card has the bar

#### Scenario: Changed answer
- **WHEN** the receiver changed an answer the Santa had read
- **THEN** it shows "New" again

#### Scenario: Seen answer
- **WHEN** an answer is older than the Santa's last look
- **THEN** it has no pill and no bar

### Requirement: New letters are marked
On the Inbox page a letter sent after the baseline SHALL show a "New" pill and its card SHALL have the accent bar, in the theme's tertiary colour.

#### Scenario: New letter
- **WHEN** the receiver sent a letter after the Santa last looked
- **THEN** the letter shows "New" and has the accent bar

#### Scenario: Seen letters
- **WHEN** a letter was sent before the Santa's last look
- **THEN** it has no pill and no bar

### Requirement: Looking clears the marks for the next time
When a page, a pane or a column has been shown with its list loaded, the app SHALL tell the server the newest time it showed, so that the next time it is opened nothing that was marked is marked again. The marks SHALL stay until it is left or closed. A page SHALL be marked seen only when it is shown, not when another page of the same group is. Marking that fails SHALL change nothing on screen.

#### Scenario: Gone the next time
- **WHEN** the Santa opens the Inbox with two new letters, leaves it and opens it again
- **THEN** the letters have no pill and no bar the second time

#### Scenario: Still there while the page is open
- **WHEN** the Santa stays on the Inbox
- **THEN** the pills and bars stay

#### Scenario: Only the shown tab
- **WHEN** the Santa opens the questions page and never opens the receiver's wishlist
- **THEN** the wishlist badge on the group page is unchanged

#### Scenario: New things after looking
- **WHEN** the receiver changes something after the Santa looked
- **THEN** the next time it is marked and counted again

#### Scenario: Another device
- **WHEN** the Santa looked at the letters on their phone
- **THEN** the Inbox badge is gone on their other devices too after they reload

#### Scenario: Marking fails
- **WHEN** the server cannot be reached while marking
- **THEN** the page looks the same and the marks come back next time

### Requirement: Read marks are the Santa's only
The pills, bars and badges SHALL appear only on the Santa's pages of their receiver's things, never on a receiver's own wishlist, questions or letters, and nothing on a screen SHALL tell a receiver that anything was read.

#### Scenario: A receiver's own pages
- **WHEN** a receiver opens their own wishlist, the questions from their Santa or the letters they sent
- **THEN** none shows a read pill, bar or badge from reads

#### Scenario: Nothing says it was read
- **WHEN** a Santa has read everything
- **THEN** nothing the receiver sees changes
