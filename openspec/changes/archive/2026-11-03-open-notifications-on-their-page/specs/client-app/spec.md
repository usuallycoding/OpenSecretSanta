# Spec Delta

## MODIFIED Requirements

### Requirement: Readable web addresses
In the web app each screen SHALL have a short, readable address after the `#` (for example `#/groups/2` for a group, `#/groups/2/gifter` and `#/groups/2/receiver` for its Gifter and Receiver pages, `#/profile`, `#/settings`, `#/admin/groups/2` and `#/admin/users/alice`), and the address bar SHALL follow the screens as the person moves between them. A page that opens on its questions tab SHALL have the address of the page followed by `/questions`. The addresses SHALL NOT contain the names of the app's classes. Bottom sheets SHALL NOT be part of the address.

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

#### Scenario: A page on its questions tab
- **WHEN** a signed-in person opens `#/groups/2/receiver/questions` or `#/groups/2/gifter/questions`
- **THEN** that page of group 2 opens on its questions tab

#### Scenario: The tab is where the page opens
- **WHEN** the person switches tabs on a page
- **THEN** the address stays as it was, and a name for the wishlist tab such as `#/groups/2/receiver/wishlist` names no screen
