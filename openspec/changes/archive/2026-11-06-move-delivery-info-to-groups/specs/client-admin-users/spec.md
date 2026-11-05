# Spec Delta

## ADDED Requirements

### Requirement: The Users tab lists usernames
The Users tab SHALL list the users the administrator manages, loaded with `GET /admin/users`: everyone in the administrator's Pending, Active and Archived groups, the people the administrator created who are in no group, and the administrator themselves, each with the username, "must change password" for a person who still has a temporary password, and "Not in any group" for a person who can be deleted.

#### Scenario: Users shown
- **WHEN** the administrator manages `alice`, who has set her own password, and `dave`, who has not
- **THEN** both are listed and only `dave` is marked "must change password"

#### Scenario: Open a user
- **WHEN** the administrator chooses a user
- **THEN** the user's detail opens, and going back returns to the list

#### Scenario: Only the administrator
- **WHEN** the administrator has no group that left Draft
- **THEN** the list shows only the administrator

#### Scenario: Not in any group
- **WHEN** the administrator created `erin`, who is now in no group
- **THEN** `erin` is listed and marked "Not in any group"

### Requirement: User detail shows the account
Choosing a user on the Users tab SHALL open their detail, and choosing a username in a group's pair list SHALL show the same detail in a bottom sheet. The detail is loaded with `GET /admin/users/{username}` and shows the username and, while it is temporary, the password. It SHALL NOT show a name, email, phone, address or delivery information.

#### Scenario: Temporary password visible
- **WHEN** a managed user has not changed their temporary password
- **THEN** the detail shows it with a note that the person must change it at first sign-in

#### Scenario: Hidden after change
- **WHEN** a managed user has set their own password
- **THEN** the detail shows no password

#### Scenario: No personal details
- **WHEN** a managed user's detail is shown
- **THEN** it has no name, email, phone, address or delivery information

#### Scenario: User out of scope
- **WHEN** the server answers that the user does not exist for this administrator
- **THEN** the screen or sheet says the user is no longer available and offers to go back or close

#### Scenario: Same detail in a sheet
- **WHEN** the owner chooses a username in a group's pair list
- **THEN** a sheet shows the same fields and temporary password rule as the Users tab detail

### Requirement: Searching users by username
The Users tab SHALL have a search field above the list, shown once the users have loaded. The list SHALL show only the users whose username matches what is typed, ignoring upper and lower case; with several words, a user SHALL match when every word is found in the username, in any order. An empty or blank search SHALL show everyone. The field SHALL have a control that empties it while it holds text.

#### Scenario: Match by username
- **WHEN** the administrator types "ali" and manages `alice` and `bob`
- **THEN** only `alice` is listed

#### Scenario: Blank search
- **WHEN** the search holds only spaces
- **THEN** every user is listed

#### Scenario: Clearing
- **WHEN** the search field holds text
- **THEN** it has a control that empties it and shows everyone again

## REMOVED Requirements

### Requirement: Users list
**Reason**: The list no longer shows a full name, which now lives in each group's delivery info.
**Migration**: See "The Users tab lists usernames".

### Requirement: User detail
**Reason**: The detail no longer shows profile fields, which now live in each group's delivery info.
**Migration**: See "User detail shows the account".

### Requirement: Searching users
**Reason**: A user's full name is no longer on the account, so the list is searched by username only.
**Migration**: See "Searching users by username".
