# Spec Delta

## ADDED Requirements

### Requirement: Oversight routes in the admin view
The admin view SHALL have typed routes for a participant's wishlist and for a group's questions, reached from the group detail and kept under the Groups tab. Back SHALL return to the group detail, and on the web the address bar SHALL follow them.

#### Scenario: Back from a wishlist
- **WHEN** the owner opens a participant's wishlist and goes back
- **THEN** the group detail is shown

#### Scenario: Groups tab stays selected
- **WHEN** the questions screen is open
- **THEN** the Groups tab is the selected tab
