## MODIFIED Requirements

### Requirement: The Receiver page of a drawn group has two tabs
Once the group is Active or Archived the page SHALL be titled "Receiving from Secret Santa" and show a tab row with two tabs, **Your Wishlist** and **Respond to Secret Santa**, and one tab at a time, which the person SHALL be able to change by choosing a tab or by swiping the content sideways, the tab row following the swipe. On a wide window the page SHALL have no tab row and no swiping: it shows both sections, with the Outbox, side by side as `client-layout` describes.

#### Scenario: Title and tabs
- **WHEN** the Receiver page of an Active group is opened
- **THEN** it is titled "Receiving from Secret Santa" with the tabs Your Wishlist and Respond to Secret Santa

#### Scenario: Wide window
- **WHEN** the Receiver page of an Active group is opened on a wide window
- **THEN** there is no tab row, and "Your Wishlist", "Respond to Secret Santa" and the Outbox are shown beside each other

### Requirement: The Gifter page has two tabs
The Gifter page SHALL show a tab row with two tabs, **<receiver username>'s Wishlist** and **Questions for <receiver username>**, and one tab at a time, which the person SHALL be able to change by choosing a tab or by swiping the content sideways, the tab row following the swipe. On a wide window the page SHALL have no tab row and no swiping: it shows both sections, with the Inbox, side by side as `client-layout` describes.

#### Scenario: Tab titles
- **WHEN** the Gifter page for `alice` is opened
- **THEN** its tabs are "alice's Wishlist" and "Questions for alice"

#### Scenario: Wide window
- **WHEN** the Gifter page for `alice` is opened on a wide window
- **THEN** there is no tab row, and "alice's Wishlist", "Questions for alice" and the Inbox are shown beside each other

### Requirement: The Secret Santa card
Below the group's information the group page SHALL show, once the group is Active or Archived, a card headed "You're Secret Santa for:" with, on the next row in a bold title font, the receiver's username in capital letters, and under it the line "Shh... Keep it a secret!" in italics, then "See Wishlist" and "Ask Questions" on one row, then "Inbox" and, last, "Delivery Info", each on its own row. While the group is Pending there SHALL be no such card, as there is no receiver to tell about yet.

#### Scenario: Pending
- **WHEN** the group is Pending
- **THEN** the group page has no "You're Secret Santa for:" card

#### Scenario: The secret line
- **WHEN** the card is shown in an Active or an Archived group
- **THEN** the line "Shh... Keep it a secret!" is under the name, in italics

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
