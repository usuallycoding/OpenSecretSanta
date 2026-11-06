# Spec Delta

## MODIFIED Requirements

### Requirement: The draw
The detail of an Active or Archived group SHALL show one list of every Santa and receiver pair, loaded with `GET /admin/groups/{id}/assignments`, in the order the server returns them, each row showing the Santa's username, a pair link (the control described by `client-pair-link`) and the receiver's username, the Santa's username at the start of the row and the receiver's username at the end of the row.

#### Scenario: Pairs shown
- **WHEN** an Active group has `alice` giving to `bob` and `bob` giving to `carol`
- **THEN** one list shows `alice -> bob` and `bob -> carol` and no separate participants list

#### Scenario: Archived group
- **WHEN** a group is Archived
- **THEN** it shows the same pair list

#### Scenario: Owner's own pair
- **WHEN** the administrator is a participant in the group
- **THEN** their username is marked "you" in the pair where they are the Santa and in the pair where they are the receiver

#### Scenario: Incomplete profile
- **WHEN** `bob`'s delivery info has no address and he gives to `carol` and receives from `alice`
- **THEN** `bob` is followed by the pin icon in both rows and the legend explains it as "delivery info incomplete"

#### Scenario: Not yet drawn
- **WHEN** the group is Pending
- **THEN** there is no pair list and no assignments request is made

#### Scenario: Draw cannot be loaded
- **WHEN** the assignments request fails while the group detail is shown
- **THEN** the list area shows the problem with "Try again" and the rest of the detail stays

#### Scenario: Long list
- **WHEN** the draw has more pairs than fit on the screen
- **THEN** the page grows and scrolls as one, the buttons above the list scroll away with it, and no area scrolls inside the page

#### Scenario: Alignment
- **WHEN** an Active group has `alice` giving to `bob`
- **THEN** `alice` is at the start of the row and `bob` is at the end of the row, with the pair link between them

#### Scenario: Participant count in the label
- **WHEN** an Active group has four participants
- **THEN** the label above the pairs reads "Who gives to whom (4 participants)"

### Requirement: Marks on usernames in the oversight lists
The administrator's own username SHALL be marked "you" wherever it appears. A username with no account SHALL be followed by `*`; one whose account is still on a temporary password by a key icon; one whose delivery info lacks a full name, email, phone or address by a pin icon. Each icon SHALL be named for assistive technology, and a legend under the list SHALL give each mark shown and its meaning, only those present.

#### Scenario: Both marks
- **WHEN** the administrator takes part in the group and their own delivery info is incomplete
- **THEN** their username is marked "you" and followed by the pin icon wherever it appears

#### Scenario: Temporary password
- **WHEN** `bob`'s account is still on a temporary password in a Pending, an Active or an Archived group
- **THEN** `bob` is followed by the key icon wherever he appears and the legend says "temporary password"

#### Scenario: Both icons
- **WHEN** `bob` has a temporary password and incomplete delivery info
- **THEN** `bob` is followed by the key icon and the pin icon, and the legend explains both

#### Scenario: No account
- **WHEN** a Pending group has a participant with no account
- **THEN** that participant is followed by `*` only, with no key and no pin, and the legend says "no account"

#### Scenario: Draft group
- **WHEN** the group is a Draft
- **THEN** no key or pin icon is shown, because no account exists for the group yet and nobody can fill delivery info in

#### Scenario: A receiver's marks on the left
- **WHEN** a pair row or a pair sheet title shows the receiver, who has a temporary password and incomplete delivery info
- **THEN** the receiver's icons are on the left of the username, as in "pin key bob", while the Santa's stay on the right of the username

#### Scenario: Spoken
- **WHEN** a screen reader reads a username followed by the key icon
- **THEN** it reads that the account is on a temporary password

## ADDED Requirements

### Requirement: The marks are counted
Above the list the group page SHALL say how many participants are on a temporary password and how many have incomplete delivery info, such as "2 on a temporary password, 3 with delivery info incomplete", leaving out a count of 0 and showing nothing when both are 0, in the same states as the icons. It SHALL be plain text with no filter and no tap.

#### Scenario: Both counts
- **WHEN** a Pending group has two participants on a temporary password and three with incomplete delivery info
- **THEN** the line reads "2 on a temporary password, 3 with delivery info incomplete"

#### Scenario: One count
- **WHEN** nobody has incomplete delivery info and one person is on a temporary password
- **THEN** the line reads "1 on a temporary password"

#### Scenario: Nothing to count
- **WHEN** nobody is on a temporary password and nobody has incomplete delivery info, or the group is a Draft
- **THEN** no line is shown

#### Scenario: Not a control
- **WHEN** the person taps the line
- **THEN** nothing happens and the list is not filtered

### Requirement: The user sheet names the missing delivery info
When the person has set something for the group but not all of the full name, email, phone and address, the delivery info part of the user sheet SHALL show a line "Missing: " naming the ones not set, as in "Missing: email, phone", besides what is set. When all four are set there SHALL be no such line, and when nothing is set the sheet SHALL only say the person has not added delivery info yet.

#### Scenario: Some missing
- **WHEN** `bob` set a full name and an address but no email or phone
- **THEN** the sheet shows his name and address and the line "Missing: email, phone"

#### Scenario: Only delivery information missing
- **WHEN** `bob` set the four required fields and no delivery information
- **THEN** the sheet shows no "Missing" line

#### Scenario: Only delivery information set
- **WHEN** `bob` set only the delivery information
- **THEN** the sheet shows it and the line "Missing: full name, email, phone, address"

#### Scenario: Nothing set
- **WHEN** `bob` set nothing for the group
- **THEN** the sheet only says he has not added delivery info yet
