# Proposal

## Why

A participant's group screen is a plain list of entries: a Wishlists section, a Your gift section with three more entries, and a list of every participant. Understanding who gives to whom and where to act on it takes five separate screens ("My wishlist", "Your receiver's wishlist", "Your receiver", "Questions from your Santa", "Your Santa"). The owner's view of a drawn group is already a compact pair list; the participant should see the same idea from their own side: me, my receiver, my Santa, and one page for each of the two relationships.

## What Changes

- **Pair rows on the group page.** While the group is Pending the page shows the person's own username with a gift icon that opens their wishlist. Once it is Active or Archived it shows two rows: `me -> receiver` and `Secret Santa -> me`. Tapping a username opens a sheet with that person's details; the arrow of the first row opens the **Gifter page**, the arrow of the second row (and the gift icon while Pending) opens the **Receiver page**.
- **Details sheets.** My username opens my own details; the receiver's username opens their details (name, address, phone and delivery information, as "Your receiver" shows today). "Secret Santa" is not tappable until the group is Archived, when it opens the Santa's username and full name.
- **Gifter page** (I am the gifter): the receiver's wishlist, read-only, then my questions to the receiver with their answers, and asking a new one while the group is Active.
- **Receiver page** (I am the receiver): my own wishlist, editable as before, then, once the names are drawn, the questions from my Santa with answering. While Pending it has the wishlist only.
- **Participants list moves down.** The list of every participant stays, as a small scrolling list at the end of the group page, for anyone who wants to see who is in the group.
- **Five screens go.** "My wishlist", "Your receiver's wishlist", "Your receiver", "Questions from your Santa" and "Your Santa" no longer exist as screens or entries; their content is in the two pages and the sheets. Adding, editing, deleting, reordering, asking, answering, validation and read-only-once-archived behave as before.
- **Web addresses.** `#/groups/{id}/gifter` and `#/groups/{id}/receiver` (the latter now means the Receiver page); the addresses of the removed screens (`/wishlist`, `/receiver/wishlist`, `/questions`, `/santa`) are no longer known and show the first screen.

Out of scope:
- The server and its API (every request already exists), the owner's and administrator's screens, and changing what a participant is allowed to see (nothing new is revealed: the Santa stays unnamed until the group is Archived).

## Capabilities

### New Capabilities
- `client-group-pages`: the pair rows, the details sheets, the Gifter page and the Receiver page.

### Modified Capabilities
- `client-groups`: the group detail becomes the group page (pair rows and a participants list at the end).
- `client-wishlist`: the two wishlist screens and their entries are replaced by sections of the pages; the archived and loading requirements name the new places.
- `client-receiver-and-questions`: the receiver and Santa screens are replaced by sheets and page sections; the questions lists and loading requirement name the new places.
- `client-app`: the readable web addresses and their examples.

## Impact

- Code: `GroupDetailScreen` becomes the group page with pair rows and sheets; the wishlist, receiver-wishlist, receiver, Santa-questions and Santa-reveal screens are split into sections used by two new page screens; `Routes.kt`, `WebAddresses`, the shell's titles and back handling, strings, a hand-drawn gift icon; README. Tests: `WebAddressesTest`, a view model test for the receiver row, the existing view model tests keep applying.
- APIs: none. Dependencies: none.
- Platforms: Android, iOS and web.
