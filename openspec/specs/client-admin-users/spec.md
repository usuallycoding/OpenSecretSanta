# client-admin-users Specification

## Purpose

How an administrator looks after the people they manage in the app: listing users, reading their details and temporary passwords, resetting a password, with no way to make anyone an administrator.

## Requirements

### Requirement: Users list
The Users tab SHALL list the users the administrator manages, loaded with `GET /admin/users`: everyone in the administrator's Pending, Active and Archived groups, the people the administrator created who are in no group, and the administrator themselves, each with the username, the full name when there is one, "must change password" for a person who still has a temporary password, and "Not in any group" for a person who can be deleted.

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

#### Scenario: Not in any group
- **WHEN** the administrator created `erin`, who is now in no group
- **THEN** `erin` is listed and marked "Not in any group"

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
Every admin user screen SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached or answers with an error, and SHALL keep what it already shows while a refresh fails. Each screen SHALL be refreshed as described in "Pulling a screen refreshes it" and "The web has no refresh control" and SHALL reload when the app returns to the foreground.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while the list loads
- **THEN** the screen says so and offers "Try again"

#### Scenario: Refresh keeps what is shown
- **WHEN** a refresh fails while the list is shown
- **THEN** the list stays on screen with the error message

### Requirement: Searching users
The Users tab SHALL have a search field above the list, shown once the users have loaded. The list SHALL show only the users whose username or full name matches what is typed, ignoring upper and lower case; with several words, a user SHALL match when every word is found in the username or the full name, in any order. An empty or blank search SHALL show everyone. The field SHALL have a control that empties it while it holds text.

#### Scenario: Match by username
- **WHEN** the administrator types "ali" and manages `alice` and `bob`
- **THEN** only `alice` is listed

#### Scenario: Match by full name
- **WHEN** the administrator types "brown" and `bob`'s full name is "Bob Brown"
- **THEN** `bob` is listed

#### Scenario: Words in any order
- **WHEN** the administrator types "brown bob"
- **THEN** `bob`, whose full name is "Bob Brown", is listed

#### Scenario: Blank search
- **WHEN** the search holds only spaces
- **THEN** every user is listed

#### Scenario: Clearing
- **WHEN** the search field holds text
- **THEN** it has a control that empties it and shows everyone again

### Requirement: No user matches the search
When no user matches, the tab SHALL say so in place of the list and keep the search field.

#### Scenario: No match
- **WHEN** the administrator types a name that nobody managed has
- **THEN** the tab says that no user matches and the search field is still there

### Requirement: Deleting a user
Each row of the Users list for a user whose `canDelete` is true SHALL show a delete icon, named "Delete user" for assistive technology. Choosing it SHALL ask for a confirmation that names the person and says the account cannot be brought back, and confirming SHALL send `DELETE /admin/users/{username}` and, once it is accepted, show the list without that user. A user whose `canDelete` is false SHALL show no such icon, and a user's detail SHALL offer no deletion.

#### Scenario: Delete
- **WHEN** the administrator chooses the delete icon on the row of `erin` and confirms
- **THEN** `erin` is deleted and the list is shown without her

#### Scenario: Cancelled
- **WHEN** the administrator cancels the confirmation
- **THEN** nothing is sent

#### Scenario: Someone in a group
- **WHEN** the list shows `alice`, who is in an Active group
- **THEN** her row has no delete icon

#### Scenario: Detail page
- **WHEN** the administrator opens the detail of `erin`, who can be deleted
- **THEN** the detail shows no deletion action

### Requirement: When a deletion is refused or fails
If the server refuses a deletion because the person has joined a group meanwhile or is no longer managed, the list SHALL say so and reload. If the server cannot be reached or fails, the list SHALL say so and keep the user.

#### Scenario: Joined a group meanwhile
- **WHEN** the server answers that `erin` is now in a group
- **THEN** the list says she can no longer be deleted and reloads without her icon

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while deleting
- **THEN** the list says so and nothing changes
