# Spec Delta

## MODIFIED Requirements

### Requirement: Views by role
After sign-in the app SHALL show a participant view to everyone and an admin view to administrators only. The participant view SHALL be a Groups tab, a Profile tab and a Settings tab under a top bar; the admin view SHALL be a Groups tab with the administrator's groups, a Users tab and the same Settings tab, under the same top bar. The top bar SHALL have no menu: for administrators only it SHALL have one icon that switches to the other view, announced as "Switch to participant view" in the admin view and "Switch to admin view" in the participant view, and for everyone else no action. "Appearance", "Notifications", "Change password" and "Sign out" SHALL be on the Settings page, as `client-settings` describes. An administrator SHALL start in the admin view. A person who is not an administrator SHALL see no admin view and no item to reach it. The choice SHALL last for the current run only. The admin status SHALL come from the server's `GET /me` and be refreshed whenever the app starts or the person signs in.

#### Scenario: Administrator
- **WHEN** an administrator signs in
- **THEN** the admin view is shown and the switch icon in the top bar switches to the participant view and back

#### Scenario: Normal participant
- **WHEN** a non-administrator signs in
- **THEN** only the participant view is shown and there is no way to reach the admin view

#### Scenario: The admin view has two tabs
- **WHEN** an administrator is in the admin view
- **THEN** a bottom bar offers its two tabs, Groups, which shows the administrator's own groups, and Users, and then the Settings tab that both views share

#### Scenario: Switching views
- **WHEN** an administrator switches to the participant view and back
- **THEN** each view opens on its first screen

#### Scenario: The menu in both views
- **WHEN** the person looks at the top bar in the participant view or in the admin view
- **THEN** there is no menu: an administrator sees the one switch icon and anyone else sees no action

### Requirement: Readable web addresses
In the web app each screen SHALL have a short, readable address after the `#` (for example `#/groups/2` for a group, `#/groups/2/gifter` and `#/groups/2/receiver` for its Gifter and Receiver pages, `#/profile`, `#/settings`, `#/admin/groups/2` and `#/admin/users/alice`), and the address bar SHALL follow the screens as the person moves between them. The addresses SHALL NOT contain the names of the app's classes. Bottom sheets SHALL NOT be part of the address.

#### Scenario: A group
- **WHEN** a signed-in person opens a group in the web app
- **THEN** the address ends in `#/groups/` followed by the group's number

#### Scenario: A user in the admin view
- **WHEN** an administrator opens the user `alice` in the web app
- **THEN** the address ends in `#/admin/users/alice`

#### Scenario: Back and forward
- **WHEN** the person moves between screens and uses the browser's back and forward buttons
- **THEN** the screens and their addresses follow, as before

#### Scenario: Sheet
- **WHEN** a bottom sheet is open on a group's screen in the web app
- **THEN** the address is still the group's address

### Requirement: Changing your own password
Choosing "Change password" on the Settings page SHALL open a bottom sheet over the current screen, which stays as it was, asking for the current password, the new password and a repeat of it, with the same checks before sending and the same field-level server errors as the password-change form, and sending `PUT /me/password`. On success the app SHALL keep the person signed in with the new token the server returns, and the sheet SHALL say that the password was changed and that the person is signed out on their other devices, with a way to close it. Closing the sheet at any time, before or after a change, SHALL discard everything typed in it.

#### Scenario: Open from either view
- **WHEN** a participant, or an administrator in the admin view, chooses "Change password" on the Settings page
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
