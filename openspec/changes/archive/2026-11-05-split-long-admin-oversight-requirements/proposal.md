# Proposal

## Why

Three requirements of `client-admin-oversight` are over 500 characters ("The draw" at 1138, "Participant wishlist" at 544 and "User sheet" at 541), each stating several behaviours. `openspec validate --strict` flags requirement text over 500 characters, so the spec validates only with warnings. Splitting them so that each requirement states one behaviour clears the warnings without changing what the app does.

## What Changes

- "The draw" keeps the pair list itself (the list, its order and the layout of a row). Its other behaviours become their own requirements: the pair list replaces the participants list and is part of the scrolling page; its label counts the participants; the "you" and `*` marks; and no pair list before the draw.
- "Participant wishlist" keeps the sheet and what it shows. Two behaviours become their own requirements: the sheet is read-only and is not a screen; its links open the link dialog.
- "User sheet" keeps the sheet and what it shows. Resetting a password from it becomes its own requirement.
- Every existing scenario stays where it is. Each new requirement has its own scenario.
- No change to behaviour, code or any other spec.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-admin-oversight`: "The draw", "Participant wishlist" and "User sheet" are cut to one behaviour each, and the behaviours taken out become seven new requirements.

## Impact

- Spec text only: `openspec/specs/client-admin-oversight/spec.md` at archive time.
- `design.md` is skipped: a wording split has no technical choices to explain.
