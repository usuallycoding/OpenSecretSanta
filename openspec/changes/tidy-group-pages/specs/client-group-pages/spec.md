# Spec Delta

## MODIFIED Requirements

### Requirement: Receiver page
The Receiver page, the one for the relationship in which the person receives, SHALL open from the gift icon while the group is Pending and from the second row's arrow once it is Active or Archived. Once the group is Active or Archived it SHALL show a tab row with two tabs, **Wishlist** and **Questions**, and one tab at a time. The Wishlist tab SHALL show the person's own wishlist for the group, loaded with `GET /groups/{id}/wishlist`, with each item's title, note and link in the person's own order, or a short explanation that nothing has been added yet with the offer to add the first item; the list SHALL be edited as `client-wishlist` describes. The Questions tab SHALL show the questions the person's Santa asked as `client-receiver-and-questions` describes, with the answering described there. While the group is Pending the page SHALL show the wishlist only, without tabs. Nothing on the page SHALL identify the Santa.

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

### Requirement: Gifter page
The Gifter page, the one for the relationship in which the person gives, SHALL open from the first row's arrow once the group is Active or Archived. It SHALL show a tab row with two tabs, **Wishlist** and **Questions**, and one tab at a time. The Wishlist tab SHALL show the list the person's receiver keeps for the group, loaded with `GET /groups/{id}/receiver/wishlist`, read-only, in the receiver's order, with each item's title, note and link, or a short explanation that the receiver has not added anything yet. The Questions tab SHALL show the person's own questions to the receiver as `client-receiver-and-questions` describes, with the receiver's answers and, while the group is Active, the way to ask a new question. Nothing the person does on the page SHALL be visible to the receiver.

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

### Requirement: Page loading and refreshing
Each tab of the Gifter and Receiver pages SHALL load on its own, show that it is loading, and show a message with "Try again" when the server cannot be reached or answers with an error, without affecting the other tab or what it already showed. Pulling a tab down (on Android and iOS) SHALL reload that tab, and returning to the app and choosing a tab SHALL reload what the tab shows, as "Pulling a screen refreshes it" and "The web has no refresh control" describe.

#### Scenario: One section fails
- **WHEN** the questions cannot be loaded and the wishlist can
- **THEN** the Wishlist tab shows the list and the Questions tab shows the problem with "Try again"

#### Scenario: Pull to refresh
- **WHEN** the person pulls a tab down on Android or iOS
- **THEN** that tab reloads and what is shown stays until the new data arrives

#### Scenario: Choosing a tab
- **WHEN** the person chooses a tab they have not looked at for a while
- **THEN** its content reloads without clearing what it showed
