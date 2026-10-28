# Proposal

## Why

A group's name can be 100 characters, so on its page and in the lists it wraps over several rows and pushes everything down. Text areas and the cut of long read-only text allow six rows, which is a lot on a phone. The buttons that add something ("Add item", "Ask a question", "New group") are wide buttons inside the content, taking a row of their own. And the Wishlist and Questions tabs can only be changed by tapping the tab row, where on a phone people expect to swipe.

## What Changes

- **Group titles are one row.** On the group page (participant's and owner's) and in both groups lists the name is shown on one row with an ellipsis. On the group page, tapping a name that was cut opens a dialog with the whole name (with the copy icon); in the lists it is not tappable on its own, because tapping the card opens the group.
- **Three rows instead of six.** Multi-line text fields grow to three rows and then scroll inside themselves, and long read-only text is cut at three lines (still opening in full when tapped), so every place that said six now says three.
- **Floating buttons.** "Add item" (wishlist), "Ask a question" (Gifter's Questions tab) and "New group" (owner's groups list) become floating action buttons at the bottom end of the screen, with an icon and an accessible name instead of a wide button with a label. The explanations that replace them when adding is not possible ("list is full", "closed", "limit reached") stay as text.
- **Swipe between tabs.** On the Gifter and Receiver pages the Wishlist and Questions tabs can also be changed by swiping the content sideways; the tab row follows, and tapping a tab still works.
- **Unchanged.** The top bar of the group pages (it keeps saying "Group"), what each action does, the sheets, the confirmations and the web addresses.

Out of scope:
- Changing group names' length limit, the top bar title (it stays "Group"), titles of other screens, the participant and owner group lists' content, an extended (labelled) floating button, and shortcuts for the floating buttons.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `client-app`: three rows instead of six; the floating action buttons.
- `client-groups`: one-row names in the groups list and on the group page, with the full-name dialog.
- `client-admin-groups`: the owner's groups list (one-row names, the floating "New group") and the group detail's title.
- `client-wishlist`: adding an item is a floating button.
- `client-receiver-and-questions`: asking a question is a floating button.
- `client-group-pages`: the two pages' tabs can be swiped.

## Impact

- Code: `MULTILINE_MAX_ROWS` and its users (`FormComponents`, `ReadOnlyText`); a one-row title with the full-text dialog (`ReadOnlyText` gets a line limit and an option for no copy icon); one-row names in `GroupsScreen` and `AdminGroupsScreen`; a shared floating button and two drawn or core icons (`ui/form`), used by `WishlistSection`, the questions section and `AdminGroupsScreen`; a `HorizontalPager` in `GroupPages`; strings; README. No server, API or dependency change.
- Platforms: Android, iOS and web.
