# client-admin-users Specification

## Purpose

TBD - created by archiving change admin-users. Update Purpose after archive.

## Requirements

### Requirement: Users list
The Users tab SHALL list the users the administrator manages, loaded with `GET /admin/users`: everyone in the administrator's Pending, Active and Archived groups and the administrator themselves, each with the username, the full name when there is one, and "must change password" for a person who still has a temporary password.

#### Scenario: Users shown
- **WHEN** the administrator manages `alice`, who has set her own password, and `dave`, who has not
- **THEN** both are listed and only `dave` is marked "must change password"

#### Scenario: Full name
- **WHEN** a managed user has a full name
- **THEN** it is shown beside the username

#### Scenario: Open a user
- **WHEN** the administrator chooses a user
- **THEN** the user's detail opens, and going back returns to the list

#### Scenario: Only the administrator
- **WHEN** the administrator has no group that left Draft
- **THEN** the list shows only the administrator

### Requirement: User detail
Choosing a user on the Users tab SHALL open their detail, and choosing a username in a group's pair list SHALL show the same detail in a bottom sheet. The detail is loaded with `GET /admin/users/{username}`, showing the username and each of full name, email, phone, address and delivery information that is set, leaving out the others. For a person who still has a temporary password the detail SHALL also show that password, selectable so it can be copied; for a person who has set their own password it SHALL show none.

#### Scenario: Profile fields
- **WHEN** a user has a full name and an address but no phone
- **THEN** the detail shows those two and nothing for the phone

#### Scenario: Temporary password visible
- **WHEN** a managed user has not changed their temporary password
- **THEN** the detail shows it with a note that the person must change it at first sign-in

#### Scenario: Hidden after change
- **WHEN** a managed user has set their own password
- **THEN** the detail shows no password

#### Scenario: User out of scope
- **WHEN** the server answers that the user does not exist for this administrator
- **THEN** the screen or sheet says the user is no longer available and offers to go back or close

#### Scenario: Same detail in a sheet
- **WHEN** the owner chooses a username in a group's pair list
- **THEN** a sheet shows the same fields and temporary password rule as the Users tab detail

### Requirement: Resetting a password
The detail of a managed user SHALL offer "Reset password", which after a confirmation that says the old password and the person's sessions stop working sends `POST /admin/users/{username}/reset-password` and shows the new temporary password once in a dialog, selectable so it can be copied, with the sentence that the person must change it when they next sign in.

#### Scenario: Reset
- **WHEN** the administrator confirms "Reset password" for `alice`
- **THEN** a dialog shows the new temporary password and the detail now shows `alice` as having to change it

#### Scenario: Cancelled
- **WHEN** the administrator cancels the confirmation
- **THEN** nothing is sent

#### Scenario: User left the scope meanwhile
- **WHEN** the server refuses the reset because the user is no longer managed by this administrator
- **THEN** the screen says the user is no longer available and offers to go back

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while resetting
- **THEN** the screen says so and nothing changes

### Requirement: No reset of one's own password
The administrator's own detail SHALL offer no "Reset password".

#### Scenario: Own account
- **WHEN** the administrator opens their own detail
- **THEN** there is no "Reset password"

### Requirement: No way to make an administrator
No screen of the app SHALL offer to make a user an administrator.

#### Scenario: User detail
- **WHEN** the administrator opens any user's detail
- **THEN** there is no action that grants administrator rights

### Requirement: Loading, errors and refreshing
Every admin user screen SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached or answers with an error, and SHALL keep what it already shows while a refresh fails. Each screen SHALL offer Refresh and SHALL reload when the app returns to the foreground.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while the list loads
- **THEN** the screen says so and offers "Try again"

#### Scenario: Refresh keeps what is shown
- **WHEN** a refresh fails while the list is shown
- **THEN** the list stays on screen with the error message
