# Spec Delta

## MODIFIED Requirements

### Requirement: Adding an item
While the group is Pending or Active, the person SHALL be able to add an item with the floating "Add item" button, in a bottom sheet over the list, with a title, an optional note and an optional link. A successful save through `POST /groups/{id}/wishlist/items` SHALL close the sheet and show the item at the end of the list. A list holds at most 50 items; at 50 adding SHALL be unavailable with an explanation.

#### Scenario: Add an item
- **WHEN** the person opens the add sheet, enters a title and saves
- **THEN** the sheet closes and the item is the last one in the list

#### Scenario: List is full
- **WHEN** the list already has 50 items
- **THEN** the add action is unavailable and says the list is full

#### Scenario: Dismissing the sheet
- **WHEN** the person closes the sheet without saving
- **THEN** nothing is added and what they typed is discarded
