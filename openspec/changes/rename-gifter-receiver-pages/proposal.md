# Proposal

## Why

The Gifter and Receiver pages are titled "Gifter" and "Receiver" and both have the same two tabs, "Wishlist" and "Questions". Nothing on either page says whose wishlist it is or who the questions are for or from, so the person has to remember which relationship they opened. Naming the pages after the relationship makes that clear at a glance.

## What Changes

- The Gifter page is titled "Giving to <receiver username>". Its tabs are "<receiver username>'s Wishlist" and "Questions for <receiver username>".
- The Receiver page is titled "Receiving from Secret Santa" once the group is Active or Archived. Its tabs are "Your Wishlist" and "Respond to Secret Santa".
- While the group is Pending the Receiver page has no tabs and is titled "Your Wishlist".
- The Gifter page falls back to "Giving to ..." with plain "Wishlist" and "Questions" tabs until the receiver's username is known, or if it cannot be loaded.
- Long usernames do not break the tab row: a label stays on one line with an ellipsis and a screen reader still announces it whole.
- The pair links on the group page stay "Gifter" and "Receiver", and so do their screen reader labels. They stop sharing strings with the page titles.
- The pages' routes, web addresses and content are unchanged. Nothing on the Receiver page identifies the Santa; "Secret Santa" in its title is the role, not a person.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-group-pages`: the Gifter page and Receiver page requirements gain the new titles and tab names, the Pending title, the Gifter fallback and the long-name behaviour.

## Impact

- `app/shared/src/ui/shell/ShellScreen.kt`: the top-bar title for the two pages comes from the page, not from the route alone.
- `app/shared/src/ui/groups/GroupPages.kt`: `TabbedPages` takes its two tab labels from the page; `GifterPage` and `ReceiverPage` supply their titles and labels.
- `app/shared/src/ui/Strings.kt`: new title and tab strings and functions that build them from a username; `TITLE_GIFTER` and `TITLE_RECEIVER` remain for the pair links.
- Tests for the string builders, and any existing test that expects the old titles or tab names.
- No server, contract or domain change.
