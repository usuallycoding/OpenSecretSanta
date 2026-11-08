# Proposal

## Why

On the owner's group screen the rules and a pair open differently depending on the window: on a wide window they are state of the screen, so the web address does not change (the rules even redirect away from `/rules`), and narrowing the window with the rules open turns them into a bottom dialog; on a narrow window the pair is a bottom dialog with no address. The Announcements already solved this by making the pane a route, which the address follows and which becomes a page or a pane by the window's width alone.

## What Changes

- The gavel opens the rules at `#/admin/groups/{id}/rules` on every window: a page with the back arrow on a narrow one, the group page with the rules in a pane on a wide one, with the address staying `/rules`. Resizing switches between the two and never shows a dialog.
- The pair link (the gift icon) in the pair list opens the pair at the new address `#/admin/groups/{id}/pairs/{santa}/{receiver}` on every window: a page titled with the pair on a narrow one (today a bottom dialog), a pane beside the group page on a wide one.
- Opening one of these panes replaces another one, the Announcements included, and the participant wishlist pane, which stays as it is, replaces and is replaced by them. Closing returns to the group's own address.
- The rules and pair entries leave the screen's `GroupSheet` state; the user sheet and the participant wishlist button are unchanged.
- An address the group's phase does not allow (rules of a Draft group, a pair that is not in the draw, a pair before the draw) opens the group's page.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-draw-rules`: the rules are a page or a pane by the window's width, both at the rules' address, and never a dialog.
- `client-admin-oversight`: the pair opens as a page on a narrow window instead of a bottom sheet, and as a pane on a wide one, each at its own address.
- `client-app`: the rules address stays on a wide window; a new address for a pair; Back closes only the user sheet now.

## Impact

- `app/shared/src/ui/admin` (`AdminGroupScreen`, `AdminGroupViewModel`, `OversightSheets`, `OversightModels`), `navigation` (`Routes`, `WebAddresses`), `shell/ShellScreen.kt` and its route lists (below-a-group screens, title, sidebar).
- No server, notification or link-intent changes.
- Out of scope: the user sheet, the participant wishlist button (still a state pane on a wide window and a bottom sheet on a narrow one), and the participant side.
