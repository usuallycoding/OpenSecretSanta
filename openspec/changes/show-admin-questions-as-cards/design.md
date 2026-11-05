# Design

## Context

The pair sheet draws the wishlist with `WishlistItems`, a `Column` spaced 8dp apart of `Card`s with 16dp padding, and the thread with `ThreadQuestions`, a `Column` spaced 12dp apart of bare `QuestionThread`s. The Gifter and Receiver pages wrap the same `QuestionThread` in `QuestionCard` (`Card`, 16dp padding), but that function takes the page's `Question` model, while the thread of the sheet holds `OversightQuestion`s. See proposal.md.

## Goals / Non-Goals

**Goals:**
- The questions of the pair sheet look like its wishlist items and like the questions on the Gifter and Receiver pages.

**Non-Goals:**
- Changing `QuestionThread` or the oversight data, or adding any action to the cards (the sheet stays read-only).

## Decisions

**Wrap in a `Card` where the thread is drawn, with the wishlist's padding and spacing.** `ThreadQuestions` puts each `QuestionThread` in a `Card(Modifier.fillMaxWidth())` with `Modifier.padding(16.dp)` and spaces the cards 8dp apart, exactly as `WishlistItems` does for its items, so the two lists in one sheet match. Alternatives rejected: converting `OversightQuestion` to `Question` to call `QuestionCard`, which adds a mapping for a four-line wrapper; making `QuestionCard` generic over the model, which touches the Gifter and Receiver pages for no visible gain.

## Risks / Trade-offs

- [The card narrows the question by its 16dp padding, so long answers wrap a little earlier in the sheet] → The Gifter and Receiver pages already do this; check a long answer in the sheet by eye.
