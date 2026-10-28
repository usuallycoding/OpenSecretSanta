# Proposal

## Why

On the web and other big screens the content is kept to 640dp, but the top bar, the bottom tab bar, the tab row of the Gifter and Receiver pages and the floating buttons still use the whole window, so the app looks stretched and the buttons float far from the content. The participants lists are plain lines of text with wordy marks ("(profile incomplete)", "Read wishlist"), and in the `me -> receiver` rows both names start at the left, so the receiver does not read as the other end of the arrow.

## What Changes

- One standard maximum width for the whole signed-in app on wide screens, applied once around the top bar, content, floating buttons and bottom bar, so all of them line up and the same width is not repeated per screen.
- Active and Archived rows (`me -> receiver`, `Secret Santa -> me` for participants, and the admin pair list) align the name after the arrow to the end of the row.
- A participant's group page shows the participants as chips directly on the page, with no box and no inner scroll, so the page scrolls as a whole when the group is large; the person's own chip is highlighted instead of being followed by "(you)".
- An owner's Draft group shows the participants as chips in the scrolling box. A participant without an account, or whose profile lacks a name or an address, gets a single `*` after the username, and one legend "* no account or profile incomplete" is shown under the list when any item has the mark.
- An owner's Pending group keeps plain rows (not chips): choosing the username opens the existing user details sheet, and a gift icon at the end of the row replaces the "Read wishlist" text and opens the participant's wishlist. A participant without an account keeps the name and `*`, with the tap and the gift icon disabled.
- **BREAKING (wording only)**: the marks "no account yet" and "profile incomplete" on participants of a Draft or Pending group are replaced by `*`; "Read wishlist" becomes an icon.

## Capabilities

### New Capabilities

- `client-layout`: the shared maximum width of the signed-in app on wide screens.

### Modified Capabilities

- `client-groups`: the participant's group page lists participants as highlighted-own chips.
- `client-group-pages`: the receiver's name in the pair rows is aligned to the end.
- `client-admin-groups`: Draft participants as chips, Pending participants as tappable rows with a gift icon, the single `*` mark with its legend.
- `client-admin-oversight`: the receiver's name in the pair list is aligned to the end; the participant wishlist opens from the gift icon; the user sheet also opens from a Pending participant.

## Impact

- `app/shared/src/ui/shell/ShellScreen.kt` (the cap around the Scaffold), and the per-screen `widthIn(max = 640.dp)` in `GroupsScreen`, `AdminGroupsScreen`, `AdminGroupScreen`, `AdminGroupFormScreen`, `AdminUsersScreen`, `AdminUserScreen`, `ProfileScreen`, `HeaderWithFooter` and `PullPage`.
- `app/shared/src/ui/groups/GroupPage.kt` (now a `PullPage`), `PairRows.kt`; `app/shared/src/ui/admin/AdminGroupScreen.kt`, `OversightSheets.kt`; `Strings.kt`; README.
- No server, API or data changes.
