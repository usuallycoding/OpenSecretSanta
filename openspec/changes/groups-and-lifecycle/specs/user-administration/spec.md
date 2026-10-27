# Spec Delta

## MODIFIED Requirements

### Requirement: Managed-user scope
An admin SHALL be able to view and reset only users who participate in a group that admin owns and that is not in Draft, plus themselves. Requests about any other user SHALL be answered as if the user did not exist.

#### Scenario: Member of own group
- **WHEN** admin A owns a Pending, Active or Archived group containing `alice` and requests `alice`
- **THEN** the request is processed

#### Scenario: Member of another admin's group only
- **WHEN** `bob` is only in a group owned by admin B and admin A requests `bob`
- **THEN** the response is 404

#### Scenario: Shared user
- **WHEN** `alice` is in non-Draft groups owned by both admin A and admin B
- **THEN** both admins can view and reset `alice`

#### Scenario: Self
- **WHEN** an admin requests their own username
- **THEN** the request is processed

#### Scenario: Named only in a Draft
- **WHEN** admin A names the existing user `carol` in a Draft group and requests `carol`
- **THEN** the response is 404, until the group leaves Draft

#### Scenario: Removed from a group
- **WHEN** `alice` is removed from the only non-Draft group of admin A that contained her
- **THEN** admin A can no longer view or reset her

### Requirement: Group ownership and membership records
The system SHALL record which admin owns each group and which usernames participate in it, including usernames that have no account yet while the group is Draft, so that managed-user scope can be determined. A user MAY participate in any number of groups.

#### Scenario: Multiple memberships
- **WHEN** a user is added to two groups
- **THEN** the user is a participant of both and in scope for the owners of both once those groups are not Draft

#### Scenario: Participant without an account
- **WHEN** a Draft group names a username that has no account
- **THEN** the name is kept in the group's participant list without creating any account
