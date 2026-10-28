# Spec Delta

## MODIFIED Requirements

### Requirement: Loading, errors and refresh
While a wishlist screen loads, the app SHALL show a progress indicator. When it cannot be loaded the app SHALL say so without technical details and offer to try again, keeping what was shown on a failed refresh. Both wishlist screens SHALL be refreshed as described in "Pulling a screen refreshes it" and "The web has no refresh control" and reload when the app returns to the foreground.

#### Scenario: Loading
- **WHEN** a wishlist screen is opened
- **THEN** a progress indicator is shown until the list arrives

#### Scenario: Server unreachable
- **WHEN** the list cannot be loaded because the server cannot be reached
- **THEN** the screen says so and offers "Try again"

#### Scenario: Return to the app
- **WHEN** the app returns to the foreground
- **THEN** the visible wishlist screen reloads without clearing what it shows
