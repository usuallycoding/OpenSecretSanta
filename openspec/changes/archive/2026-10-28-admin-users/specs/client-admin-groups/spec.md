# Spec Delta

## MODIFIED Requirements

### Requirement: Temporary passwords are shown once
When making a group Pending, or saving a Pending group's participants, creates accounts, the app SHALL show each created username with its temporary password in a dialog, selectable so that it can be copied, and SHALL say that each person must change it when first signing in and that an administrator can look it up under Users until then. The dialog SHALL be shown only when accounts were created. Once it is closed the app SHALL keep none of the passwords.

#### Scenario: Accounts created
- **WHEN** making a group Pending creates accounts for `bob` and `carol`
- **THEN** a dialog lists `bob` and `carol` with their temporary passwords

#### Scenario: No accounts created
- **WHEN** every participant already has an account
- **THEN** no dialog is shown

#### Scenario: Closing the dialog
- **WHEN** the dialog is closed
- **THEN** the passwords are no longer anywhere in the app
