# Spec Delta

## MODIFIED Requirements

### Requirement: The draw
The detail of an Active or Archived group SHALL show one list of every Santa and receiver pair, loaded with `GET /admin/groups/{id}/assignments`, in the order the server returns them, each row showing the Santa's username, a pair link (the control described by `client-pair-link`) and the receiver's username, the Santa's username at the start of the row and the receiver's username at the end of the row. This list SHALL replace the participants list and any separate draw section, and SHALL be shown in the box at the end of the page described by `client-admin-groups`, with its legend inside the box. The administrator's own username SHALL be marked "you" wherever it appears, and a username whose profile is incomplete SHALL be followed by `*` wherever it appears, with a legend "* profile incomplete" under the list. A group that is Draft or Pending SHALL show no pair list and the app SHALL NOT request it.

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
- **WHEN** `bob` has no address and gives to `carol` and receives from `alice`
- **THEN** `bob` is followed by `*` in both rows and the legend "* profile incomplete" is shown

#### Scenario: Not yet drawn
- **WHEN** the group is Pending
- **THEN** there is no pair list and no assignments request is made

#### Scenario: Draw cannot be loaded
- **WHEN** the assignments request fails while the group detail is shown
- **THEN** the list area shows the problem with "Try again" and the rest of the detail stays

#### Scenario: Long list
- **WHEN** the draw has more pairs than fit in the box
- **THEN** the list scrolls inside the box and the buttons above it stay in view

#### Scenario: Alignment
- **WHEN** an Active group has `alice` giving to `bob`
- **THEN** `alice` is at the start of the row and `bob` is at the end of the row, with the pair link between them

### Requirement: Pair sheet
Choosing the pair link in a row of the pair list SHALL open a bottom sheet titled with the pair, showing the receiver's wishlist, loaded with `GET /admin/groups/{id}/participants/{receiver}/wishlist` and shown as in "Participant wishlist", and below it the thread of questions the Santa asked the receiver. The sheet SHALL offer no way to ask, answer or change anything.

#### Scenario: Wishlist and thread shown
- **WHEN** the owner chooses the pair link in the row of `alice` and `bob`
- **THEN** the sheet shows `bob`'s wishlist and the questions `alice` asked `bob` with his answers

#### Scenario: Read-only
- **WHEN** the pair sheet is shown, whatever the group's state
- **THEN** there is no control that asks, answers or changes a question or a wishlist item
