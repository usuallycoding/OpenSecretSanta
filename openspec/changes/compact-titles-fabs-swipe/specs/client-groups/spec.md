# Spec Delta

## MODIFIED Requirements

### Requirement: Groups tab
The participant view SHALL have a Groups tab that lists the groups the signed-in person participates in, as returned by `GET /groups`, in the order the server returns them. Each entry SHALL show the group's name on one row, with an ellipsis when it is longer, and its state and, when present, its date and time and its location, and the number of participants. The name SHALL NOT open anything by itself: choosing the entry opens the group.

#### Scenario: Listed groups
- **WHEN** the person opens the Groups tab and is in two groups
- **THEN** both are listed with their name, state, date, time, location and number of participants

#### Scenario: Missing details are left out
- **WHEN** a group has no date, time or location
- **THEN** the entry shows only what the group has

#### Scenario: No groups
- **WHEN** the person is in no group
- **THEN** the tab explains that an organiser adds people to groups by username instead of showing an empty list

#### Scenario: Draft groups
- **WHEN** an organiser has named the person only in a group that is still a Draft
- **THEN** that group is not shown, because the server does not return it

#### Scenario: A long name
- **WHEN** a group's name is longer than one row
- **THEN** the entry shows one row with an ellipsis, and choosing it opens the group

### Requirement: Group page
Choosing a group SHALL open its page, loaded with `GET /groups/{id}`, showing its name on one row with an ellipsis when it is longer, then its state and state note, description, date, time and location, then the pair rows of `client-group-pages`, and at the end the list of every participant's username with the person's own marked. Choosing a name that was cut SHALL open a dialog with the whole name and the copy icon. The participants list SHALL be a box that scrolls inside itself and fills all the height left below the details and the pair rows, with a minimum height; when the details leave less room than that, they SHALL scroll above it. Parts the group does not have SHALL be left out.

#### Scenario: Opening a group
- **WHEN** the person chooses a group in the list
- **THEN** the page shows its details, the pair rows and, at the end, every participant's username with their own marked "you"

#### Scenario: Back to the list
- **WHEN** the person goes back from the group page
- **THEN** the list is shown again

#### Scenario: Group no longer available
- **WHEN** the server answers that the group does not exist, for example because the person was removed from it
- **THEN** the page says the group is no longer available and offers to go back

#### Scenario: Many participants
- **WHEN** the group has more participants than fit in the list
- **THEN** the list fills the rest of the page and scrolls inside itself, and the rest of the page does not grow

#### Scenario: Few participants
- **WHEN** the group has four participants on a tall screen
- **THEN** the box still fills the rest of the page, with the names at its top

#### Scenario: A long name
- **WHEN** the group's name is longer than one row and the person chooses it
- **THEN** a dialog shows the whole name and can be closed
