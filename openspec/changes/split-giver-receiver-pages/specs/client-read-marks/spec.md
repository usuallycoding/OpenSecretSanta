# Spec Delta

## MODIFIED Requirements

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
On the questions page, pane or column of the Gifter side a question whose answer was given or changed after the baseline SHALL show a "New" pill beside the time of the answer, and its card SHALL have the accent bar, in the theme's tertiary colour. Unanswered questions and answers seen before SHALL look as they do now.

#### Scenario: New answer
- **WHEN** the receiver answered a question after the Santa last looked
- **THEN** the question shows "New" beside the answer's time and the card has the bar

#### Scenario: Changed answer
- **WHEN** the receiver changed an answer the Santa had read
- **THEN** it shows "New" again

#### Scenario: Seen answer
- **WHEN** an answer is older than the Santa's last look
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
