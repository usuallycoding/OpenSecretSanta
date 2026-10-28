# Spec Delta

## ADDED Requirements

### Requirement: Links open in a dialog
Choosing a link shown on an item SHALL open a dialog with the whole link, not cut, with an "Open in browser" button and a "Cancel" button, and a copy icon as for other read-only text. "Open in browser" SHALL open the link in the platform's browser or default handler and close the dialog; "Cancel" SHALL close it and open nothing. Only addresses the server accepted (`http` or `https`) SHALL ever be shown as links. This applies to every place a wishlist link is shown.

#### Scenario: Show the whole link
- **WHEN** the person chooses a link of 300 characters
- **THEN** a dialog shows all of it, wrapped

#### Scenario: Open it
- **WHEN** the person chooses "Open in browser" in the link dialog
- **THEN** the link opens outside the app in the platform's browser and the dialog closes

#### Scenario: Cancel
- **WHEN** the person chooses "Cancel" in the link dialog
- **THEN** the dialog closes and nothing opens

## REMOVED Requirements

### Requirement: Links open in the browser
**Reason**: A link now opens a dialog first (`Links open in a dialog`), so that it can be read in full and copied before it leaves the app.
**Migration**: The browser opens from the "Open in browser" button of the dialog.
