# Spec Delta

## MODIFIED Requirements

### Requirement: User record
A user SHALL have a username, a password and an admin flag. Their name, email, phone, address and delivery information SHALL NOT be on the account: they are kept per group, as `delivery-info` describes.

#### Scenario: New user has only a username
- **WHEN** a user is created from a username alone
- **THEN** the user has no password of their choosing yet and is not an admin

#### Scenario: Password never exposed
- **WHEN** any endpoint returns a user
- **THEN** neither the password nor its hash appears in the response

### Requirement: Username is immutable
The system SHALL NOT allow a user's username to change after creation.

#### Scenario: Username change attempt
- **WHEN** a user calls `PUT /me` with a different username
- **THEN** the response is 405, because no route changes a username, and nothing is changed

### Requirement: Read own account
The server SHALL expose `GET /me` returning the caller's username, admin flag and whether a password change is required, and whether the server can send email and push notifications.

#### Scenario: Own account returned
- **WHEN** an authenticated user requests `GET /me`
- **THEN** the response contains their username, `isAdmin`, `mustChangePassword`, `emailAvailable` and `pushAvailable`, and no name, email, phone, address or delivery information

#### Scenario: What the server can send
- **WHEN** the server has an email key configured and no push key
- **THEN** `emailAvailable` is true and `pushAvailable` is false, whoever asks

## REMOVED Requirements

### Requirement: Edit own profile
**Reason**: The profile moved from the account to each group.
**Migration**: Use `PUT /groups/{id}/delivery-info`, as `delivery-info` describes.
