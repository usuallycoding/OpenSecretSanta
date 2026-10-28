# Spec Delta

## MODIFIED Requirements

### Requirement: The draw
The detail of an Active or Archived group SHALL show one list of every Santa and receiver pair, loaded with `GET /admin/groups/{id}/assignments`, in the order the server returns them, each row showing the Santa's username, an arrow and the receiver's username. This list SHALL replace the participants list and any separate draw section, and SHALL be shown in the box at the end of the page described by `client-admin-groups`, with its legend inside the box. The administrator's own username SHALL be marked "you" wherever it appears, and a username whose profile is incomplete SHALL be followed by `*` wherever it appears, with a legend "* profile incomplete" under the list. A group that is Draft or Pending SHALL show no pair list and the app SHALL NOT request it.

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
