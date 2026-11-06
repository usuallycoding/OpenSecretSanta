# Design

## Context

See proposal.md for the motivation. Today `ShellScreen` is one `Scaffold` (top bar, bottom bar, `NavHost`) inside a box that caps it at 640dp. Each screen is a route; Gifter and Receiver are one route each with two tabs held by a `HorizontalPager` (`TabbedPages`), and the route's `questions` flag chooses the tab it opens on. Inbox and Outbox (`SendLetterRoute`) are their own routes. The four floating buttons are drawn inside their screens through `AppFab`. Pages already tell the top bar their title through `PageTitle`/`onTitle`. Every bottom sheet is an `AppBottomSheet`. The owner's gift icon sets a `GroupSheet.Wishlist` state in `AdminGroupViewModel`, which `GroupSheetHost` renders.

The sidebar's items map one-to-one onto routes and tabs that exist, so the nav graph, the web addresses and the view models keep their shape.

## Goals / Non-Goals

**Goals:**
- One width test, 840dp, decides between today's layout and the wide one; below it nothing changes.
- The wide layout is a different frame around the same routes, so resizing across the breakpoint keeps the open page.

**Non-Goals:**
- No icon-only rail for 600-840dp, no list-detail layout for the participant pages, no new routes or web addresses.
- No side panes for the owner's user, pair and participants sheets; they become dialogs.

## Decisions

**Measure the window, not the device.** The shell reads the width of the space it is given (`BoxWithConstraints`) and compares it with one `WIDE_BREAKPOINT` constant next to `MAX_CONTENT_WIDTH`. A browser window, a tablet in split screen and a rotated phone all behave. A library for window size classes is not added: it would be a new dependency for one comparison, and the platforms' versions differ.

**Keep one `NavHost`, change only the chrome.** On wide windows the frame is a `Row` of sidebar and the existing `Scaffold` content; the `NavHost` is the same composable in both. This is what makes resizing safe, because the back stack and the screens' state are not rebuilt. The alternative, a separate wide shell, would duplicate the route table.

**The sidebar reads the same state as the pages.** It needs the group list and each group's phase, and the badges (unread, unanswered questions) the cards already show. The group list view model moves up to the shell, so the Groups screen and the sidebar read one list, and a group's phase comes from its row. The open group's badge counts come from two view models of the shell's own, refreshed when the open screen changes.

**Giving and Receiving are one page each on wide windows.** The sidebar's Giving and Receiving items open the existing Gifter and Receiver routes, so the web addresses do not change. On a wide window those pages replace the tab row and pager with `WideColumns`: the sections side by side, each under its heading and with its own `PullPage`, so each scrolls and reloads by itself. The Inbox and Outbox, which are their own routes on narrow windows, become the third column, from content functions extracted from their screens. The view models for them are created only on wide windows. The tabs stay for narrow windows.

**Floating buttons register themselves in the top bar.** On wide windows the shell provides a host through a composition local, and `AppFab` hands its action (icon, name, click) to it instead of drawing a floating button, so the four call sites need no change. On the Giving and Receiving pages each column has its own host and draws its actions at the end of its heading; elsewhere the shell's host puts them in the top bar, as an icon with its name. A button that is not composed is not registered, so an unavailable action leaves no button, and `DisposableEffect` removes it when its column leaves. Alternatives: each page draws its own top-bar row (breaks the single top bar), or the shell asks the view models (couples it to every screen).

**Sheets switch form inside `AppBottomSheet`.** All sheets go through it, so on wide windows it shows a centered dialog with a width cap and the same content and close paths, and the keyboard rules carry over (the dialog is a window of its own, as the sheet is). One change in one place; the sheets' callers do not change.

**The owner's wishlist pane is a second column in `AdminGroupScreen`.** `GroupSheet.Wishlist` already holds who is open. On wide windows `GroupSheetHost` skips that sheet kind and the screen lays out the group page and a wishlist pane side by side, sharing the content composable the sheet uses. The pane has its own close control, and back closes it before leaving the page. The other sheet kinds go through `AppBottomSheet` and so become dialogs.

**Top bar.** It uses the center-aligned top app bar on every screen, so titles are centered on narrow and wide windows alike.

**Width inside the content area.** The 640dp cap moves from the whole shell to the content beside the sidebar, centered there, so text lines stay readable. On windows between 640 and 840dp the shell keeps the old centered column.

## Risks / Trade-offs

- [The sidebar shows data the pages load, so a failed load leaves it half-empty] → the shared holder keeps the last good value and the sidebar shows the group's name and Overview whatever else is missing; Giving and Receiving appear only once the phase is known.
- [Resizing across the breakpoint while a sheet or the pane is open] → the sheet or pane state lives in the view model, so the same wishlist reappears in the other form; covered by a task.
- [A page that publishes actions late leaves the top bar empty for a moment] → actions appear with the page's own loaded state, as the floating buttons do today.
- [Phone in landscape is below 840dp and unchanged] → intended; the breakpoint is one constant if that turns out to be wrong.
- [Three platforms] → the width test and dialogs are plain Compose, but each platform is checked at both widths, since iOS and the web resize differently.
