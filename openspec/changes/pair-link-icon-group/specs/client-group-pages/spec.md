# Spec Delta

## MODIFIED Requirements

### Requirement: Pair rows
Below the group's details the group page SHALL show, while the group is Pending, one row with the person's own username and a gift icon that opens the Receiver page; and, once the group is Active or Archived, two rows: `my username -> receiver's username`, whose pair link opens the Gifter page, and `Secret Santa -> my username`, whose pair link opens the Receiver page. In each of those rows the name before the pair link SHALL start at the start of the row and the name after it SHALL end at the end of the row, and the pair link SHALL be the control described by `client-pair-link`. The receiver's username SHALL come from `GET /groups/{id}/receiver`; while it loads the row SHALL show that, and when it fails the row SHALL say so with "Try again" and the rest of the page SHALL stay. A Draft group SHALL show no rows.

#### Scenario: Pending
- **WHEN** the group is Pending
- **THEN** one row shows the person's own username with a gift icon, and no row names a receiver or a Santa

#### Scenario: Active or Archived
- **WHEN** the group is Active or Archived
- **THEN** two rows are shown, each with a pair link between its names: `alice` and `bob`, and `Secret Santa` and `alice`, for the person `alice` whose receiver is `bob`

#### Scenario: Receiver cannot be loaded
- **WHEN** the receiver request fails in an Active group
- **THEN** the first row says so and offers "Try again", and the rest of the page is unchanged

#### Scenario: The arrows
- **WHEN** the person chooses the pair link of the first row, and then of the second
- **THEN** the Gifter page opens, and the Receiver page opens

#### Scenario: Alignment
- **WHEN** the group is Active
- **THEN** in both rows the name before the pair link is at the start of the row and the name after it is at the end of the row

### Requirement: Receiver page
The Receiver page, the one for the relationship in which the person receives, SHALL open from the gift icon while the group is Pending and from the second row's pair link once it is Active or Archived. Once the group is Active or Archived it SHALL show a tab row with two tabs, **Wishlist** and **Questions**, and one tab at a time, which the person SHALL be able to change by choosing a tab or by swiping the content sideways, the tab row following the swipe. The Wishlist tab SHALL show the person's own wishlist for the group, loaded with `GET /groups/{id}/wishlist`, with each item's title, note and link in the person's own order, or a short explanation that nothing has been added yet with the offer to add the first item; the list SHALL be edited as `client-wishlist` describes. The Questions tab SHALL show the questions the person's Santa asked as `client-receiver-and-questions` describes, with the answering described there. While the group is Pending the page SHALL show the wishlist only, without tabs. Nothing on the page SHALL identify the Santa.

#### Scenario: Items in order
- **WHEN** the person has three items
- **THEN** the Wishlist tab shows them in the order the person chose, each with its title and, when present, its note and link

#### Scenario: Empty list
- **WHEN** the person has not added anything to this group
- **THEN** the Wishlist tab says so and offers to add the first item

#### Scenario: Separate per group
- **WHEN** the person is in two groups
- **THEN** each group's Receiver page shows only that group's items

#### Scenario: Pending group
- **WHEN** the group is Pending
- **THEN** the page shows the wishlist and no tabs and no questions

#### Scenario: Active group
- **WHEN** the group is Active and the Santa has asked a question
- **THEN** the page has the tabs Wishlist and Questions, and the Questions tab shows the question with an "Answer" action

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the wishlist and the questions with the person's answers are shown in their tabs and nothing can be changed

#### Scenario: Switching tabs
- **WHEN** the person chooses the Questions tab and then the Wishlist tab
- **THEN** each shows its own content and the tab row stays in place

#### Scenario: Swiping
- **WHEN** the person swipes the Wishlist tab's content to the left
- **THEN** the Questions tab is shown and the tab row marks it

### Requirement: Gifter page
The Gifter page, the one for the relationship in which the person gives, SHALL open from the first row's pair link once the group is Active or Archived. It SHALL show a tab row with two tabs, **Wishlist** and **Questions**, and one tab at a time, which the person SHALL be able to change by choosing a tab or by swiping the content sideways, the tab row following the swipe. The Wishlist tab SHALL show the list the person's receiver keeps for the group, loaded with `GET /groups/{id}/receiver/wishlist`, read-only, in the receiver's order, with each item's title, note and link, or a short explanation that the receiver has not added anything yet. The Questions tab SHALL show the person's own questions to the receiver as `client-receiver-and-questions` describes, with the receiver's answers and, while the group is Active, the way to ask a new question. Nothing the person does on the page SHALL be visible to the receiver.

#### Scenario: Receiver's items
- **WHEN** the person opens the page and the receiver has two items
- **THEN** the Wishlist tab shows both in the receiver's order with their note and link, and no control changes them

#### Scenario: Nothing added yet
- **WHEN** the receiver has not added anything
- **THEN** the Wishlist tab says the receiver has not added anything yet

#### Scenario: Questions and answers
- **WHEN** the person has asked two questions and the receiver answered the first
- **THEN** the Questions tab shows question 1 with its answer and question 2 with "Not answered yet"

#### Scenario: Ask from the page
- **WHEN** the group is Active and the person chooses to ask a question on the Questions tab
- **THEN** a bottom sheet opens, as `client-receiver-and-questions` describes

#### Scenario: Swiping
- **WHEN** the person swipes the Wishlist tab's content to the left
- **THEN** the Questions tab is shown and the tab row marks it
