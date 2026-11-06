# Proposal

## Why

On a tablet or a desktop browser the signed-in app is one 640dp column with a phone's bottom bar and floating buttons, centered in an empty window. Wide windows have room for what phones have to hide: where you are in a group, the actions of the page you are on, and a wishlist next to the list it came from.

## What Changes

- On a window 840dp wide or wider the bottom bar becomes a permanent sidebar; narrower windows keep today's layout, so phones and landscape phones do not change.
- The participant sidebar lists Groups and Settings. The open group is listed under Groups, expanded into Overview, Giving and Receiving; other groups are one line each. Giving holds the receiver's wishlist, the questions for the receiver and the Inbox. Receiving holds the person's own wishlist, Respond to Secret Santa and the Outbox. Items that exist only after the draw appear only then.
- The admin sidebar lists Groups, Users and Settings, with no group tree. The view switch stays in the top bar.
- On wide windows the Gifter and Receiver pages no longer show their tab row or swipe: the sidebar chooses what they show.
- On wide windows the floating buttons (add a wishlist item, ask a question, write a letter, new group) become buttons in the top bar. Narrow windows keep the floating buttons.
- In a Pending group the gift icon on an owner's participant row opens the person's wishlist in a pane beside the group page instead of a bottom sheet.
- On wide windows every other bottom sheet is shown as a centered, width-limited dialog with the same content.
- The 640dp column rule is replaced: content keeps a readable width inside the content area, next to the sidebar.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-layout`: the one centered column becomes two layouts, the narrow one as today and the wide one with a sidebar, a content area with a readable width and a top bar that carries the page's buttons.
- `client-app`: the views by role scenario that names a bottom bar, and the floating-buttons requirement, which become top-bar buttons on wide windows.
- `client-settings`: the Settings item is in the bottom bar on narrow windows and in the sidebar on wide ones.
- `client-group-pages`: the Gifter and Receiver pages' two tabs become sidebar entries on wide windows, and Inbox and Outbox join the sidebar's tree.
- `client-admin-oversight`: a participant's wishlist opens in a pane beside the group page on wide windows.

## Impact

- Client UI only (`app/shared/src/ui`): `shell/ShellScreen.kt` (sidebar, top bar actions, breakpoint), `groups/GroupPages.kt` (the tab pager), `form/AppFab.kt` and its four call sites, `form/SheetSupport.kt` (`AppBottomSheet` as a dialog), `admin/AdminGroupScreen.kt` and `admin/OversightSheets.kt` (the wishlist pane), plus shared state for the group list, the open group's phase, the receiver's name and the badge counts the sidebar needs.
- No server, API, data-layer or route changes; web addresses and deep links stay as they are.
- Strings gain the sidebar's labels where the tab labels cannot be reused.
- Android, iOS and the web app each need checking at both widths, including resizing across the breakpoint.
