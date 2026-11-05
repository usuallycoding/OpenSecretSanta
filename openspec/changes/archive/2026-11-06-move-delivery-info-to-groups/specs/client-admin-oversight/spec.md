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
- **THEN** `bob` is followed by `*` in both rows and the legend "* delivery info incomplete" is shown

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

### Requirement: User sheet
Choosing either username in a row of the pair list, or the username of a participant with an account in a Pending group, SHALL open a bottom sheet with that person's details, loaded with `GET /admin/users/{username}` and shown as the user detail of `client-admin-users`: the username and the temporary password while it is still temporary.

#### Scenario: Open from the Santa
- **WHEN** the owner chooses `alice` in the row `alice -> bob`
- **THEN** a sheet shows `alice`'s details

#### Scenario: Open from the receiver
- **WHEN** the owner chooses `bob` in the row `alice -> bob`
- **THEN** a sheet shows `bob`'s details

#### Scenario: Reset from the sheet
- **WHEN** the owner confirms "Reset password" for `alice` in her sheet
- **THEN** a dialog shows the new temporary password once and the sheet then shows `alice` as having to change it

#### Scenario: Own account
- **WHEN** the owner opens the sheet of their own username
- **THEN** there is no "Reset password"

#### Scenario: Person no longer available
- **WHEN** the server answers that the user does not exist for this administrator
- **THEN** the sheet says the user is no longer available and offers to close

#### Scenario: Open from a Pending participant
- **WHEN** the owner chooses the username of a participant with an account in a Pending group
- **THEN** a sheet shows that person's details

### Requirement: Marks on usernames in the oversight lists
The administrator's own username SHALL be marked "you" wherever it appears, and a username whose delivery info is incomplete SHALL be followed by `*` wherever it appears, with a legend "* delivery info incomplete" under the list.

#### Scenario: Both marks
- **WHEN** the administrator takes part in the group and their own delivery info is incomplete
- **THEN** their username is marked "you" and followed by `*` wherever it appears

