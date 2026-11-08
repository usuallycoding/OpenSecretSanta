# Spec Delta

## MODIFIED Requirements

### Requirement: Giving and Receiving side by side
On a wide window the sidebar's Giving item SHALL open one page that shows, side by side and each under its own heading, "<receiver username>'s Wishlist", "Questions for <receiver username>" and the Inbox, and the Receiving item one page that shows "Your Wishlist", "Respond to Secret Santa" and the Outbox. Each column SHALL scroll and reload by itself and SHALL keep a readable width, and when all of them cannot, the page SHALL scroll sideways. A Pending group's Receiving page SHALL show only the wishlist, at the standard width. Only the sidebar and the addresses `#/groups/{id}/giver` and `#/groups/{id}/receiver` open these pages: the group page's buttons open a pane.

#### Scenario: Giving page
- **WHEN** the person chooses Giving for a group whose receiver is `alice` on a wide window
- **THEN** "alice's Wishlist", "Questions for alice" and the Inbox are shown beside each other, and the page is titled "Giving to alice"

#### Scenario: Receiving page
- **WHEN** the person chooses Receiving on a wide window
- **THEN** "Your Wishlist", "Respond to Secret Santa" and the Outbox are shown beside each other

#### Scenario: Not enough room
- **WHEN** the area beside the sidebar is narrower than three columns of 300dp
- **THEN** the columns keep their width and the page scrolls sideways

#### Scenario: Pending group
- **WHEN** the person chooses Receiving in a Pending group
- **THEN** only the wishlist is shown, centered at the standard width

#### Scenario: Buttons to open the Inbox and the Outbox
- **WHEN** the person chooses the Inbox or the "Send letter" button on a group's Overview on a wide window
- **THEN** the Inbox or the Outbox opens in a pane beside the group page, as `client-group-panes` describes, and not the Giving or the Receiving page
