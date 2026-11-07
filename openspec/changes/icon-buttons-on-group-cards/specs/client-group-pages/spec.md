## ADDED Requirements

### Requirement: The card actions are icon buttons
The actions of the "You're Secret Santa for:" and "Your stuff:" cards, except "Reveal Santa", SHALL be outlined icon buttons with no text, in one row at the start of the card. The icons SHALL be the gift for "See Wishlist" and "Edit Wishlist", the Material "Send" paper plane for "Ask Questions" and "Respond to Santa", the mail icon for "Inbox" and "Send letter" and the outlined map pin for both "Delivery Info" buttons, in that order. "Reveal Santa" SHALL stay a text button.

#### Scenario: Secret Santa card icons
- **WHEN** the group is Active
- **THEN** the "You're Secret Santa for:" card has one row of four outlined icon buttons, gift, send, mail and pin, with no text on them

#### Scenario: Your stuff card icons
- **WHEN** the group is Active
- **THEN** the "Your stuff:" card has one row of four outlined icon buttons, gift (Edit Wishlist), send (Respond to Santa), mail (Send letter) and pin (Delivery Info), and no text on them

#### Scenario: Before the draw
- **WHEN** the group is Pending
- **THEN** the "Your stuff:" card has only the gift and the pin buttons

#### Scenario: Reveal Santa
- **WHEN** the group is Archived
- **THEN** "Reveal Santa" is still a text button, above the row of icon buttons in the "Your stuff:" card

### Requirement: Card icon buttons keep their names, badges and states
Each card icon button's former label SHALL be its spoken name and its tooltip, shown on hover and on a long press, so the requirements that name these buttons still describe them. The count badges of `client-read-marks` and `Respond to Santa counts unanswered questions` SHALL stay on the corner of their buttons with the count still spoken with the name, and a button SHALL be disabled where it was.

#### Scenario: A tooltip
- **WHEN** the person hovers a card icon button, or presses it for a long time on a touch screen
- **THEN** a tooltip shows its name, such as "Ask Questions", and a short press still opens what it opens

#### Scenario: Spoken name
- **WHEN** a screen reader reads a card icon button
- **THEN** it reads the name, such as "Inbox", and with new items "Inbox, 3 new"

#### Scenario: Badges stay
- **WHEN** three new letters wait for the Santa
- **THEN** the mail button of the "You're Secret Santa for:" card has a badge showing 3 on its corner

#### Scenario: Receiver not known
- **WHEN** the receiver has not loaded
- **THEN** the four icon buttons of the "You're Secret Santa for:" card are disabled

## MODIFIED Requirements

### Requirement: The Secret Santa card
Below the group's information the group page SHALL show, once the group is Active or Archived, a card headed "You're Secret Santa for:" with, on the next row in a bold title font, the receiver's username in capital letters, and under it the line "Shh... Keep it a secret!" in italics, then one row of four icon buttons, as `The card actions are icon buttons` describes: "See Wishlist", "Ask Questions", "Inbox" and, last, "Delivery Info". While the group is Pending there SHALL be no such card, as there is no receiver to tell about yet.

#### Scenario: Pending
- **WHEN** the group is Pending
- **THEN** the group page has no "You're Secret Santa for:" card

#### Scenario: The secret line
- **WHEN** the card is shown in an Active or an Archived group
- **THEN** the line "Shh... Keep it a secret!" is under the name, in italics

#### Scenario: Active
- **WHEN** the group is Active and the person's receiver is `bob`
- **THEN** the card shows the heading, "BOB", the line "Shh... Keep it a secret!" and one row of four icon buttons: "See Wishlist", "Ask Questions", "Inbox" and, last, "Delivery Info"

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
Below the Secret Santa card the group page SHALL show a card headed "Your stuff:", the line "Tell your santa what you want and let them know you!" in italics under it, one row of icon buttons, as `The card actions are icon buttons` describes: "Edit Wishlist" and, from Active on, "Respond to Santa" and "Send letter" and, last, "Delivery Info". In an Archived group it SHALL also have, above that row, the "Reveal Santa" text button of `The Santa stays hidden until the group is archived`.

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

### Requirement: The Your stuff card asks for missing delivery info
In a Pending or an Active group, when the person's delivery info for the group lacks the full name, email, phone or address, the "Your stuff:" card SHALL show the text "Please fill out your delivery info in order to be able to receive your secret gift!" in the theme's error colour, directly above the row of icon buttons, below "Reveal Santa" if that is shown, and while it is shown the "Delivery Info" icon button SHALL be drawn in the error colour too, its outline and its icon. The message SHALL be plain text, not a control, and nothing here SHALL block any action.

#### Scenario: Incomplete in a Pending group
- **WHEN** the group is Pending and the person has not set their phone
- **THEN** the message is shown directly above the row of icon buttons, and the "Delivery Info" icon button is red

#### Scenario: Incomplete in an Active group
- **WHEN** the group is Active and the person has set nothing
- **THEN** the message is shown directly above the row of icon buttons, below "Reveal Santa" if that is shown

#### Scenario: Complete
- **WHEN** the person has set their full name, email, phone and address
- **THEN** no message is shown and the "Delivery Info" icon button is drawn as the others are

#### Scenario: Only the delivery information empty
- **WHEN** the person has set the four required fields and left the delivery information empty
- **THEN** no message is shown

#### Scenario: Archived group
- **WHEN** the group is Archived and the person's delivery info is incomplete
- **THEN** no message is shown, because nothing can be changed any more

#### Scenario: Wording
- **WHEN** the message is shown
- **THEN** it reads exactly "Please fill out your delivery info in order to be able to receive your secret gift!" in the error colour, and a screen reader reads it as text

#### Scenario: Nothing is blocked
- **WHEN** the message is shown
- **THEN** every button of the card can still be used
