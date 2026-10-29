# Spec Delta

## MODIFIED Requirements

### Requirement: Read own account
The server SHALL expose `GET /me` returning the caller's username, profile fields, admin flag and whether a password change is required, and whether the server can send email and push notifications.

#### Scenario: Own account returned
- **WHEN** an authenticated user requests `GET /me`
- **THEN** the response contains their own fields, `isAdmin`, `mustChangePassword`, `emailAvailable` and `pushAvailable`

#### Scenario: What the server can send
- **WHEN** the server has an email key configured and no push key
- **THEN** `emailAvailable` is true and `pushAvailable` is false, whoever asks
