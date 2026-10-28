## MODIFIED Requirements

### Requirement: Views by role
After sign-in the app SHALL show a participant view to everyone and an admin view to administrators only. The participant view SHALL be a Groups tab and a Profile tab under a top bar; the admin view SHALL be a Groups tab with the administrator's groups and a Users tab, under the same top bar. The top bar SHALL have a menu with "Change password", "Sign out" and, for administrators only, an item that switches to the other view. An administrator SHALL start in the admin view. A person who is not an administrator SHALL see no admin view and no item to reach it. The choice SHALL last for the current run only. The admin status SHALL come from the server's `GET /me` and be refreshed whenever the app starts or the person signs in.

#### Scenario: Administrator
- **WHEN** an administrator signs in
- **THEN** the admin view is shown and the top-bar menu switches to the participant view and back

#### Scenario: Normal participant
- **WHEN** a non-administrator signs in
- **THEN** only the participant view is shown and there is no way to reach the admin view

#### Scenario: The admin view has two tabs
- **WHEN** an administrator is in the admin view
- **THEN** a bottom bar offers Groups, which shows the administrator's own groups, and Users

#### Scenario: Switching views
- **WHEN** an administrator switches to the participant view and back
- **THEN** each view opens on its first screen

#### Scenario: The menu in both views
- **WHEN** the person opens the top-bar menu in the participant view or in the admin view
- **THEN** it offers "Change password" and "Sign out", and the view switch for an administrator

## ADDED Requirements

### Requirement: Changing your own password
Choosing "Change password" in the top-bar menu SHALL open a bottom sheet over the current screen, which stays as it was, asking for the current password, the new password and a repeat of it, with the same checks before sending and the same field-level server errors as the password-change form, and sending `PUT /me/password`. On success the app SHALL keep the person signed in with the new token the server returns, and the sheet SHALL say that the password was changed and that the person is signed out on their other devices, with a way to close it. Closing the sheet at any time, before or after a change, SHALL discard everything typed in it.

#### Scenario: Open from either view
- **WHEN** a participant, or an administrator in the admin view, chooses "Change password" in the menu
- **THEN** the sheet opens over the screen they were on

#### Scenario: Change succeeds
- **WHEN** the person enters the right current password and a valid new one twice and saves
- **THEN** the sheet says the password was changed and that other devices are signed out, the screen behind is unchanged and the person is still signed in

#### Scenario: Checks before sending
- **WHEN** the new password is shorter than 8 characters, equals the current one, or does not match the repeat
- **THEN** the sheet says what is wrong next to the field and sends nothing

#### Scenario: Wrong current password
- **WHEN** the server refuses the change because the current password is wrong
- **THEN** the message appears next to the current password field and the sheet stays open with the new entries

#### Scenario: Server unreachable
- **WHEN** the request cannot reach the server
- **THEN** the sheet says so and keeps what was typed

#### Scenario: Closing discards the entries
- **WHEN** the person closes the sheet and opens it again
- **THEN** all three fields are empty and no earlier message is shown
