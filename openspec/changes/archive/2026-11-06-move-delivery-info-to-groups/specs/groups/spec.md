# Spec Delta

## MODIFIED Requirements

### Requirement: List and view owned groups
The server SHALL expose `GET /admin/groups` listing every group the admin owns in any state, and `GET /admin/groups/{id}` returning one with all its fields and, per participant, whether an account exists, whether the participant's delivery info for the group is complete (full name and address both present) and the full name in it when there is one.

#### Scenario: Only own groups listed
- **WHEN** admin A lists groups while admin B also owns groups
- **THEN** only A's groups are returned

#### Scenario: Participant flags
- **WHEN** the owner views a Pending group containing `alice` with a full name and address in her delivery info, `bob` without an address, and `carol` who has no account yet
- **THEN** alice is shown as existing and complete, bob as existing and incomplete, carol as not existing and incomplete

#### Scenario: Full names
- **WHEN** the owner views a group containing `alice` whose delivery info has the full name "Alice Brown", `bob` without a full name and `carol` who has no account yet
- **THEN** alice carries the full name "Alice Brown" and bob and carol carry none

#### Scenario: Draft has no delivery info yet
- **WHEN** the owner views a Draft group whose participants have saved nothing for it
- **THEN** no participant carries a full name and the completeness flags are false, because nobody can fill delivery info in a Draft

