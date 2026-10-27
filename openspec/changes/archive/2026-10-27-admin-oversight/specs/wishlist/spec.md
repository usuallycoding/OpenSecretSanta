# Spec Delta

## MODIFIED Requirements

### Requirement: Wishlist is private
A wishlist SHALL be readable only by its owner, by the participant who is the owner's Santa in that group once it is Active or Archived, in the way described by the gift-visibility capability, and by the owner of the group through `GET /admin/groups/{id}/participants/{username}/wishlist` as described by the admin-oversight capability. No other admin endpoint SHALL return it, and no admin SHALL be able to change it.

#### Scenario: Other participants
- **WHEN** a participant who is not the owner's Santa tries to read the owner's wishlist through the group
- **THEN** they can only read the wishlist of their own receiver

#### Scenario: Group owner through oversight
- **WHEN** the owner of the group uses the admin-oversight wishlist endpoint
- **THEN** the response contains that participant's items, read-only

#### Scenario: Admin endpoints
- **WHEN** an admin uses any admin endpoint other than the group owner's oversight endpoint
- **THEN** no response contains wishlist items
