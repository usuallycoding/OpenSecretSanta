## MODIFIED Requirements

### Requirement: Views by role
After sign-in the app SHALL show a participant view to everyone and an admin view to administrators only. The participant view SHALL be a Groups tab and a Profile tab under a top bar; the admin view SHALL be a Groups tab with the administrator's groups and a Users tab, under the same top bar. The top bar SHALL have a menu with "Appearance", "Change password", "Sign out" and, for administrators only, an item that switches to the other view. An administrator SHALL start in the admin view. A person who is not an administrator SHALL see no admin view and no item to reach it. The choice SHALL last for the current run only. The admin status SHALL come from the server's `GET /me` and be refreshed whenever the app starts or the person signs in.

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
- **THEN** it offers "Appearance", "Change password" and "Sign out", and the view switch for an administrator
