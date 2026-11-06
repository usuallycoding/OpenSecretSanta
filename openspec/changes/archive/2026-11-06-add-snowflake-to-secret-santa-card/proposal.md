# Proposal

## Why

The "You're Secret Santa for:" card is the page's one festive moment, and it looks like every other card. A small snowflake in its corner gives it character without adding anything to read or press.

## What Changes

- The "You're Secret Santa for:" card gets a snowflake in its top right corner, in a soft colour that works on the light and the dark theme.
- It is purely decorative: not read by a screen reader, not tappable, never over any text, and it does not change the card's size.
- It is always there, whether or not "Make it snow" is on: the falling snow is a separate setting.
- The "Your stuff:" card is unchanged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-group-pages`: one added requirement, "The Secret Santa card has a snowflake". "The Secret Santa card" is unchanged.

## Impact

- Client only: a new `SnowflakeIcon.kt` in `ui/form` and `SecretSantaCard` in `PairCards.kt`. No server, contract or database change.
