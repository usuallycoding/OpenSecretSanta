# Spec Delta

## MODIFIED Requirements

### Requirement: List and view owned groups
The server SHALL expose `GET /admin/groups` listing every group the admin owns in any state, and `GET /admin/groups/{id}` returning one with all its fields and, per participant, whether an account exists, whether the participant's delivery info for the group is complete (full name, email, phone and address all present) and the full name in it when there is one.

#### Scenario: Only own groups listed
- **WHEN** admin A lists groups while admin B also owns groups
- **THEN** only A's groups are returned

#### Scenario: Participant flags
- **WHEN** the owner views a Pending group containing `alice` with a full name, email, phone and address in her delivery info, `bob` without a phone, and `carol` who has no account yet
- **THEN** alice is shown as existing and complete, bob as existing and incomplete, carol as not existing and incomplete

#### Scenario: Full names
- **WHEN** the owner views a group containing `alice` whose delivery info has the full name "Alice Brown", `bob` without a full name and `carol` who has no account yet
- **THEN** alice carries the full name "Alice Brown" and bob and carol carry none

#### Scenario: Draft has no delivery info yet
- **WHEN** the owner views a Draft group whose participants have saved nothing for it
- **THEN** no participant carries a full name and the completeness flags are false, because nobody can fill delivery info in a Draft

## ADDED Requirements

### Requirement: The owner sees who still has a temporary password
`GET /admin/groups` and `GET /admin/groups/{id}` SHALL carry, per participant, `mustChangePassword`: true when the participant has an account that still has a temporary password, false when the account's password was changed or when there is no account. It SHALL be only a flag; no password is revealed by it.

#### Scenario: Still temporary
- **WHEN** the owner views a Pending group with `bob`, whose account was created with the group and who has not signed in
- **THEN** `bob` carries `mustChangePassword` true

#### Scenario: Changed
- **WHEN** `bob` changes his temporary password and the owner views the group again
- **THEN** `bob` carries `mustChangePassword` false

#### Scenario: No account
- **WHEN** the owner views a Draft group whose participants have no account yet
- **THEN** every participant carries `mustChangePassword` false

#### Scenario: Only the owner
- **WHEN** an admin who does not own the group requests the group
- **THEN** the response is 404 and carries no flag
