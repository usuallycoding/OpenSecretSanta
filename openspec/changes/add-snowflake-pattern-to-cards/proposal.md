# Proposal

## Why

The Secret Santa card has a single snowflake in its corner while every other card is plain. A faint, scattered pattern of snowflakes on all cards gives the whole app the festive look without adding anything to read or press, and replaces a decoration that reads as a one-off.

## What Changes

- Every card in the app gets a faint pattern of snowflakes behind its content: several shapes, several sizes, turned every which way, some cut off by the card's edge.
- The pattern is decorative only: not read by a screen reader, not tappable, never changes a card's size or layout, and it sits behind the text and buttons.
- A card always shows the same pattern, so nothing shuffles while scrolling or when the card is redrawn; different cards show different patterns.
- It is always there, whether or not "Make it snow" is on, and works on the light and the dark theme.
- **Replaces** the single corner snowflake of the "You're Secret Santa for:" card: the text of that card no longer keeps clear of a corner, and the "Your stuff:" card is no longer the exception.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-appearance`: two added requirements, one for the pattern on every card and one for its look.
- `client-group-pages`: "The Secret Santa card has a snowflake" is removed.

## Impact

- Client only. `SnowflakeIcon.kt` in `ui/form` becomes a set of flake shapes, a new card wrapper and flake placement are added, and the 13 `Card(` call sites in `ui/admin`, `ui/groups`, `ui/gift` and `ui/wishlist` use the wrapper. `SecretSantaCard` in `PairCards.kt` loses its corner flake and the end padding that kept text clear of it. No server, contract or database change.
