# Spec Delta

## MODIFIED Requirements

### Requirement: Group page
Choosing a group SHALL open its page, loaded with `GET /groups/{id}`, showing its name, state and state note, description, date, time and location, then the pair rows of `client-group-pages`, and at the end the list of every participant's username with the person's own marked. The participants list SHALL be a box that scrolls inside itself and fills all the height left below the details and the pair rows, with a minimum height; when the details leave less room than that, they SHALL scroll above it. Parts the group does not have SHALL be left out.

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
- **THEN** the list fills the rest of the page and scrolls inside itself, and the rest of the page does not grow

#### Scenario: Few participants
- **WHEN** the group has four participants on a tall screen
- **THEN** the box still fills the rest of the page, with the names at its top
