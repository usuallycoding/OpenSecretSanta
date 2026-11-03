# Spec Delta

## ADDED Requirements

### Requirement: Pages can open on a tab
The Receiver and Gifter pages SHALL be able to open on either of their two tabs, the wishlist tab by default. A page that has no tabs, the Receiver page of a Pending group, SHALL show its wishlist whichever tab was asked for. Opening on a tab SHALL NOT change what the tab shows or how it loads.

#### Scenario: Opens on the questions tab
- **WHEN** the Receiver page is opened on its questions tab in an Active group
- **THEN** the "Respond to Secret Santa" tab is shown and its questions load

#### Scenario: Default tab
- **WHEN** a page is opened without a tab
- **THEN** its wishlist tab is shown

#### Scenario: Pending group
- **WHEN** the Receiver page is opened on its questions tab while the group is Pending
- **THEN** the page shows the wishlist and no tabs

#### Scenario: Other tab still works
- **WHEN** a page opened on its questions tab is swiped to its wishlist tab
- **THEN** the wishlist tab shows and loads as it does when it is opened first
