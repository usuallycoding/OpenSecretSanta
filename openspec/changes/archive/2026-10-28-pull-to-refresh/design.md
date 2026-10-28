# Design

## Context

Eleven screens load through `LoadContent` and show a "Refresh" `TextButton` in a row above it: Groups, the group page, the participant's wishlist, the receiver's wishlist, the receiver page, the Santa's questions and the Santa reveal on the participant side, and the admin groups list, group page, users list and user page. `LoadContent(state, onRetry) { value -> ... }` shows the first-load spinner, the failure with "Try again", or the content with a thin `LinearProgressIndicator` while a reload runs. `Loader` is the same everywhere; `state.loading` is true for any load, including the one on returning to the app. Content is a `LazyColumn`, a `verticalScroll` column, or a bare `Text` for empty screens. `Platform` has `getPlatform()` with one implementation per target. Material 3's `PullToRefreshBox(isRefreshing, onRefresh, ...)` is in the version in use. Sheets, the Profile tab and the admin forms have no Refresh button. See proposal.md and specs/.

## Goals / Non-Goals

**Goals:**
- Pull to refresh on every screen that had the button, on Android and iOS; no refresh control on the web; the quiet reload on resume unchanged.

**Non-Goals:**
- The Profile tab (unsaved edits), sheets, cleaner web addresses.

## Decisions

### D1: The gesture lives in `LoadContent`
```
 LoadContent(state, onRetry, onRefresh = viewModel::refresh) { value -> content }
   value != null and the platform has the pull:
     PullToRefreshBox(isRefreshing = pulled, onRefresh = { pulled = true; onRefresh() })
        Column { error message + "Try again" ; content }
   value != null and no pull (web), or no onRefresh: as today, without the button
   no value yet (first load or failed first load): spinner or failure with "Try again", no pull
```
Every refreshing screen already goes through `LoadContent`, so each screen's change is to pass `onRefresh` and delete its Refresh row. `pulled` is local state, set when the person pulls and cleared when `state.loading` turns false, so the pull indicator is shown only for a pull. The thin bar is skipped while `pulled`, so a pull does not show two indicators; the automatic reload on resume keeps the bar and no pull indicator.

### D2: Which platforms have the pull
A flag on `Platform` (`hasPullToRefresh`), true on Android and iOS, false on the web, read where the screens are composed. A browser on a phone has its own pull-to-reload that reloads the page, which with the session kept is the right thing there.

### D3: The content must be scrollable
`PullToRefreshBox` reacts to scrolling the content past its top, so every refreshing screen needs scrollable content. Lists (`LazyColumn`) and the detail pages (`verticalScroll`) already are. The empty and short states are bare text or short columns, so a shared `ScrollableMessage` composable (a `verticalScroll` box that fills the space) replaces them: the empty Groups, wishlist, receiver wishlist, questions and admin lists. `LazyColumn` content is not wrapped in another scroll container (that would crash), only the empty branches change.

### D4: What stays as it is
Reload on `ON_RESUME`, keeping the old value while a reload runs or fails, and the "Try again" on the error message. `Strings.REFRESH` has no user left and is deleted. The Profile tab keeps reloading on resume only: a pull there could replace what the person has typed. Sheets reload each time they open and keep their own "Try again" per part.

### D5: Tests
The flag and `LoadContent`'s decisions are small; the common tests cover the flag per platform (the wasm test already asserts the platform name). The gesture itself is verified by running the app: on the Android emulator a pull from the top of a list, of a detail page, of an empty screen and with the server stopped (the indicator goes away, the content stays, "Try again" shows); in headless Chrome that no screen has a Refresh button and that a browser refresh keeps the screen and the session; iOS is verified by the person running the app.

## Risks / Trade-offs

- [A pull and the keyboard-dismissing drag of forms both react to dragging] → Only the Profile and the forms dismiss on a drag, and they are not pull screens, so they do not overlap.
- [A pull on a screen that cannot scroll does nothing] → D3 makes the empty states scrollable; the check in the tasks covers one of each kind.
- [People who liked the button lose it on the phone] → The gesture is the platform's own habit; "Try again" remains for failures.

## Migration Plan

Nothing to migrate. Rollback is the previous build.
