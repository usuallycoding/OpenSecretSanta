# Spec Delta

## MODIFIED Requirements

### Requirement: The rules page is a page, or a pane on a wide window
On a narrow window the gavel button SHALL open the rules page as a new page with a back action. On a wide window it SHALL open the same content in the pane beside the group page, like a participant's wishlist, with a close action, leaving the group page where it was. Both SHALL be the same screen at the rules' web address, so that changing the window's width shows the page or the pane and never a bottom sheet. Opening a participant's wishlist, a pair or the Announcements while the rules pane is open SHALL replace it.

#### Scenario: Narrow window
- **WHEN** the admin taps the gavel on a phone-width window
- **THEN** the rules page opens as a page and back returns to the group page

#### Scenario: Wide window
- **WHEN** the admin taps the gavel on a wide window
- **THEN** the rules show in the pane to the right of the group page

#### Scenario: Replaced by another pane
- **WHEN** the rules pane is open and the admin opens a participant's wishlist
- **THEN** the pane shows the wishlist instead

#### Scenario: Window made narrow
- **WHEN** the rules are open in the pane and the window is made narrower than 840dp
- **THEN** the rules are shown as a page with the back action, not in a bottom sheet

#### Scenario: Window made wide
- **WHEN** the rules page is open and the window is made 840dp wide or wider
- **THEN** the group page is shown with the rules in the pane beside it

#### Scenario: Replaced by a pair or the announcements
- **WHEN** the rules pane is open and the admin opens a pair or the Announcements
- **THEN** the pane shows it instead, and closing it shows the group page alone
