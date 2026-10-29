# Design

## Context

- The arrow is an `IconButton` holding `Icons.AutoMirrored.Filled.ArrowForward`, in three places: the first and second rows of `PairRows` (participant's group page; they open the Gifter and the Receiver page) and every row of `PairList` in `OversightSheets` (administrator's pair list; it opens the pair sheet). Each place sets the icon's `contentDescription` to the label of what it opens (`Strings.TITLE_GIFTER`, `Strings.TITLE_RECEIVER`, `Strings.adminPair(santa, receiver)`).
- `GiftIcon` is an internal `ImageVector` in `ui/form`, already used alone for the Pending row and for the administrator's per-participant wishlist button.
- The theme defines the tonal container colours (`secondaryContainer`, `onSecondaryContainer`) for light and dark, and `ParticipantChips` already draws a pill with a `Surface` in `secondaryContainer`, so a tonal pill needs no new colour.
- The project has no composable UI tests, only view-model and logic tests; how a screen looks and what a screen reader says are checked by running the app.

## Goals / Non-Goals

**Goals:**
- One shared control for all three places, so they cannot drift apart.
- A control that reads as a button: tonal fill, a pressed state, a touch target of at least 48dp.
- A single spoken announcement, with the label each place already had.

**Non-Goals:**
- Changing what the Gifter page, Receiver page or pair sheet show; turning the lone gift icons into pair links; animations or a first-use hint; new strings.

## Decisions

### One composable, `PairLink`, in `ui/form`
`PairLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier)`. `PairRows` and `PairList` call it in place of their `IconButton`; the `ArrowForward` imports go from both. It lives next to `GiftIcon` and `ParticipantChips`, the other shared pieces the group screens use.
- Why not a parameter-less icon trio copied into each place: three copies would drift, and the touch target, colours and spoken label are exactly what must be the same everywhere.

### The pill
A `Surface(onClick = ..., shape = pill, color = secondaryContainer, contentColor = onSecondaryContainer)` holding a `Row` of three icons: `ArrowForward` (auto-mirrored), `GiftIcon`, `ArrowForward` (auto-mirrored), about 18, 20 and 18dp with a few dp between them and horizontal padding that makes the pill about 84 to 90dp wide and about 32dp tall.
- Why a filled pill and not plain icons with a ripple: the point of the change is that people tap it. A tonal fill is the same visual language the app already uses for its other tappable chips and buttons, and a ripple alone is invisible until pressed.
- Colour: `secondaryContainer` is also what `ParticipantChips` uses for the person's own username on the same page. They are told apart by shape and content (icons, and chips are not pressable), so no second colour is introduced; if they look confusable on a device, switching the pill to `primaryContainer` is a one-line change (the two are the same value in the light palette).
- The arrows are `AutoMirrored`, so in a right-to-left language the group still leads from the giver, at the start, to the receiver, at the end.

### Touch target
The drawn pill is about 32dp tall; the control around it is at least 48 by 48dp (the pill centred in a `minimumInteractiveComponentSize()` area that also responds to a press), so a thumb does not have to hit a small shape. The pressed state is the `Surface`'s own ripple, clipped to the pill.

### Accessibility
The `Surface` carries one `contentDescription` (the `label`) and the three icons have `null` descriptions, so the control is announced once as a button with the place's existing label, never as arrow, gift, arrow. No new strings are needed.

### Rows keep their layout
The row stays `Row { UserButton(weight 1) ; PairLink ; UserButton(weight 1, end-aligned) }`. The pill is about 40dp wider than the `IconButton` it replaces, which takes about 20dp from each name; a name that no longer fits wraps (the buttons' `Text` is not limited to one line) and the link keeps its size. Nothing is truncated.

## Risks / Trade-offs

- [Less room for names, more wrapping on a narrow screen] → about 118dp per name on a 360dp phone fits ordinary usernames; a task checks a long username with the "(you)" and `*` marks on the phone.
- [The pill and the person's own username chip share a colour on the group page] → different shape and content, and a one-line switch to `primaryContainer` is available.
- [Looks and the spoken label cannot be unit-tested here] → checked on the phone with a screenshot, `uiautomator` for the touch target's bounds and its description, a forced right-to-left layout, and both themes; the web is checked by the person running it.
- [Admin lists with many pairs get busier with a pill in every row] → the list is a read-only list of pairs a few dozen at most, and the pill is compact; revisit only if it looks heavy.

## Migration Plan

Nothing to migrate: a client-only look change. Rolling back is putting the `IconButton` back in the two places.

## Open Questions

- The exact icon sizes and padding, to be tuned on the device until the pill looks balanced next to the names.
