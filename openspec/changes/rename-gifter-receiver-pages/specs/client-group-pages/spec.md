# Spec Delta

## MODIFIED Requirements

### Requirement: Receiver page
The Receiver page, the one for the relationship in which the person receives, SHALL open from the gift icon while the group is Pending and from the second row's pair link once it is Active or Archived. Once the group is Active or Archived it SHALL be titled "Receiving from Secret Santa" and show a tab row with two tabs, **Your Wishlist** and **Respond to Secret Santa**, and one tab at a time, which the person SHALL be able to change by choosing a tab or by swiping the content sideways, the tab row following the swipe. The first tab SHALL show the person's own wishlist for the group, loaded with `GET /groups/{id}/wishlist`, with each item's title, note and link in the person's own order, or a short explanation that nothing has been added yet with the offer to add the first item; the list SHALL be edited as `client-wishlist` describes. The second tab SHALL show the questions the person's Santa asked as `client-receiver-and-questions` describes, with the answering described there. While the group is Pending the page SHALL be titled "Your Wishlist" and show the wishlist only, without tabs. Nothing on the page SHALL identify the Santa.

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
- **THEN** the page is titled "Your Wishlist", shows the wishlist and no tabs and no questions

#### Scenario: Active group
- **WHEN** the group is Active and the Santa has asked a question
- **THEN** the page is titled "Receiving from Secret Santa", has the tabs "Your Wishlist" and "Respond to Secret Santa", and the "Respond to Secret Santa" tab shows the question with an "Answer" action

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the wishlist and the questions with the person's answers are shown in their tabs and nothing can be changed

#### Scenario: Group phase not known yet
- **WHEN** the page is opened and the group's phase has not loaded
- **THEN** the page is titled "Your Wishlist" until the phase is known, and is then titled for that phase

#### Scenario: Switching tabs
- **WHEN** the person chooses the "Respond to Secret Santa" tab and then the "Your Wishlist" tab
- **THEN** each shows its own content and the tab row stays in place

#### Scenario: Swiping
- **WHEN** the person swipes the "Your Wishlist" tab's content to the left
- **THEN** the "Respond to Secret Santa" tab is shown and the tab row marks it

### Requirement: Gifter page
The Gifter page, the one for the relationship in which the person gives, SHALL open from the first row's pair link once the group is Active or Archived and SHALL be titled "Giving to <receiver username>". It SHALL show a tab row with two tabs, **<receiver username>'s Wishlist** and **Questions for <receiver username>**, and one tab at a time, which the person SHALL be able to change by choosing a tab or by swiping the content sideways, the tab row following the swipe. The first tab SHALL show the list the person's receiver keeps for the group, loaded with `GET /groups/{id}/receiver/wishlist`, read-only, in the receiver's order, with each item's title, note and link, or a short explanation that the receiver has not added anything yet. The second tab SHALL show the person's own questions to the receiver as `client-receiver-and-questions` describes, with the receiver's answers and, while the group is Active, the way to ask a new question. Nothing the person does on the page SHALL be visible to the receiver.

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

#### Scenario: Named after the receiver
- **WHEN** the person opens the Gifter page and their receiver is `bob`
- **THEN** the page is titled "Giving to bob" and its tabs read "bob's Wishlist" and "Questions for bob"

#### Scenario: Receiver not known yet
- **WHEN** the receiver's username has not loaded, or cannot be loaded
- **THEN** the page is titled "Giving to ..." and its tabs read "Wishlist" and "Questions", and they take the receiver's name once it loads

#### Scenario: Opened from an address
- **WHEN** the Gifter page is opened from its web address, with no earlier screen
- **THEN** the title and tabs show the fallback until the receiver loads and then name the receiver

#### Scenario: A long username
- **WHEN** the receiver's username is too long for its tab
- **THEN** the tab label stays on one line with an ellipsis, and a screen reader announces the whole label

#### Scenario: Swiping
- **WHEN** the person swipes the Wishlist tab's content to the left
- **THEN** the Questions tab is shown and the tab row marks it
