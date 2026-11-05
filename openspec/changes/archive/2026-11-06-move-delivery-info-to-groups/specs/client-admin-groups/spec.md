# Spec Delta

## MODIFIED Requirements

### Requirement: Marks on participants
In the participants list of a Draft or Pending group a participant without an account SHALL be followed by a single `*`, and so SHALL, in a Pending group, a participant whose delivery info lacks a name or an address, and the list SHALL end with the legend "* no account or delivery info incomplete", shown only when at least one participant has the mark.

#### Scenario: Participants with marks
- **WHEN** a Draft group has `alice` with an account and `newcomer` without one
- **THEN** `alice` has no mark, `newcomer` is followed by `*` and the legend is shown

#### Scenario: Incomplete profile
- **WHEN** a Pending group has a participant whose delivery info has no address
- **THEN** that participant is followed by `*`, the same as a participant without an account

#### Scenario: Incomplete profile after the draw
- **WHEN** an Active group has a participant whose delivery info has no address
- **THEN** that username is followed by `*` in the pair list

#### Scenario: Nobody marked
- **WHEN** every participant has an account and, in a Pending group, complete delivery info
- **THEN** no `*` and no legend are shown

### Requirement: Marks in the pair list
In the pair list of an Active or Archived group a username whose delivery info lacks a name or an address SHALL be followed by `*`, as described in `client-admin-oversight`.

#### Scenario: Pair list mark
- **WHEN** `bob`'s delivery info has no address and the group is Active
- **THEN** `bob` is followed by `*` in the pair list

