# Spec Delta

## MODIFIED Requirements

### Requirement: Read-only once archived
While the group is Archived, the wishlist on the Receiver page SHALL be read-only: no add, edit, delete or reorder control is shown and the page says the list can no longer be changed. When the group is archived while the person is editing, a refused change SHALL close the sheet, reload the list as read-only and say so.

#### Scenario: Archived group
- **WHEN** the person opens the Receiver page of an Archived group
- **THEN** the items are shown with no way to change them and a note that the list is closed

#### Scenario: Archived meanwhile
- **WHEN** the server answers a change with 409 because the group was archived after the page opened
- **THEN** the sheet closes, the list reloads read-only and the page says it can no longer be changed

#### Scenario: Item deleted elsewhere
- **WHEN** the server answers an edit or delete with 404 because the item no longer exists
- **THEN** the sheet closes, the list reloads and the page says the item no longer exists

### Requirement: Loading, errors and refresh
While a wishlist loads, the app SHALL show a progress indicator in its place. When it cannot be loaded the app SHALL say so without technical details and offer to try again, keeping what was shown on a failed refresh. Both wishlists (the person's own on the Receiver page and the receiver's on the Gifter page) SHALL be refreshed as described in "Pulling a screen refreshes it" and "The web has no refresh control" and reload when the app returns to the foreground.

#### Scenario: Loading
- **WHEN** a page with a wishlist is opened
- **THEN** a progress indicator is shown in the wishlist's place until the list arrives

#### Scenario: Server unreachable
- **WHEN** the list cannot be loaded because the server cannot be reached
- **THEN** the wishlist area says so and offers "Try again"

#### Scenario: Return to the app
- **WHEN** the app returns to the foreground
- **THEN** the visible page's wishlist reloads without clearing what it shows

## REMOVED Requirements

### Requirement: My wishlist
**Reason**: "My wishlist" is no longer an entry or a screen; the person's own wishlist is the first section of the Receiver page.
**Migration**: See `Receiver page` in `client-group-pages`.

### Requirement: Your receiver's wishlist
**Reason**: "Your receiver's wishlist" is no longer an entry or a screen; the receiver's list is the first section of the Gifter page.
**Migration**: See `Gifter page` in `client-group-pages`.
