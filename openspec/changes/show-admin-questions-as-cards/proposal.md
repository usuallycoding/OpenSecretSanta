# Proposal

## Why

In the owner's pair sheet the wishlist items are cards, but the questions of the pair's thread below them are bare text on the sheet's background. The questions on the Gifter and Receiver pages are cards, so the owner's overview is the one place they are not, and the sheet looks like two different lists.

## What Changes

- Each question of the pair thread is shown in a card, like the wishlist items in the same sheet and like the questions on the Gifter and Receiver pages. The number circle, the indented answer and the line inside it stay as they are.
- The cards are spaced like the wishlist cards above them.
- No other screen, no data and no server behaviour change.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-admin-oversight`: "Pair thread uses the same layout" also says that each question is in a card, like the wishlist items in the same sheet.

## Impact

- `app/shared/src/ui/admin/OversightSheets.kt`: `ThreadQuestions` wraps each question in a card.
- Shared UI code, so Android, iOS and the web change together; no tests exist for the sheet's layout, so it is checked by eye.
- `openspec/specs/client-admin-oversight` at archive time.
