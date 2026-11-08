# Spec Delta

## Purpose

How the group page opens its wishlists, questions, Inbox, Outbox and announcements in a pane beside it on a wide window, what replaces and closes the pane, and how the web address follows it.

## ADDED Requirements

### Requirement: The group page's buttons open a pane on a wide window
On a wide window the buttons of the group page's two cards that open the receiver's wishlist, the person's questions to the receiver, the Inbox, the person's own wishlist, the questions from their Santa and the Outbox SHALL open that page in a pane to the right of the group page, as the Announcements bell opens the announcements, leaving the group page as it is. On a small screen they SHALL open the page.

#### Scenario: A button opens its pane
- **WHEN** the person chooses "See Wishlist" on a wide window
- **THEN** the receiver's wishlist opens in a pane to the right of the group page, and the group page stays

#### Scenario: Each button has its own page
- **WHEN** the person chooses, in turn, Ask Questions, Inbox, Edit Wishlist, Respond to Santa and Send letter
- **THEN** the pane shows the questions to the receiver, the Inbox, the person's wishlist, the questions from the Santa and the Outbox

#### Scenario: A small screen
- **WHEN** the person chooses one of the buttons on a phone
- **THEN** the page opens as a screen of its own and Back shows the group page

### Requirement: The pane and the group page share the width equally
While a pane is open on a wide window the group page and the pane SHALL be equally wide, each taking half of the area beside the sidebar, with a divider between them. With no pane open the group page SHALL be centered at its readable width.

#### Scenario: A pane opens
- **WHEN** the person opens a page in the pane on a wide window
- **THEN** the group page and the pane are the same width

#### Scenario: The pane is closed
- **WHEN** the pane is closed
- **THEN** the group page is centered at its readable width again

### Requirement: The pane has a heading with the page's button and a way to close it
The pane SHALL have a heading with the page's title, the page's own button where it has one, such as "Add item", "Ask a question" or "Send letter", as a button in place of the floating button, and a close icon. The announcements pane SHALL have the same heading.

#### Scenario: A button in the heading
- **WHEN** the person's own wishlist is open in the pane
- **THEN** the heading says "Your Wishlist" and has the button to add an item, and no floating button is drawn

#### Scenario: A page with no button
- **WHEN** the receiver's wishlist is open in the pane
- **THEN** the heading has the title and the close icon and no other button

#### Scenario: Closing
- **WHEN** the person chooses the close icon
- **THEN** the pane is gone and the group page is shown alone, centered at its readable width

### Requirement: One pane at a time, and a new one replaces it
Only one pane SHALL be open. Opening another page, or the announcements, while a pane is open SHALL replace it, so that going back, and the close icon, always return to the group page and never to the pane that was replaced. Choosing the button of the pane that is open SHALL change nothing.

#### Scenario: Replaced
- **WHEN** the wishlist pane is open and the person chooses Inbox
- **THEN** the Inbox is in the pane, and the wishlist is not

#### Scenario: Back closes it
- **WHEN** the person has opened the wishlist, then the Inbox, and goes back
- **THEN** the group page is shown alone

#### Scenario: The same button again
- **WHEN** the Inbox is open and the person chooses the Inbox button again
- **THEN** the pane stays as it is, and going back once closes it

#### Scenario: The bell replaces it too
- **WHEN** a page is open in the pane and the person chooses the Announcements bell
- **THEN** the announcements are in the pane

### Requirement: The address follows the pane
While a pane is open on the web the address SHALL be the open page's own address, `#/groups/{id}/giver/wishlist`, `#/groups/{id}/giver/questions`, `#/groups/{id}/inbox`, `#/groups/{id}/receiver/wishlist`, `#/groups/{id}/receiver/questions` or `#/groups/{id}/outbox`, and the group's own address `#/groups/{id}` once it is closed. The browser's back button SHALL close the pane.

#### Scenario: Opening and closing
- **WHEN** a person on a wide window opens the Outbox of group 7 from the group page and then closes it
- **THEN** the address is `#/groups/7/outbox` while it is open and `#/groups/7` once it is closed

#### Scenario: Browser back
- **WHEN** the pane is open and the person uses the browser's back button
- **THEN** the group page is shown alone

### Requirement: An address, a link or a notification opens the pane
On a wide window each of the six addresses SHALL show the group's page with that page in the pane, whether it is typed, refreshed, opened from a link or opened by tapping a notification, with the group list behind the group page, and the address SHALL stay. `giver` and `receiver` without a suffix SHALL NOT open a pane.

#### Scenario: A typed address
- **WHEN** a person on a wide window opens `#/groups/7/receiver/questions`
- **THEN** group 7's page is shown with the questions from the Santa in the pane, and the address is still `#/groups/7/receiver/questions`

#### Scenario: Each of the six
- **WHEN** a person on a wide window opens `giver/wishlist`, `giver/questions`, `inbox`, `receiver/wishlist` or `outbox` of a group
- **THEN** the group's page is shown with that page in the pane

#### Scenario: A notification
- **WHEN** a person on a wide window taps the notification of a question from their Santa
- **THEN** the group's page is shown with the questions from the Santa in the pane

#### Scenario: Back from a pane opened by an address
- **WHEN** a person opens an address with no earlier screen and closes the pane
- **THEN** the group's page is shown alone, and not the group list

### Requirement: A pane marks what it shows as seen
A page in the pane SHALL be marked and counted as the same page is on a small screen: what is new carries its pill and bar while the pane is open, the newest time shown is sent as seen once the list is loaded, and the badges on the group page's buttons are gone once the pane has been closed and the group's data has been loaded again.

#### Scenario: Marks in the pane
- **WHEN** the person opens the Inbox in the pane with two new letters
- **THEN** the letters carry the pill and the bar, and after the pane is closed and opened again they do not

#### Scenario: The badge after closing
- **WHEN** the person closes the Inbox pane
- **THEN** the Inbox button's badge on the group page is gone
