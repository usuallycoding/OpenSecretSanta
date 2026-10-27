# Spec Delta

## Purpose

Defines the user record in Open Secret Santa, what users can change about their own account, how temporary passwords behave, and how the first administrator comes into existence.

## ADDED Requirements

### Requirement: User record
A user SHALL have a username, full name, email, phone number, address, delivery information, a password, and an admin flag. Every field except the username, the password and the admin flag SHALL be optional and initially empty.

#### Scenario: New user has only a username
- **WHEN** a user is created from a username alone
- **THEN** full name, email, phone number, address and delivery information are empty and the user is not an admin

#### Scenario: Password never exposed
- **WHEN** any endpoint returns a user
- **THEN** neither the password nor its hash appears in the response

### Requirement: Username format and uniqueness
A username SHALL be stored in lowercase, consist of letters, digits, `.`, `_` and `-`, and be 3 to 32 characters long. Usernames SHALL be unique ignoring case.

#### Scenario: Normalized on creation
- **WHEN** a user is created with the username `Alice_B`
- **THEN** the stored username is `alice_b`

#### Scenario: Invalid username rejected
- **WHEN** a username is shorter than 3 characters, longer than 32, or contains a space or `@`
- **THEN** it is rejected as invalid

#### Scenario: Duplicate ignoring case
- **WHEN** `alice` exists and another user is created as `ALICE`
- **THEN** no second user is created

### Requirement: Username is immutable
The system SHALL NOT allow a user's username to change after creation.

#### Scenario: Username change attempt
- **WHEN** a user submits a profile update containing a different username
- **THEN** the response is 400 and nothing is changed

### Requirement: Read own account
The server SHALL expose `GET /me` returning the caller's username, profile fields, admin flag and whether a password change is required.

#### Scenario: Own account returned
- **WHEN** an authenticated user requests `GET /me`
- **THEN** the response contains their own fields, `isAdmin` and `mustChangePassword`

### Requirement: Edit own profile
The server SHALL expose `PUT /me` letting an authenticated user replace their full name, email, phone number, address and delivery information, any of which may be cleared. A provided email SHALL be a syntactically valid address and each text field SHALL be at most 500 characters.

#### Scenario: Profile updated
- **WHEN** a user submits new values for their editable fields
- **THEN** the response is 200 and a later `GET /me` shows the new values

#### Scenario: Field cleared
- **WHEN** a user submits a null or blank phone number
- **THEN** the phone number becomes empty

#### Scenario: Invalid email
- **WHEN** a user submits an email that is not a valid address
- **THEN** the response is 400 and the profile is unchanged

#### Scenario: Only own profile
- **WHEN** a user calls `PUT /me`
- **THEN** only their own record is modified

### Requirement: Change own password
The server SHALL expose `PUT /me/password` requiring the current password and a new password of at least 8 characters that differs from the current one. On success it SHALL return a fresh access token and invalidate all earlier tokens of that user.

#### Scenario: Successful change
- **WHEN** a user submits the correct current password and a valid new password
- **THEN** the response is 200 with a new token, the old token is rejected, and logging in with the new password works

#### Scenario: Wrong current password
- **WHEN** the submitted current password is wrong
- **THEN** the response is 403 and the password is unchanged

#### Scenario: Weak or unchanged password
- **WHEN** the new password is shorter than 8 characters or equals the current one
- **THEN** the response is 400 and the password is unchanged

#### Scenario: Temporary password replaced
- **WHEN** a user with a temporary password changes it successfully
- **THEN** the temporary password is discarded and `mustChangePassword` becomes false

### Requirement: Temporary passwords are retrievable only while temporary
The system SHALL generate temporary passwords of 12 random characters from an alphabet without look-alike characters, and SHALL keep a temporary password retrievable by authorized admins only until the user sets their own, storing it only in encrypted form.

#### Scenario: Generated format
- **WHEN** a temporary password is generated
- **THEN** it has 12 characters, none of which is `0`, `O`, `1`, `l` or `I`

#### Scenario: Not stored in clear
- **WHEN** the database file is inspected
- **THEN** no temporary password appears in plain text

#### Scenario: Gone after change
- **WHEN** the user has set their own password
- **THEN** no temporary password is retrievable for that user

### Requirement: Default administrator
On startup with no users in the database, the server SHALL create an administrator from `ADMIN_USERNAME` and `ADMIN_PASSWORD` who must change that password at first login. When users already exist, the server SHALL NOT create or alter any user from these variables.

#### Scenario: First boot
- **WHEN** the server starts against an empty database with `ADMIN_USERNAME=boss` and `ADMIN_PASSWORD` set
- **THEN** user `boss` exists as an admin with `mustChangePassword` true, and logging in with that password works

#### Scenario: Later boots ignore the variables
- **WHEN** the server restarts with a different `ADMIN_PASSWORD` after users exist
- **THEN** no user is created or changed

#### Scenario: Password not recoverable via admin views
- **WHEN** an admin views the seeded admin account
- **THEN** no temporary password is shown for it
