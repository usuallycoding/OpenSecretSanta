## Context

See proposal.md for motivation. `LinkText` (`app/shared/src/ui/form/ReadOnlyText.kt`) shows a link on one ellipsized line with a `CopyButton`; choosing the text sets a flag that shows `LinkDialog`, whose "Open in browser" button calls `LocalUriHandler.current.openUri(url)` inside `runCatching` and closes the dialog. `LinkText` is called from one place, `ItemText`, which the person's own list, a Santa's view of the receiver's list and the owner's participant wishlist sheet all share. `LinkDialog` has no other caller and no test refers to it.

## Goals / Non-Goals

**Goals:**
- One tap or click on a link opens it, on every platform the app runs on (Android, iOS, web).
- Keep the copy icon and the one-line layout as they are.

**Non-Goals:**
- Showing more of a long link, such as its host or its middle, or wrapping it.
- A confirmation or warning for unfamiliar sites.
- Any server, contract or domain change.

## Decisions

### `LinkText` calls `openUri` itself
The clickable text handles the tap with `LocalUriHandler.current.openUri(url)` inside the same `runCatching` the dialog used, so a link nothing can open still does nothing and says nothing, as the spec says. `LinkDialog` and the `open` state are removed. The handler is read once in `LinkText` and the call stays directly in the click handler, which is what lets the web target's `window.open` run from a real user gesture, as the dialog's button did.

Alternative: keep the dialog behind a long press so the whole link can still be read. Rejected: it adds a gesture nobody would find, and the copy icon already gives the whole link.

### The row is marked as a link for screen readers
The clickable text gets button semantics (`Role.Button`) and an "Open link in browser" description (a new string in `Strings.kt`), so that it is no longer a plain text that happens to react to taps. The label carries the meaning; whether a dedicated link role exists in the Compose version in use is checked when implementing, and used if it does.

### Accepted trade-off: the whole link is not shown before leaving
The dialog was the only place the whole link was visible before the person left the app, and the row cuts a long link with an ellipsis. Opening directly means a long address is opened without being seen in full. What stays in place: the server only accepts `http` and `https` addresses with a plain host (no user-info, ASCII or `xn--` host names), so nothing can run inside the app, and the copy icon still gives the whole link. The remaining risk is an unexpected destination, which the person chose to accept by asking for direct opening.

### Remove what is left unused
`LinkDialog`, `Strings.WISHLIST_LINK` ("Link") and `Strings.OPEN_IN_BROWSER` have no other user, so they are deleted with the dialog. `Strings.COPY` and the copy icon stay.

## Risks / Trade-offs

- [A long link opens without being read in full] → Accepted above; the copy icon, the `http` and `https` rule and the strict host rule bound the risk. A later change can show the host if it proves a problem.
- [An accidental tap on a link now leaves the app] → Links sit on their own line with the copy icon beside them, and edit and drag controls are at the end of the item card, away from the link text; no change is made to the layout.
- [The web target blocks a popup] → The call is made directly from the click handler, as the dialog's button did, so it counts as a user gesture; this is checked by hand on the web target.
- [No handler can open the link] → Nothing happens, as before; there is no message to show.

## Migration Plan

Nothing to migrate. Ship with the next app release; the server is not involved.
