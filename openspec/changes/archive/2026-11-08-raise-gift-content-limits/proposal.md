# Proposal

## Why

A participant can ask at most 30 questions, send at most 30 letters and keep at most 50 wishlist items per group. People who want to say more, or list more ideas, hit the limit with no way round it. The limits are ceilings, not typical sizes, so they can be raised well above what a group usually needs.

## What Changes

- A Santa can ask up to **200** questions per group (was 30).
- A receiver can send up to **200** letters per group (was 30).
- A participant can keep up to **200** wishlist items per group (was 50).
- The three limits stay separate numbers, so they can differ again later.
- The character length caps stay as they are: a question 1 to 1000 characters, a letter title 1 to 100 and text 1 to 2000, and the wishlist title, note and link caps.
- The refusals, the "limit reached" messages and the way the apps hide the add, ask and send actions at the limit work as before, at the new number.
- Notifications are not affected: they never say how many items exist.
- The lists in the apps stay plain scrolling columns. Earlier designs chose that because the limits were 50 and 30; 200 is a ceiling, and the lists are checked on devices after the change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `santa-questions`: "Ask a question" allows up to 200 questions per Santa per group, and its limit scenario uses 200.
- `letters`: "Send a letter" allows up to 200 letters per receiver per group, and its limit scenario uses 200.
- `wishlist`: "Add an item" allows a list of up to 200 items, and its full-list scenario uses 200.
- `client-receiver-and-questions`: "Asking a question" makes asking unavailable at 200, and its limit scenario uses 200.
- `client-letters`: the "Letter limit reached" scenario refers to 200 letters.
- `client-wishlist`: "Adding an item" makes adding unavailable at 200, and its full-list scenario uses 200.

## Impact

- `core/domain/src`: the three limit constants (`QuestionLimits.MAX_QUESTIONS_PER_SANTA`, `LetterLimits.MAX_LETTERS_PER_RECEIVER`, `WishlistLimits.MAX_ITEMS`), and the comments in `QuestionUseCases.kt` and `LetterUseCases.kt` that say "up to 30". The server and the apps read these constants, so nothing else in the code changes.
- `app/shared/test/ui/TestSession.kt`: the client test double for sending letters has its own 30 and follows the real constant instead.
- The six specs above. The domain tests already use the constants.
- No change to the API, the stored data or the notifications. A group that already has 30 or 50 items simply can add more.
