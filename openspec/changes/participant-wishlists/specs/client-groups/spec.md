# Spec Delta

## MODIFIED Requirements

### Requirement: Group detail
Choosing a group SHALL open its detail screen, loaded with `GET /groups/{id}`, showing its name, state and state note, description, date, time, location and the participants' usernames, with the person's own username marked, followed by a Wishlists section. Parts the group does not have SHALL be left out. The Wishlists section SHALL always offer "My wishlist" and SHALL offer "Your receiver's wishlist" only while the group is Active or Archived; while it is Pending it SHALL say that the receiver's list appears after the names are drawn.

#### Scenario: Opening a group
- **WHEN** the person chooses a group in the list
- **THEN** the detail screen shows its details and every participant's username, with their own marked "you"

#### Scenario: Back to the list
- **WHEN** the person goes back from the detail screen
- **THEN** the list is shown again

#### Scenario: Group no longer available
- **WHEN** the server answers that the group does not exist, for example because the person was removed from it
- **THEN** the screen says the group is no longer available and offers to go back

#### Scenario: Wishlists in a Pending group
- **WHEN** the group is Pending
- **THEN** the Wishlists section offers only "My wishlist" and says the receiver's list appears after the draw

#### Scenario: Wishlists in an Active or Archived group
- **WHEN** the group is Active or Archived
- **THEN** the Wishlists section offers both "My wishlist" and "Your receiver's wishlist"
