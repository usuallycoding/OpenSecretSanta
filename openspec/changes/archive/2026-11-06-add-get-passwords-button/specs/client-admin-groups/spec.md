# Spec Delta

## ADDED Requirements

### Requirement: Get passwords button
The administrator's page for a Pending or Active group SHALL show a "Get passwords" button after "Send announcement". Choosing it SHALL load the managed users with `GET /admin/users` and open a dialog listing each participant of that group who still has a temporary password, as `username: password`, selectable so that it can be copied. A Draft or Archived group SHALL NOT have the button.

#### Scenario: Pending group with new accounts
- **WHEN** a Pending group has `bob` and `carol` who have not changed their temporary passwords, and the administrator chooses "Get passwords"
- **THEN** a dialog lists `bob` and `carol` with their temporary passwords

#### Scenario: Only those who still have one
- **WHEN** `alice` has set her own password and `dave` has not
- **THEN** the dialog lists `dave` and not `alice`

#### Scenario: Only this group
- **WHEN** another group of the administrator has a person with a temporary password who is not in this group
- **THEN** that person is not listed

#### Scenario: Button after Send announcement
- **WHEN** the page of a Pending or Active group shows its buttons
- **THEN** "Get passwords" comes directly after "Send announcement"

#### Scenario: Other states
- **WHEN** the group is a Draft or Archived
- **THEN** there is no "Get passwords" button

### Requirement: The passwords dialog on demand
The dialog opened by "Get passwords" SHALL say that the listed people have not changed their password yet and must change it when they first sign in. When nobody in the group has a temporary password it SHALL say so and list nobody. Closing it SHALL leave none of the passwords in the app. The button SHALL be unavailable while the passwords load, and a failure to load SHALL be said with the page's usual notice and open no dialog.

#### Scenario: Nobody has one
- **WHEN** every participant has set their own password
- **THEN** the dialog says that nobody in the group has a temporary password

#### Scenario: Closing
- **WHEN** the dialog is closed
- **THEN** the passwords are no longer anywhere in the app

#### Scenario: Loading
- **WHEN** the passwords are being loaded
- **THEN** the button cannot be chosen again until they arrive

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached
- **THEN** the page says so with its notice and no dialog opens
