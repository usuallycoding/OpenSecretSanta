# Spec Delta

## REMOVED Requirements

### Requirement: Admin group detail
**Reason**: Its oversight sentence and two scenarios (a participant wishlist link on every drawn group, a Questions screen) no longer apply; the detail is restated below.
**Migration**: See "Group detail shows who takes part".

## MODIFIED Requirements

### Requirement: Marks on participants
In the participants list of a Draft or Pending group a participant without an account SHALL be marked "no account yet" and a participant whose profile lacks a name or an address SHALL be marked "profile incomplete". In the pair list of an Active or Archived group a username whose profile lacks a name or an address SHALL be followed by `*`, as described in `client-admin-oversight`. The marks SHALL NOT block any action.

#### Scenario: Participants with marks
- **WHEN** a Draft group has `alice` with a complete profile and `newcomer` without an account
- **THEN** `alice` has no mark and `newcomer` is marked "no account yet"

#### Scenario: Incomplete profile
- **WHEN** a participant's account has no address
- **THEN** that participant is marked "profile incomplete"

#### Scenario: Incomplete profile after the draw
- **WHEN** an Active group has a participant whose account has no address
- **THEN** that username is followed by `*` in the pair list

## ADDED Requirements

### Requirement: Group detail shows who takes part
Choosing a group SHALL open its detail, loaded with `GET /admin/groups/{id}`, showing its details, its state with a sentence on what that state means for the owner, and who takes part. For a Draft or Pending group that is the participants list; for an Active or Archived group it is the pair list of `client-admin-oversight`, which replaces the participants list. In a Pending group each participant SHALL offer "Read wishlist".

#### Scenario: Group not available
- **WHEN** the server answers that the group does not exist for this administrator
- **THEN** the screen says the group is no longer available and offers to go back

#### Scenario: Pending group
- **WHEN** a Pending group's detail is shown
- **THEN** it lists the participants and each offers "Read wishlist"

#### Scenario: Draft offers no oversight
- **WHEN** the group is Draft
- **THEN** it lists the participants with no "Read wishlist" and no pair list

#### Scenario: Drawn group
- **WHEN** the group is Active or Archived
- **THEN** the detail shows the pair list and no separate participants list, and no Questions button
