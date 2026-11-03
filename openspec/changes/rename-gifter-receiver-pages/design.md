# Design

## Context

The top bar lives in `ShellScreen`, outside the `NavHost`, and `titleOf` picks its text from the destination's route alone. `GifterRoute` and `ReceiverRoute` carry only the group id. The data the new titles need arrives later and lives inside the pages: the receiver's username in `ReceiverViewModel` (`GET /groups/{id}/receiver`) and the group's phase in `WishlistViewModel`. `TabbedPages` has the two tab labels hard-coded and is shared by both pages. See proposal.md for motivation and the delta spec for the requirements.

## Goals / Non-Goals

**Goals:**
- Titles and tab labels that depend on loaded data, with safe fallbacks before it loads.
- One place that builds each string, testable without UI.

**Non-Goals:**
- No change to routes, web addresses, view models' loading or what the pages show.
- No change to the pair links, their labels or `client-pair-link`.

## Decisions

- **The page supplies its title; the route stays as it is.** The shell keeps one piece of state for a page-supplied title, and `GifterPage` and `ReceiverPage` set it from their loaded data and clear it when they leave. `titleOf` uses it when set and otherwise falls back to the fixed titles. Alternative: put the username in `GifterRoute`. Rejected: it changes the route and the web address, and a deep link could carry a wrong name; the server remains the only source of who the receiver is.
- **Tab labels are parameters of `TabbedPages`,** two strings passed by each page, in place of the shared constants. The tab row stays one implementation.
- **Strings are built by small functions in `Strings.kt`** (for example one taking a username and returning "Giving to bob"), next to `emailSummary`, so they are plain unit-testable and the Gifter and Receiver wording is in one file.
- **Fallbacks:** the Gifter title is "Giving to ..." and its tabs "Wishlist" and "Questions" until the username is known. The Receiver title is "Your Wishlist" until the phase is known: that is true in every phase, so it can only be replaced by the more specific title, never contradicted. Both avoid blocking on a failed request.
- **Long labels:** each tab label is one line with an ellipsis, and the tab's accessibility label is the full text. Usernames go up to 32 characters, so "Questions for <username>" can exceed half a phone-width row.
- **Pair links keep `TITLE_GIFTER` and `TITLE_RECEIVER`** as their own labels; only the top-bar titles move to the new strings.

## Risks / Trade-offs

- [A brief flash of the fallback title on a cold open] → Accepted; it is accurate and is replaced as soon as the data arrives.
- [A title left over from a page after navigating away] → The page clears its title when it leaves composition, and `titleOf` ignores it for any other destination.
- [The Pending title shows before the phase loads for an Active group] → Accepted, as above; it changes once the phase is known.

## Migration Plan

None: no data or API change. Rollback is reverting the app code.
