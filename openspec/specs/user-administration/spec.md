# user-administration Specification

## Purpose

Defines what administrators can do with other users' accounts in Open Secret Santa: which users they may manage, how they see and reset temporary credentials, how admins are promoted, and how missing users are created in bulk for groups.

## Requirements

### Requirement: Admin-only endpoints
Every `/admin/...` endpoint SHALL be available only to admins; any other authenticated user SHALL receive HTTP 403.

#### Scenario: Normal user refused
- **WHEN** a non-admin user calls an `/admin/...` endpoint
- **THEN** the response is 403

### Requirement: Managed-user scope
An admin SHALL be able to view and reset only users who are members of a group that admin owns, plus themselves. Requests about any other user SHALL be answered as if the user did not exist.

#### Scenario: Member of own group
- **WHEN** admin A owns a group containing `alice` and requests `alice`
- **THEN** the request is processed

#### Scenario: Member of another admin's group only
- **WHEN** `bob` is only in a group owned by admin B and admin A requests `bob`
- **THEN** the response is 404

#### Scenario: Shared user
- **WHEN** `alice` is in groups owned by both admin A and admin B
- **THEN** both admins can view and reset `alice`

#### Scenario: Self
- **WHEN** an admin requests their own username
- **THEN** the request is processed

### Requirement: List and view managed users
The server SHALL expose `GET /admin/users` listing the admin's managed users and `GET /admin/users/{username}` returning one with all profile fields, admin flag, `mustChangePassword` and, only while the user's password is temporary, the current temporary password.

#### Scenario: Temporary password visible
- **WHEN** an admin views a managed user who has not yet changed their temporary password
- **THEN** the response includes that temporary password in clear text

#### Scenario: Hidden after change
- **WHEN** an admin views a managed user who has set their own password
- **THEN** the response contains no temporary password

#### Scenario: List is scoped
- **WHEN** an admin lists users
- **THEN** only users within their managed scope are returned

### Requirement: Reset a user's password
The server SHALL expose `POST /admin/users/{username}/reset-password` that generates a new temporary password for a managed user, requires them to change it at next login, invalidates their existing tokens, clears any login lockout, and returns the new temporary password.

#### Scenario: Reset succeeds
- **WHEN** an admin resets a managed user's password
- **THEN** the response contains a new temporary password, the old password and tokens stop working, and the user must change the new one at next login

#### Scenario: Out of scope
- **WHEN** an admin resets a user outside their scope
- **THEN** the response is 404 and nothing changes

### Requirement: Promote a user to admin
The server SHALL expose `POST /admin/users/{username}/promote` that makes any existing user an admin, regardless of group membership. Promoting an existing admin SHALL succeed without change. No endpoint SHALL remove admin rights.

#### Scenario: Promotion
- **WHEN** an admin promotes an existing normal user who is in none of their groups
- **THEN** the response is 200 and the user is an admin

#### Scenario: Already admin
- **WHEN** an admin promotes a user who is already an admin
- **THEN** the response is 200 and nothing changes

#### Scenario: Unknown user
- **WHEN** an admin promotes a username that does not exist
- **THEN** the response is 404

### Requirement: Bulk creation of missing users
The system SHALL provide an all-or-nothing operation, for use by group management, that takes a list of usernames and creates each missing user with a generated temporary password while leaving existing users untouched.

#### Scenario: Mixed list
- **WHEN** the list contains `alice` (existing) and `bob` (missing)
- **THEN** `bob` is created with a temporary password and `alice` is unchanged

#### Scenario: Invalid entry rejects batch
- **WHEN** the list contains an invalid username
- **THEN** no user from the list is created

#### Scenario: Duplicates in list
- **WHEN** the list contains the same username twice, differing only in case
- **THEN** that user is created once

### Requirement: Group ownership and membership records
The system SHALL record which admin owns each group and which users belong to it, so that managed-user scope can be determined. A user MAY belong to any number of groups.

#### Scenario: Multiple memberships
- **WHEN** a user is added to two groups
- **THEN** the user is a member of both and in scope for the owners of both
