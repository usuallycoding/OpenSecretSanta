## ADDED Requirements

### Requirement: Copying from the passwords dialog
Every row of the temporary passwords dialog SHALL have the app's copy icon, which puts that row on the clipboard as `username: password` with the icon's usual confirmation or failure. With two or more rows the dialog SHALL also have a "Copy all" button beside "OK" that copies every row as `username: password`, one per line, in the order shown. The rows SHALL stay selectable, in every use of the dialog.

#### Scenario: Copy one
- **WHEN** the dialog lists `bob` and `carol` and the person chooses the copy icon of `bob`
- **THEN** the clipboard holds the single line `bob: <password>` and nothing of `carol`, and that icon shows it was copied

#### Scenario: Copy all
- **WHEN** the dialog lists `bob` and `carol` and the person chooses "Copy all"
- **THEN** the clipboard holds a line `bob: <password>` and a line `carol: <password>`, in that order

#### Scenario: One person
- **WHEN** the dialog lists one person, as after "Reset password"
- **THEN** it has that row's copy icon and no "Copy all"

#### Scenario: Nobody
- **WHEN** the dialog says that nobody has a temporary password
- **THEN** it has no copy icon and no "Copy all"

#### Scenario: Clipboard refused
- **WHEN** the platform refuses the copy, as the web does on a page that is not secure
- **THEN** the icon or button shows that it failed and the rows can still be selected and copied by hand

#### Scenario: Closing
- **WHEN** the dialog is closed
- **THEN** the passwords are no longer anywhere in the app
