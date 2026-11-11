## MODIFIED Requirements

### Requirement: Owner reads a participant's wishlist
The server SHALL expose `GET /admin/groups/{id}/participants/{username}/wishlist`, usable only by the group's owner while the group is Pending, Active or Archived, returning the wishlist that participant keeps for that group in their chosen order, with each item's title, note, links in their order, creation time and last-changed time and without item ids. For apps that predate several links, each item SHALL also carry its first link as the deprecated `url`.

#### Scenario: Owner reads a list
- **WHEN** the owner requests the wishlist of participant `bob` in an Active group
- **THEN** the response lists `bob`'s items for that group in his order with title, note, links and times, and no ids

#### Scenario: Several links
- **WHEN** an item of `bob` has three links
- **THEN** the response lists the three in their order in `urls` and the first as `url`

#### Scenario: Pending group
- **WHEN** the owner requests a participant's wishlist while the group is Pending
- **THEN** the response lists the items the participant has added so far

#### Scenario: Empty list
- **WHEN** the participant has no items in this group
- **THEN** the response is 200 with an empty list

#### Scenario: Draft group
- **WHEN** the owner requests a wishlist of a Draft group
- **THEN** the response is 409 with code `invalid_state`

#### Scenario: Not a participant
- **WHEN** the owner requests the wishlist of a username that is not a participant of the group
- **THEN** the response is 404

#### Scenario: Still readable after archive
- **WHEN** the group has been Archived
- **THEN** the owner can still read every participant's wishlist, unchanged
