# Spec Delta

## ADDED Requirements

### Requirement: Group page
Choosing a group SHALL open its page, loaded with `GET /groups/{id}`, showing its name, state and state note, description, date, time and location, then the pair rows of `client-group-pages`, and at the end the list of every participant's username with the person's own marked. The participants list SHALL be a small list of fixed height that scrolls inside itself when it has more names than fit. Parts the group does not have SHALL be left out.

#### Scenario: Opening a group
- **WHEN** the person chooses a group in the list
- **THEN** the page shows its details, the pair rows and, at the end, every participant's username with their own marked "you"

#### Scenario: Back to the list
- **WHEN** the person goes back from the group page
- **THEN** the list is shown again

#### Scenario: Group no longer available
- **WHEN** the server answers that the group does not exist, for example because the person was removed from it
- **THEN** the page says the group is no longer available and offers to go back

#### Scenario: Many participants
- **WHEN** the group has more participants than fit in the list
- **THEN** the list keeps its height and scrolls inside itself, and the rest of the page does not grow

## REMOVED Requirements

### Requirement: Group detail
**Reason**: The detail with its Wishlists and Your gift sections is replaced by the group page, whose pair rows lead to the Gifter and Receiver pages.
**Migration**: See `Group page` here and `client-group-pages`.
