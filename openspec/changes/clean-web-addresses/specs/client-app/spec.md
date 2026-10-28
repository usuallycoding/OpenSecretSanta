# Spec Delta

## ADDED Requirements

### Requirement: Readable web addresses
In the web app each screen SHALL have a short, readable address after the `#` (for example `#/groups/2` for a group, `#/groups/2/wishlist` for its wishlist, `#/profile`, `#/admin/groups/2` and `#/admin/users/alice`), and the address bar SHALL follow the screens as the person moves between them. The addresses SHALL NOT contain the names of the app's classes. Bottom sheets SHALL NOT be part of the address.

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

### Requirement: Opening a web address
In the web app, opening or refreshing an address, or typing one into the address bar while signed in, SHALL show the screen it names. An address that names no screen SHALL show the first screen of the person's view. An address of an admin screen opened by someone who is not an administrator SHALL show the first screen. When the person is signed out, the sign-in screen SHALL be shown and, after signing in, the screen the address names.

#### Scenario: Refresh
- **WHEN** a signed-in person refreshes the page on `#/groups/2/wishlist`
- **THEN** that group's wishlist is shown

#### Scenario: Typed address
- **WHEN** a signed-in person types `#/profile` into the address bar
- **THEN** the Profile tab is shown without reloading the page

#### Scenario: Sign in first
- **WHEN** a signed-out person opens `#/groups/2` and signs in
- **THEN** the screen of group 2 is shown

#### Scenario: Unknown address
- **WHEN** a signed-in person opens an address the app does not know
- **THEN** the first screen of their view is shown

#### Scenario: Admin address without admin rights
- **WHEN** a participant who is not an administrator opens `#/admin/groups/2`
- **THEN** the first screen is shown and not an admin screen

#### Scenario: Old address
- **WHEN** a person opens an address in the old form that contains the names of the app's classes
- **THEN** the first screen is shown
