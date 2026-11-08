# Spec Delta

## ADDED Requirements

### Requirement: The Receiver side is the person's wishlist and the questions from their Santa
The person's own wishlist and the questions from their Santa SHALL each be a page of their own, as `client-giver-receiver-pages` describes, opened from the "Edit Wishlist" button of the "Your stuff" card and, once the group is Active or Archived, from its "Respond to Santa" button. On a wide window the sidebar's Receiving item SHALL show them side by side with the Outbox. Nothing on them SHALL identify the Santa.

#### Scenario: Items in order
- **WHEN** the person has three items
- **THEN** the wishlist page shows them in the order the person chose, each with its title and, when present, its note and link

#### Scenario: Empty list
- **WHEN** the person has not added anything to this group
- **THEN** the wishlist page says so and offers to add the first item

#### Scenario: Separate per group
- **WHEN** the person is in two groups
- **THEN** each group's wishlist page shows only that group's items

#### Scenario: Pending group
- **WHEN** the group is Pending
- **THEN** the wishlist page, titled "Your Wishlist", is the only page, and there are no questions

#### Scenario: Active group
- **WHEN** the group is Active and the Santa has asked a question
- **THEN** the "Respond to Secret Santa" page shows the question with an "Answer" action

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the wishlist and the questions with the person's answers are shown on their pages and nothing can be changed

#### Scenario: Group phase not known yet
- **WHEN** the wishlist page is opened and the group's phase has not loaded
- **THEN** it is titled "Your Wishlist", shows the wishlist, and the questions page is offered once the phase is known

### Requirement: The Gifter side is the receiver's wishlist and the person's questions to them
The receiver's wishlist and the person's questions to them SHALL each be a page of their own, as `client-giver-receiver-pages` describes, opened from the "See Wishlist" and "Ask Questions" buttons of the "Secret Santa" card once the group is Active or Archived. On a wide window the sidebar's Giving item SHALL show them side by side with the Inbox, titled "Giving to <receiver username>". Nothing the person does there SHALL be visible to the receiver.

#### Scenario: Receiver's items
- **WHEN** the person opens the receiver's wishlist and the receiver has two items
- **THEN** the page shows both in the receiver's order with their note and link, and no control changes them

#### Scenario: Nothing added yet
- **WHEN** the receiver has not added anything
- **THEN** the wishlist page says the receiver has not added anything yet

#### Scenario: Questions and answers
- **WHEN** the person has asked two questions and the receiver answered the first
- **THEN** the questions page shows question 1 with its answer and question 2 with "Not answered yet"

#### Scenario: Ask from the page
- **WHEN** the group is Active and the person chooses to ask a question on the questions page
- **THEN** a bottom sheet opens, as `client-receiver-and-questions` describes

#### Scenario: Receiver not known yet
- **WHEN** the receiver's username has not loaded, or cannot be loaded
- **THEN** the pages are titled "Wishlist" and "Questions", and the Giving page "Giving to ...", and they take the receiver's name once it loads

#### Scenario: Opened from an address
- **WHEN** the receiver's wishlist is opened from its web address, with no earlier screen
- **THEN** the title shows the fallback until the receiver loads and then names the receiver

#### Scenario: A long username
- **WHEN** the receiver's username is too long for the title
- **THEN** the title stays on one line with an ellipsis, and a screen reader announces the whole title

### Requirement: The pages load, fail and refresh on their own
Each of the pages, and each section of a Giving or Receiving page on a wide window, SHALL load on its own, show that it is loading, and show a message with "Try again" when the server cannot be reached or answers with an error, without affecting another page or what it already showed.

#### Scenario: One page fails
- **WHEN** the questions cannot be loaded and the wishlist can
- **THEN** the wishlist page shows the list and the questions page shows the problem with "Try again"

#### Scenario: Pull to refresh
- **WHEN** the person pulls a page down on Android or iOS
- **THEN** that page reloads and what is shown stays until the new data arrives

#### Scenario: Returning to a page
- **WHEN** the person opens a page they have not looked at for a while
- **THEN** its content reloads without clearing what it showed

## REMOVED Requirements

### Requirement: Receiver page
**Reason**: The wishlist and the questions are no longer two tabs of one page; the Receiver side is described by its own requirement.
**Migration**: See "The Receiver side is the person's wishlist and the questions from their Santa" and `client-giver-receiver-pages`.

### Requirement: Gifter page
**Reason**: The wishlist and the questions are no longer two tabs of one page; the Gifter side is described by its own requirement.
**Migration**: See "The Gifter side is the receiver's wishlist and the person's questions to them" and `client-giver-receiver-pages`.

### Requirement: Pages can open on a tab
**Reason**: There are no tabs; each of the wishlist and the questions has a page and an address of its own.
**Migration**: Open `giver/wishlist`, `giver/questions`, `receiver/wishlist` or `receiver/questions`.

### Requirement: The Receiver page of a drawn group has two tabs
**Reason**: The two tabs are two pages.
**Migration**: The "Your Wishlist" page and the "Respond to Secret Santa" page.

### Requirement: The Receiver page's wishlist tab
**Reason**: The wishlist is a page of its own.
**Migration**: The "Your Wishlist" page.

### Requirement: The Receiver page's questions tab
**Reason**: The questions are a page of their own.
**Migration**: The "Respond to Secret Santa" page.

### Requirement: A pending group's Receiver page has no tabs
**Reason**: There are no tabs; a Pending group has only the wishlist page.
**Migration**: See "A Pending group has its own wishlist only" in `client-giver-receiver-pages`.

### Requirement: The Gifter page has two tabs
**Reason**: The two tabs are two pages.
**Migration**: The "<receiver username>'s Wishlist" page and the "Questions for <receiver username>" page.

### Requirement: The Gifter page's wishlist tab
**Reason**: The receiver's wishlist is a page of its own.
**Migration**: The "<receiver username>'s Wishlist" page.

### Requirement: The Gifter page's questions tab
**Reason**: The questions are a page of their own.
**Migration**: The "Questions for <receiver username>" page.

### Requirement: Tabs reload
**Reason**: There are no tabs to reload; each page reloads as pages do.
**Migration**: See "The pages load, fail and refresh on their own".

### Requirement: Page loading and refreshing
**Reason**: It spoke of the tabs of the Gifter and Receiver pages.
**Migration**: See "The pages load, fail and refresh on their own".
