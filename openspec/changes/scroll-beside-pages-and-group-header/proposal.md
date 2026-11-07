# Proposal

## Why

Three small fixes to how pages look and behave on wider displays and on a group's page:
- On a wide window a page keeps a readable width and the rest is empty space, but only the page itself scrolls. A mouse wheel or a drag in the empty space beside it does nothing, which feels broken.
- The group's description is plain text above the information card; it should sit in a card of its own like the other facts.
- The group's name sits at the start of the row; it should be centered.

## What Changes

- **Scrolling beside a page.** Scrolling by the mouse wheel, trackpad or a touch drag in the empty space beside a page scrolls that page, on a wide window and between the standard width (640dp) and the wide breakpoint (840dp), where the bars and the page form a centered column. Over the page nothing changes; pages that fill the window are unaffected.
- **The description in a card of its own.** On the group page and on the owner's group screen the description is shown in its own card, above the card with the date and time, location and budget. A group with no description shows no description card. This reverses the earlier requirement that the description is plain text and not in a card.
- **The group name is centered horizontally** on the group page and on the owner's group screen. The state and its note under it are not changed (assumption: they stay where they are).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-layout`: a new requirement, "Scrolling works beside a page".
- `client-groups`: "Group page" says the name is centered, "Group information is shown in a card" says the description is in a card of its own above it, and "The description is shown outside the card" is replaced by "The description is shown in a card of its own".
- `client-admin-groups`: "Admin group details are shown in a card" says the same for the owner's screen: the name centered and the description in a card of its own.

## Impact

- `app/shared/src/ui/layout` (a small host for the scroll to forward, and the registration the scrollers call), `ShellScreen.kt` (the scroll handler on the full-width area around the pages), and one registration line in each scroller: `PullPage`, the lists of groups, admin groups and users, Settings, a user's page and the rules page.
- `GroupInfoCard.kt` (the description card), and the two headers (`GroupPage.kt`, `AdminGroupScreen.kt`) for the centered name.
- No server, contract, data or setting change.
