# Design

## Context

Content is already capped at 640dp, but by a separate `widthIn(max = 640.dp)` in each screen (`GroupsScreen`, `AdminGroupsScreen`, `AdminGroupScreen`, `AdminGroupFormScreen`, `AdminUsersScreen`, `AdminUserScreen`, `ProfileScreen`, `HeaderWithFooter`, `PullPage`). The `TopAppBar`, the `NavigationBar`, the tab row of the Gifter and Receiver pages and the `AppFab` (aligned to the bottom end of the box it is placed in, which is the whole page) ignore it. Login and password screens use their own 420dp and are outside the signed-in shell.

The participants are lines of text: `Participants` in `GroupPage.kt` (own name followed by "(you)") and `People` in `AdminGroupScreen.kt` (marks in brackets, "Read wishlist" text button). The pair rows use `UserButton` with `TextAlign.Start` for both names.

## Goals / Non-Goals

**Goals:**
- One width constant applied once in the shell, covering the bars, content and buttons.
- Chips for participants (user view, owner's Draft), rows with a user sheet and gift icon for the owner's Pending group.
- One `*` mark with one legend.
- The name after the arrow aligned to the end.

**Non-Goals:**
- Changing the width of sheets, dialogs, login or the password screen.
- Changing what the server returns or any API.
- Making chips tappable in the user view or in a Draft group.

## Decisions

1. **Cap in `ShellScreen`, not per screen.** Wrap the whole `Scaffold` in a `Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter)` and give the Scaffold `Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxSize()`; the area outside the column shows the theme background. `MAX_CONTENT_WIDTH` (640.dp) lives with the other layout constants and the per-screen `widthIn(max = 640.dp)` calls are removed, so the FABs, the tab row and the bars follow automatically. Alternative: cap each bar separately; rejected because it repeats the number and the FAB would still need its own rule. Sheets (`ModalBottomSheet`) are windows of their own and keep Material's default width.
2. **Pairs: end-aligned receiver.** `UserButton` takes the alignment; the right one uses `TextAlign.End` in `PairRows` and in `PairList`. The arrow keeps its place between the two weighted buttons.
3. **Chips.** A shared `ParticipantChips` composable in `ui/form` puts a `FlowRow` of chips either inside `ScrollingBox` (the owner's Draft group, `boxed = true`) or straight into the page (the participant's group page, `boxed = false`, where the page itself scrolls). The chips are not clickable; the earlier idea of Material `AssistChip`s (`onClick = {}`) was replaced by a plain `Surface` with the chip shape. A chip with `highlighted = true` uses `AssistChipDefaults.assistChipColors(containerColor = secondaryContainer)` with a primary-coloured label; others use the default outlined chip. Because the chips are not interactive, the alternative `SuggestionChip`/`InputChip` was not needed; if `AssistChip` exposes a click ripple the implementation uses a plain `Surface` with the chip shape instead.
4. **Marks.** `usernameLabel` already appends ` *` for incomplete profiles. The owner's participant model gets one flag `marked = !hasAccount || !profileComplete`, used by the Draft chips and the Pending rows; the legend string becomes `ADMIN_MARK_LEGEND = "* no account or profile incomplete"` for Draft/Pending, while the pair list keeps `ADMIN_INCOMPLETE_LEGEND`, since there every participant has an account. `ADMIN_PROFILE_INCOMPLETE`, `ADMIN_NO_ACCOUNT` and `ADMIN_OPEN_WISHLIST` text are replaced (the last stays as the icon's content description).
5. **Pending rows.** Each row is a `Row` with a `TextButton` for the username (opens `viewModel.openUserSheet(username)`, the sheet that the pair list already uses) and an `IconButton(GiftIcon)` for `openWishlistSheet`. For a participant without an account both are `enabled = false` and the row still shows the name and `*`.
6. **Own chip.** `ParticipantItem.isMe` already exists; the "(you)" text is dropped, and `Strings.YOU` stays for other uses.
7. **README.** The Draft/Pending/Active description and the layout notes are updated.

8. **The participant's group page scrolls as one.** `GroupPage` moves from `HeaderWithFooter` to `PullPage` (details, pair rows, then the chips in a column), so there is no inner scroll area and no minimum height. `HeaderWithFooter` stays for the owner's group page.

## Risks / Trade-offs

- A group with many participants makes the participant's page longer; the whole page scrolls, which is the intent.
- A capped Scaffold leaves bands on both sides of the bars on very wide windows; they show the plain background, which is what the user asked for.
- The sheets can be wider than the column on wide windows; accepted because Material's sheet width is its own cap.
- Highlighting by colour alone: the own chip is also announced via a content description ("you") so it does not rely on colour for assistive technology.
- `*` merges "no account" and "incomplete profile", so the owner can no longer tell them apart in the list; the user sheet still shows what is missing.
