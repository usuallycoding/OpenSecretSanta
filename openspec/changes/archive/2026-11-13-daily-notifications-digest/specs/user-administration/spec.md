# Spec Delta

## MODIFIED Requirements

### Requirement: What deleting a user removes
Deleting a user SHALL remove the account together with its push devices, its notification settings, what is waiting for its daily digest and its login lockout record. After it the user's tokens SHALL no longer be accepted and the username SHALL be free to use again.

#### Scenario: Devices and settings
- **WHEN** a deleted user had two registered devices, notification settings and a mark for the daily digest
- **THEN** none of them remain

#### Scenario: Lockout record
- **WHEN** a deleted user had failed logins on record
- **THEN** the record is gone

#### Scenario: Signed-in user
- **WHEN** `erin` is signed in and is deleted
- **THEN** her next request is refused as unauthenticated

#### Scenario: Name freed
- **WHEN** `erin` was deleted and a group is then given the username `erin`
- **THEN** a new account is created for it
