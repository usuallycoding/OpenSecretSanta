# Spec Delta

## MODIFIED Requirements

### Requirement: Pair sheet
Choosing the pair link in a row of the pair list SHALL open the pair, titled with the pair, showing the receiver's wishlist, loaded with `GET /admin/groups/{id}/participants/{receiver}/wishlist` and shown as in "Participant wishlist", and below it the thread of questions the Santa asked the receiver. On a narrow window it SHALL be a page with a back action and on a wide window a pane beside the group page, as the rules are, each at the pair's web address. It SHALL offer no way to ask, answer or change anything.

#### Scenario: Wishlist and thread shown
- **WHEN** the owner chooses the pair link in the row of `alice` and `bob`
- **THEN** the page or pane shows `bob`'s wishlist and the questions `alice` asked `bob` with his answers

#### Scenario: Read-only
- **WHEN** the pair page or pane is shown, whatever the group's state
- **THEN** there is no control that asks, answers or changes a question or a wishlist item

#### Scenario: Pane beside the page
- **WHEN** the owner chooses a pair link on a wide window
- **THEN** the pair opens in a pane beside the group page, and choosing another pair shows that pair in the pane

#### Scenario: Closing the pane
- **WHEN** the owner closes the pane, or goes back
- **THEN** the pane closes and the group page stays open

#### Scenario: A page on a narrow window
- **WHEN** the owner chooses a pair link on a phone-width window
- **THEN** the pair opens as a page titled with the pair, and back returns to the group page

#### Scenario: Window resized
- **WHEN** a pair is open and the window is made wider or narrower than 840dp
- **THEN** the pair is shown as a pane or as a page, never in a bottom sheet

## ADDED Requirements

### Requirement: What was said of the pair sheet carries over
Wherever another requirement speaks of the pair sheet, it SHALL apply to the page or the pane that shows the pair, with the same title, content and marks.

#### Scenario: The thread and the letters
- **WHEN** another requirement says the pair sheet shows the thread of questions and the letters
- **THEN** the pair page and the pair pane show them
