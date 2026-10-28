# Spec Delta

## MODIFIED Requirements

### Requirement: Admin group detail
Choosing a group SHALL open its detail, loaded with `GET /admin/groups/{id}`, showing its details, its state with a sentence on what that state means for the owner, and its participants. Where the state allows, the detail SHALL also offer the owner's oversight: choosing a participant opens their wishlist (Pending, Active and Archived), and an Active or Archived group shows the draw and a way to the Questions screen.

#### Scenario: Group not available
- **WHEN** the server answers that the group does not exist for this administrator
- **THEN** the screen says the group is no longer available and offers to go back

#### Scenario: Participant wishlist offered
- **WHEN** a Pending, Active or Archived group's detail is shown
- **THEN** choosing a participant opens that person's wishlist

#### Scenario: Draft offers no oversight
- **WHEN** the group is Draft
- **THEN** choosing a participant does not open a wishlist and there is no draw or Questions entry

#### Scenario: Questions offered
- **WHEN** the group is Active or Archived
- **THEN** the detail offers the Questions screen
