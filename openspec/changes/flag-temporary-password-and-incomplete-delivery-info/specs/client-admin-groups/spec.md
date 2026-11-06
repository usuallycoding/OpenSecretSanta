# Spec Delta

## MODIFIED Requirements

### Requirement: Marks on participants
In the participants list of a Draft or Pending group a participant without an account SHALL be followed by a single `*`. In a Pending group a participant with an account SHALL also be followed by a key icon while still on a temporary password and by a pin icon while their delivery info lacks a full name, email, phone or address, as `client-admin-oversight` describes. The list SHALL end with a legend of the marks shown, only when at least one is.

#### Scenario: Participants with marks
- **WHEN** a Draft group has `alice` with an account and `newcomer` without one
- **THEN** `alice` has no mark, `newcomer` is followed by `*` and the legend "* no account" is shown

#### Scenario: Incomplete profile
- **WHEN** a Pending group has a participant with an account whose delivery info has no address
- **THEN** that participant is followed by the pin icon, not by `*`, and the legend explains the icon

#### Scenario: Incomplete profile after the draw
- **WHEN** an Active group has a participant whose delivery info has no address
- **THEN** that username is followed by the pin icon in the pair list

#### Scenario: Nobody marked
- **WHEN** every participant has an account, has changed their temporary password and, in a Pending group, has complete delivery info
- **THEN** no mark and no legend are shown

#### Scenario: Temporary password
- **WHEN** a Pending group has a participant who has not changed their temporary password
- **THEN** that participant is followed by the key icon and the legend explains it

### Requirement: Marks in the pair list
In the pair list of an Active or Archived group a username SHALL carry the key icon while its account is on a temporary password and the pin icon while its delivery info lacks a full name, email, phone or address, as described in `client-admin-oversight`.

#### Scenario: Pair list mark
- **WHEN** `bob`'s delivery info has no address and the group is Active
- **THEN** `bob` is followed by the pin icon in the pair list
