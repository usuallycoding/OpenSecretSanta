# client-giver-receiver-pages Specification

## Purpose
The six pages of a drawn group's gift exchange on a small screen, the wishlists, the questions, the Inbox and the Outbox, each a page of its own with its own title and web address, and what a Pending group has of them.

## Requirements

### Requirement: The wishlists, the questions and the letters are pages of their own
On a small screen the receiver's wishlist, the person's questions to the receiver, the Inbox, the person's own wishlist, the questions from their Santa and the Outbox SHALL each be a page of its own, with no tab row, a title of its own and the back arrow in the top bar. A title that names the receiver SHALL read without the name until the receiver's username is known.

#### Scenario: The receiver's wishlist
- **WHEN** the person opens the receiver's wishlist, and their receiver is `bob`
- **THEN** a page titled "bob's Wishlist" shows it, with no tab row and the back arrow

#### Scenario: The questions
- **WHEN** the person opens their questions to `bob`
- **THEN** a page titled "Questions for bob" shows them, with no tab row

#### Scenario: Their own side
- **WHEN** the person opens their own wishlist, the questions from their Santa or the Outbox
- **THEN** each is a page titled "Your Wishlist", "Respond to Secret Santa" or "Letters to your Santa"

#### Scenario: The receiver is not known yet
- **WHEN** the receiver's username has not loaded
- **THEN** the page is titled "Wishlist", "Questions" or "Letters from your receiver", and takes the name once it loads

#### Scenario: Nothing to swipe
- **WHEN** the person swipes sideways on one of the pages
- **THEN** no other page opens

### Requirement: Each page has its own web address
On the web the six pages SHALL have the addresses `#/groups/{id}/giver/wishlist`, `#/groups/{id}/giver/questions`, `#/groups/{id}/inbox`, `#/groups/{id}/receiver/wishlist`, `#/groups/{id}/receiver/questions` and `#/groups/{id}/outbox`, which show the page when they are opened, refreshed or typed, and which the address bar shows while the page is open. The group's page SHALL be behind the page.

#### Scenario: Each address
- **WHEN** a signed-in person opens `#/groups/2/giver/questions` on a phone
- **THEN** group 2's questions page opens, with group 2's page and then the group list behind it

#### Scenario: Both wishlists
- **WHEN** a signed-in person opens `#/groups/2/giver/wishlist` or `#/groups/2/receiver/wishlist`
- **THEN** the receiver's wishlist or the person's own wishlist opens

#### Scenario: Refresh
- **WHEN** the person refreshes the web page on `#/groups/2/outbox`
- **THEN** the Outbox of group 2 is shown

#### Scenario: The address follows the page
- **WHEN** the person opens the Inbox from the group page and then goes back
- **THEN** the address is `#/groups/2/inbox` on the Inbox and `#/groups/2` again on the group page

### Requirement: The addresses without a suffix
`#/groups/{id}/giver` and `#/groups/{id}/receiver` SHALL show, on a wide window, the Giving and the Receiving page with their three sections side by side, and, on a small screen, the receiver's wishlist page and the person's own wishlist page, which is what they opened before the tabs were split.

#### Scenario: A small screen
- **WHEN** a person opens `#/groups/2/giver` on a phone
- **THEN** the receiver's wishlist of group 2 opens, and its address is `#/groups/2/giver/wishlist`

#### Scenario: A small screen, the receiving side
- **WHEN** a person opens `#/groups/2/receiver` on a phone
- **THEN** their own wishlist opens, and its address is `#/groups/2/receiver/wishlist`

#### Scenario: A wide window
- **WHEN** a person opens `#/groups/2/giver` on a wide window
- **THEN** the Giving page opens with the receiver's wishlist, the questions and the Inbox side by side, and the address stays

### Requirement: A Pending group has its own wishlist only
While a group is Pending the person's own wishlist SHALL be the only one of the six pages that has anything to show. `receiver/questions` SHALL show it, and `giver/wishlist`, `giver/questions`, `inbox` and `outbox` SHALL show the group's page, because there is no receiver, no question and no letter before the names are drawn.

#### Scenario: Questions of a Pending group
- **WHEN** a person opens `#/groups/2/receiver/questions` while the group is Pending
- **THEN** their own wishlist is shown

#### Scenario: The receiver's side of a Pending group
- **WHEN** a person opens `#/groups/2/giver/wishlist`, `#/groups/2/inbox` or `#/groups/2/outbox` while the group is Pending
- **THEN** the group's page is shown and no page opens

#### Scenario: Drawn groups
- **WHEN** the group is Active or Archived
- **THEN** all six addresses show their own page

### Requirement: What was said of the Gifter and Receiver pages carries over
Wherever another requirement speaks of the Gifter page, the Receiver page, their wishlist or questions tabs, the Inbox page or the Send letter page, it SHALL apply to the page, the pane or the column that shows the same thing, with the page's own title and without a tab row.

#### Scenario: The receiver's list
- **WHEN** another requirement says the Gifter page shows the receiver's wishlist in the receiver's order
- **THEN** the receiver's wishlist page, pane and column show it that way

#### Scenario: Asking a question
- **WHEN** another requirement says the person can ask a question on the Gifter page
- **THEN** the questions page, pane and column offer it
