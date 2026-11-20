# Spec Delta

## ADDED Requirements

### Requirement: Wishlist responses carry link previews
All endpoints returning wishlist items SHALL attach link preview metadata in `linkPreviews`, keyed by exact URL, while keeping `urls` and deprecated `url` unchanged. Each entry SHALL indicate `pending` or `ready` and, when ready, include title, site name, and optional relative thumbnail path; failed previews SHALL be omitted.

#### Scenario: Ready preview returned with item
- **WHEN** a participant reads their wishlist and a link has a cached ready preview with a thumbnail
- **THEN** the item includes that URL in `urls` and a `ready` entry for it in `linkPreviews` with its title, site name, and thumbnail path

#### Scenario: Pending preview returned after save
- **WHEN** a participant adds an item with a new URL whose preview has not finished fetching yet
- **THEN** `POST /groups/{id}/wishlist/items` returns 201 immediately without waiting for the external site, and `linkPreviews` marks that URL as `pending`

#### Scenario: Failed preview omitted
- **WHEN** a link's preview fetch failed
- **THEN** the link remains in `urls` and has no entry in `linkPreviews`

#### Scenario: Shared and oversight wishlists include previews
- **WHEN** a Santa reads their receiver's wishlist or a group owner reads a participant's wishlist through admin oversight
- **THEN** the returned items include the same `linkPreviews` data for each link

#### Scenario: Older clients unaffected
- **WHEN** an older client that ignores `linkPreviews` reads a wishlist
- **THEN** `urls` and `url` are present in the exact same shape as before

### Requirement: Non-blocking preview queuing on wishlist writes and reads
Adding or editing a wishlist item SHALL queue background preview resolution for any new or uncached URLs on that item without delaying the response. Reading any wishlist SHALL queue background preview resolution for any URLs missing a cache entry or eligible for retry after cooldown, without delaying the read response.

#### Scenario: Slow external site does not delay save
- **WHEN** a participant saves an item with a link to a slow or unreachable external host
- **THEN** the save completes immediately and the preview fetch runs in the background
