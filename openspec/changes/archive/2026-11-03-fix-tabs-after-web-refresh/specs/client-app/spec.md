# Spec Delta

## MODIFIED Requirements

### Requirement: Opening a web address
In the web app, opening or refreshing an address, or typing one into the address bar while signed in, SHALL show the screen it names. An address that names no screen SHALL show the first screen of the person's view. An address of an admin screen opened by someone who is not an administrator SHALL show the first screen. When the person is signed out, the sign-in screen SHALL be shown and, after signing in, the screen the address names. After a refresh every tab of the view SHALL work as it does without one.

#### Scenario: Refresh
- **WHEN** a signed-in person refreshes the page on `#/groups/2/receiver`
- **THEN** that group's Receiver page is shown

#### Scenario: Typed address
- **WHEN** a signed-in person types `#/profile` into the address bar
- **THEN** the Profile tab is shown without reloading the page

#### Scenario: Sign in first
- **WHEN** a signed-out person opens `#/groups/2` and signs in
- **THEN** the screen of group 2 is shown

#### Scenario: Unknown address
- **WHEN** a signed-in person opens an address the app does not know, such as the former `#/groups/2/wishlist`
- **THEN** the first screen of their view is shown

#### Scenario: Admin address without admin rights
- **WHEN** a participant who is not an administrator opens `#/admin/groups/2`
- **THEN** the first screen is shown and not an admin screen

#### Scenario: Old address
- **WHEN** a person opens an address in the old form that contains the names of the app's classes
- **THEN** the first screen is shown

#### Scenario: Groups tab after refreshing another tab
- **WHEN** a signed-in person refreshes the page on `#/profile` or `#/settings` and chooses the Groups tab
- **THEN** the groups list is shown and its address is `#/groups`

#### Scenario: Other tabs after a refresh
- **WHEN** a signed-in person refreshes the page on `#/profile` and chooses the Settings tab, then the Profile tab, then the Groups tab
- **THEN** each tab is shown as it is when no refresh has happened

#### Scenario: The admin view after a refresh
- **WHEN** an administrator refreshes the page on `#/admin/users` and chooses the Groups tab
- **THEN** the administrator's groups are shown and its address is `#/admin/groups`

#### Scenario: Back after a refresh
- **WHEN** a signed-in person refreshes the page on `#/profile` and goes back
- **THEN** the Groups tab is shown first, as it is without a refresh
