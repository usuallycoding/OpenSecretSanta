# user-administration Specification

## Purpose

Defines what administrators can do with other users' accounts in Open Secret Santa: which users they may manage, how they see and reset temporary credentials, and how missing users are created in bulk for groups.

## Requirements

### Requirement: Admin-only endpoints
Every `/admin/...` endpoint SHALL be available only to admins; any other authenticated user SHALL receive HTTP 403.

#### Scenario: Normal user refused
- **WHEN** a non-admin user calls an `/admin/...` endpoint
- **THEN** the response is 403

### Requirement: Managed-user scope
An admin SHALL be able to view and reset only users who participate in a group that admin owns and that is not in Draft, the non-administrator users that admin created who are in no group at all (of any owner, in any state), and themselves. Requests about any other user SHALL be answered as if the user did not exist.

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
- **WHEN** `alice`, whom another admin created, is removed from the only non-Draft group of admin A that contained her
- **THEN** admin A can no longer view or reset her

#### Scenario: Own user in no group
- **WHEN** admin A created `erin` by naming her in a Pending group and then removed her, and she is in no group
- **THEN** admin A can view and reset `erin`

#### Scenario: Another admin's user in no group
- **WHEN** `erin` was created by admin B and is in no group
- **THEN** admin A requesting `erin` gets 404

#### Scenario: Own user who joined a group
- **WHEN** `erin` was created by admin A and now only belongs to a Draft group of admin B
- **THEN** admin A requesting `erin` gets 404

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
The system SHALL record which admin owns each group and which usernames participate in it, including usernames that have no account yet while the group is Draft, so that managed-user scope can be determined. A user MAY participate in any number of groups.

#### Scenario: Multiple memberships
- **WHEN** a user is added to two groups
- **THEN** the user is a participant of both and in scope for the owners of both once those groups are not Draft

#### Scenario: Participant without an account
- **WHEN** a Draft group names a username that has no account
- **THEN** the name is kept in the group's participant list without creating any account

### Requirement: Who created an account
The server SHALL record the administrator whose action created an account: adding a username that has no account to a Pending group, or the bulk creation of missing users. The first administrator and accounts created before this was recorded SHALL have no creator, except that when exactly one administrator exists the migration that adds the record SHALL give every existing non-administrator account to that administrator.

#### Scenario: Created by adding to a Pending group
- **WHEN** admin A moves a group naming `newcomer`, who has no account, to Pending
- **THEN** `newcomer`'s account has admin A as its creator

#### Scenario: First administrator
- **WHEN** the first administrator is created at start-up
- **THEN** the account has no creator

#### Scenario: Single administrator migration
- **WHEN** the migration runs on a database with one administrator and three other accounts
- **THEN** each of the three has that administrator as its creator

#### Scenario: Several administrators migration
- **WHEN** the migration runs on a database with two administrators
- **THEN** no existing account is given a creator

### Requirement: Deleting an unassigned user
The server SHALL expose `DELETE /admin/users/{username}`, answering 204 and deleting the account, only for a user the caller created who is not an administrator, is not the caller and is in no group of any owner in any state. A user outside the caller's scope SHALL be answered as not found; an administrator, the caller, or a user in any group SHALL be refused with 409 and nothing SHALL be deleted.

#### Scenario: Delete
- **WHEN** admin A deletes `erin`, whom A created and who is in no group
- **THEN** the answer is 204 and `erin` can no longer sign in or be listed

#### Scenario: In a group
- **WHEN** `erin` joined an Active group of admin B since A last looked and A deletes her
- **THEN** the answer is 409 and `erin` still exists

#### Scenario: Administrator
- **WHEN** an admin tries to delete another administrator, or themselves
- **THEN** the answer is 409 and nothing is deleted

#### Scenario: Outside the scope
- **WHEN** admin A deletes `erin`, whom admin B created and who is in no group
- **THEN** the answer is 404 and `erin` still exists

### Requirement: What deleting a user removes
Deleting a user SHALL remove the account together with its push devices, its notification settings and its login lockout record. After it the user's tokens SHALL no longer be accepted and the username SHALL be free to use again.

#### Scenario: Devices and settings
- **WHEN** a deleted user had two registered devices and notification settings
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

### Requirement: Users say whether they can be deleted
`GET /admin/users` and `GET /admin/users/{username}` SHALL give each user a `canDelete` flag that is true exactly when `DELETE /admin/users/{username}` would be accepted for the caller.

#### Scenario: Deletable
- **WHEN** admin A lists users and `erin` is one A created who is in no group
- **THEN** `erin` has `canDelete` true

#### Scenario: Not deletable
- **WHEN** admin A lists users including `alice`, who is in A's Active group, and A themselves
- **THEN** both have `canDelete` false
