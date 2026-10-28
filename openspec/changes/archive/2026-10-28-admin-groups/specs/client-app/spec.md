# Spec Delta

## REMOVED Requirements

### Requirement: Participant and admin views
**Reason**: The admin view is no longer a placeholder, so the requirement and its "Views are placeholders for now" scenario are replaced by "Participant view and admin view" below.
**Migration**: None; the behavior is carried over unchanged except that the admin view now shows the administrator's groups.

## ADDED Requirements

### Requirement: Participant view and admin view
After sign-in the app SHALL show a participant view to everyone and an admin view to administrators only. The participant view SHALL be a Groups tab and a Profile tab under a top bar; the admin view SHALL show the administrator's groups under the same top bar. The top bar SHALL have a menu with "Sign out" and, for administrators only, an item that switches to the other view. An administrator SHALL start in the admin view. A person who is not an administrator SHALL see no admin view and no item to reach it. The choice SHALL last for the current run only. The admin status SHALL come from the server's `GET /me` and be refreshed whenever the app starts or the person signs in.

#### Scenario: Administrator
- **WHEN** an administrator signs in
- **THEN** the admin view is shown and the top-bar menu switches to the participant view and back

#### Scenario: Normal participant
- **WHEN** a non-administrator signs in
- **THEN** only the participant view is shown and there is no way to reach the admin view

#### Scenario: Promoted while signed in
- **WHEN** an account is promoted after the person signed in
- **THEN** the admin view becomes available the next time the app starts or the person signs in again

#### Scenario: The admin view shows the groups
- **WHEN** an administrator is in the admin view
- **THEN** it shows the administrator's own groups, and switching to the participant view and back opens each view on its first screen
