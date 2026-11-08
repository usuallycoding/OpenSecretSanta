# Spec Delta

## ADDED Requirements

### Requirement: Screens have readable web addresses
In the web app each screen SHALL have a short, readable address after the `#` (for example `#/groups/2` for a group, `#/groups/2/giver/wishlist` and `#/groups/2/receiver/questions` for the pages of its gift exchange, `#/groups/2/delivery-info`, `#/settings`, `#/admin/groups/2` and `#/admin/users/alice`), and the address bar SHALL follow the screens as the person moves between them.

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

#### Scenario: A page of the gift exchange
- **WHEN** a signed-in person opens `#/groups/2/receiver/questions` or `#/groups/2/giver/wishlist`
- **THEN** that page of group 2 opens, as `client-giver-receiver-pages` describes

## REMOVED Requirements

### Requirement: Readable web addresses
**Reason**: Renamed to "Screens have readable web addresses", because its examples and two of its scenarios spoke of the tabs of the Giver and Receiver pages.
**Migration**: See "Screens have readable web addresses".

### Requirement: A questions tab has its own address
**Reason**: There are no tabs; the questions and the wishlist each have a page and an address of their own.
**Migration**: See "Each page has its own web address" in `client-giver-receiver-pages`.
