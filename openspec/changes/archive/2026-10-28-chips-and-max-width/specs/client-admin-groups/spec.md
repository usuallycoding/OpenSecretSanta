## MODIFIED Requirements

### Requirement: Group detail shows who takes part
Choosing a group SHALL open its detail, loaded with `GET /admin/groups/{id}`, showing its name on one row with an ellipsis when it is longer (choosing a name that was cut opens a dialog with the whole name), its details, its state with a sentence on what that state means for the owner, the actions of the group, and at the end who takes part. For a Draft group that is the participants as chips; for a Pending group it is the participants as rows; for an Active or Archived group it is the pair list of `client-admin-oversight`, which replaces the participants list. In a Pending group choosing a participant's username SHALL open that person's user sheet of `client-admin-oversight`, and each row SHALL end with a gift icon, named "Read wishlist" for assistive technology, that opens the participant's wishlist. The people SHALL be shown in a box at the end of the page that scrolls inside itself and fills all the height left below the details and the actions, with a minimum height; when the details and actions leave less room than that, they SHALL scroll above it. The chips of a Draft group SHALL wrap onto as many lines as needed and SHALL NOT be tappable.

#### Scenario: Group not available
- **WHEN** the server answers that the group does not exist for this administrator
- **THEN** the screen says the group is no longer available and offers to go back

#### Scenario: Pending group
- **WHEN** a Pending group's detail is shown
- **THEN** it lists the participants as rows, each username opens the user sheet and each row ends with a gift icon that opens the wishlist

#### Scenario: Draft offers no oversight
- **WHEN** the group is Draft
- **THEN** it lists the participants as chips with no gift icon and no pair list, and choosing a chip does nothing

#### Scenario: Drawn group
- **WHEN** the group is Active or Archived
- **THEN** the detail shows the pair list and no separate participants list, and no Questions button

#### Scenario: Many participants
- **WHEN** a Pending group has 13 participants
- **THEN** the participants are in a box that fills the rest of the page and scrolls inside itself, and the buttons above it stay in view

#### Scenario: A long name
- **WHEN** the group's name is longer than one row and the administrator chooses it
- **THEN** a dialog shows the whole name and can be closed

#### Scenario: Participant without an account in a Pending group
- **WHEN** a Pending group has a participant who has no account
- **THEN** their row shows the username and `*`, and choosing the username or the gift icon does nothing

### Requirement: Marks on participants
In the participants list of a Draft or Pending group a participant without an account, and a participant whose profile lacks a name or an address, SHALL each be followed by a single `*`, the same mark for both, and the list SHALL end with the legend "* no account or profile incomplete", shown only when at least one participant has the mark. In the pair list of an Active or Archived group a username whose profile lacks a name or an address SHALL be followed by `*`, as described in `client-admin-oversight`. The marks SHALL NOT block any action.

#### Scenario: Participants with marks
- **WHEN** a Draft group has `alice` with a complete profile and `newcomer` without an account
- **THEN** `alice` has no mark, `newcomer` is followed by `*` and the legend is shown

#### Scenario: Incomplete profile
- **WHEN** a participant's account has no address
- **THEN** that participant is followed by `*`, the same as a participant without an account

#### Scenario: Incomplete profile after the draw
- **WHEN** an Active group has a participant whose account has no address
- **THEN** that username is followed by `*` in the pair list

#### Scenario: Nobody marked
- **WHEN** every participant has an account and a complete profile
- **THEN** no `*` and no legend are shown
