# Spec Delta

## MODIFIED Requirements

### Requirement: The Secret Santa card
Below the group's information the group page SHALL show a card headed "You're Secret Santa for:" with, on the next row in a bold title font, "TBD" while the group is Pending and the receiver's username in capital letters once it is Active or Archived, and under it the line "Shh... Keep it a secret!" in italics. From Active on it SHALL have "See Wishlist" and "Ask Questions" on one row, then "Inbox" and, last, "Delivery Info", each on its own row. While Pending it SHALL have no button.

#### Scenario: Pending
- **WHEN** the group is Pending
- **THEN** the card shows the heading and "TBD" and no button

#### Scenario: The secret line
- **WHEN** the card is shown in a Pending, an Active or an Archived group
- **THEN** the line "Shh... Keep it a secret!" is under "TBD" or the name, in italics

#### Scenario: Active
- **WHEN** the group is Active and the person's receiver is `bob`
- **THEN** the card shows the heading, "BOB", the line "Shh... Keep it a secret!", the buttons "See Wishlist" and "Ask Questions", an "Inbox" button and, last, a "Delivery Info" button

#### Scenario: Archived
- **WHEN** the group is Archived
- **THEN** the card is shown as for an Active group, and the pages it opens are read-only as before, "Inbox" included

#### Scenario: The name as it is
- **WHEN** a screen reader reads the card for the receiver `bob`
- **THEN** it reads "bob", not a spelled-out capital, and the sheet shows `bob` as written

#### Scenario: Delivery Info
- **WHEN** the person chooses "Delivery Info"
- **THEN** the receiver's details sheet opens

#### Scenario: See Wishlist
- **WHEN** the person chooses "See Wishlist"
- **THEN** the Gifter page opens on its wishlist tab

#### Scenario: Ask Questions
- **WHEN** the person chooses "Ask Questions"
- **THEN** the Gifter page opens on its questions tab

#### Scenario: Inbox
- **WHEN** the person chooses "Inbox"
- **THEN** the Inbox page opens, with the letters their receiver has sent them

#### Scenario: Receiver not known
- **WHEN** the receiver request has not finished or has failed
- **THEN** the name reads "..." and the four buttons are disabled, and a failure shows its message with "Try again"

### Requirement: The Your stuff card
Below the Secret Santa card the group page SHALL show a card headed "Your stuff:", the line "Tell your santa what you want and let them know you!" in italics under it, "Edit Wishlist" and, from Active on, "Respond to Santa" on one row, then, from Active on, "Send letter" on its own row and, last, "Delivery Info" on its own row. In an Archived group it SHALL also have, above that, the "Reveal Santa" button of `The Santa stays hidden until the group is archived`.

#### Scenario: The encouraging line
- **WHEN** the card is shown in a Pending, an Active or an Archived group
- **THEN** the line "Tell your santa what you want and let them know you!" is under "Your stuff:", in italics

#### Scenario: Pending
- **WHEN** the group is Pending
- **THEN** the card has "Delivery Info" and "Edit Wishlist" and no "Respond to Santa" and no "Send letter"

#### Scenario: Active
- **WHEN** the group is Active
- **THEN** the card has "Delivery Info", "Edit Wishlist", "Respond to Santa" and "Send letter" and no "Reveal Santa"

#### Scenario: Archived
- **WHEN** the group is Archived
- **THEN** the card has "Delivery Info", "Edit Wishlist", "Respond to Santa", "Send letter" and "Reveal Santa"

#### Scenario: Delivery Info
- **WHEN** the person chooses "Delivery Info"
- **THEN** a sheet shows their own username and the delivery info they have set for this group

#### Scenario: Edit Wishlist
- **WHEN** the person chooses "Edit Wishlist"
- **THEN** the Receiver page opens on their wishlist tab

#### Scenario: Send letter
- **WHEN** the person chooses "Send letter"
- **THEN** the Send letter page opens, with the letters they have sent their Santa

#### Scenario: Respond to Santa
- **WHEN** the person chooses "Respond to Santa" in an Active group
- **THEN** the Receiver page opens on its "Respond to Secret Santa" tab

#### Scenario: Group phase not known
- **WHEN** the group has not loaded
- **THEN** neither card is shown
