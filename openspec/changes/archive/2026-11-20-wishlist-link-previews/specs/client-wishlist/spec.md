# Spec Delta

## MODIFIED Requirements

### Requirement: A link is one line with a copy icon
Wherever a wishlist item is shown, each link SHALL be shown with a copy icon that copies the whole URL without opening it, and a screen reader SHALL read the link as one that opens in the browser. When a `ready` preview exists, it SHALL render as a compact horizontal row with a small square thumbnail (or link icon placeholder), title, and site name/host, ellipsized as needed. Otherwise it SHALL fall back to showing the URL on one line, cut with an ellipsis when it does not fit.

#### Scenario: A long link
- **WHEN** an item has a link of 300 characters whose preview is pending or unavailable
- **THEN** it is shown on one line, cut with an ellipsis, and choosing it opens the whole link

#### Scenario: Copy the whole link
- **WHEN** the person chooses the copy icon of a link of 300 characters (whether shown as a preview or fallback line)
- **THEN** all 300 characters are copied and the link does not open

#### Scenario: Read as a link
- **WHEN** a screen reader reaches a link preview or fallback link
- **THEN** it is read as a link that opens in the browser

#### Scenario: Link with full preview
- **WHEN** an item has a link whose preview is `ready` with a title, site name, and cached thumbnail
- **THEN** the link is shown as a compact horizontal row with the square thumbnail, title, site name, and copy icon, and choosing the preview opens the link in the browser

#### Scenario: Link with title-only preview
- **WHEN** an item has a link whose preview is `ready` with a title and site name but no thumbnail (or the thumbnail fails to load)
- **THEN** the link is shown as a compact horizontal row with a link icon placeholder, title, site name, and copy icon

## ADDED Requirements

### Requirement: Automatic follow-up refresh for pending previews
When a loaded wishlist contains one or more links whose previews are `pending`, the client SHALL perform a brief, bounded background refresh so newly resolved previews appear automatically without requiring a manual reload.

#### Scenario: Preview appears shortly after adding a link
- **WHEN** a person adds an item with a new link and the initial response marks its preview as `pending`
- **THEN** the item immediately shows the fallback one-line URL and automatically updates to the compact preview row once the follow-up refresh receives the `ready` preview

#### Scenario: Bounded retries when still pending
- **WHEN** a preview remains `pending` after the bounded follow-up checks complete
- **THEN** the client stops polling and leaves the fallback link visible until the next normal refresh
