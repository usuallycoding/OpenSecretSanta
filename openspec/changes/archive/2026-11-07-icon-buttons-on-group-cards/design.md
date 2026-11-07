# Design

## Context

`PairCards.kt` draws the two cards with text `Button`s: `UnreadButton` wraps a `Button` in `CountBadged` and gives it a content description with the count (`withNewOrEdited`, `withNew`, `withUnanswered`), and "Respond to Santa" does the same by hand. The icons exist already: `GiftIcon` and `PlaceIcon` are drawn in `ui/form`, and the send icon is `Icons.AutoMirrored.Filled.Send` (used by the wide-mode "Send letter" button). The owner's group page already uses `OutlinedIconButton`s for its actions.

## Goals / Non-Goals

**Goals:**
- Icon buttons with the same behaviour as the text buttons: names, counts spoken, badges, disabled states, and a red Delivery Info button with the nudge.
- One shared button, so the eight buttons cannot drift apart.

**Non-Goals:**
- Changing "Reveal Santa", the wide-mode top-bar actions, the floating buttons, or what any button opens.
- New strings: the old labels are reused as names and tooltips.

## Decisions

**One `CardIconButton`.** `CardIconButton(icon, name, onClick, enabled, count: String?, spoken: String, error: Boolean)` in `ui/groups` draws `CountBadged(count) { TooltipBox(...) { OutlinedIconButton(onClick, enabled, border = ...) { Icon(icon, contentDescription = null) } } }` with `Modifier.semantics { contentDescription = spoken }` on the button, where `spoken` is the name or the name with its count, built by the strings that exist (`withNewOrEdited`, `withNew`, `withUnanswered`). The icon announces nothing of its own; the button reads the name, so a screen reader hears "Inbox, 3 new" as before, and the badge stays silent as it is today.

**Tooltips.** Material 3's `TooltipBox` with `PlainTooltip` and `TooltipDefaults.rememberPlainTooltipPositionProvider()` shows on hover on desktop and the web and on a long press on touch. It is an experimental API, used with the opt-in the project already uses for sheets. If a platform does not show it, the spoken name and the position of the button still say what it is, and that is tried on each platform (a task).

**Icons.** The mail icon is drawn as an `ImageVector` (`MailIcon`, the Material "mail" path, like `KeyIcon` and `GavelIcon`) because the core icon set has none. The gift is `GiftIcon`, the pin is `PlaceIcon` and the send icon is `Icons.AutoMirrored.Filled.Send`. They are tinted by the button's content colour, so the red button needs only a red border and content colour.

**The red button.** `error = true` sets the border to `MaterialTheme.colorScheme.error` (as the owner page's delete button does) and the icon's tint to the error colour; the flag is the same condition that shows the message (`asksForDeliveryInfo`), so they cannot disagree.

**Layout.** Each card shows one `Row(spacedBy(8.dp))` of the buttons at the start, in the order gift, send, mail, pin; "Reveal Santa" stays above the row as a full-width text button, and the message sits directly above the row, below "Reveal Santa". The Santa card's buttons are disabled while the receiver is unknown, as now; "Try again" stays as it is.

## Risks / Trade-offs

- [Icons without words are harder to read] → tooltips on hover and long press, the spoken names, the same order on both cards, and the badges say what is new.
- [Two cards use the same icon for different things] → "See Wishlist" and "Edit Wishlist" share the gift, and "Ask Questions" and "Respond to Santa" share the send icon, which is what was asked for; the card's heading says which side the person is on.
- [The tooltip API is experimental or behaves differently on a platform] → opt in as elsewhere and look at each platform; the button works without it.
- [A red button with no badge may read as an error state] → it is only red while the message above it asks for the delivery info, and goes back as soon as it is complete.

## Migration Plan

None: no data, API or setting changes.
