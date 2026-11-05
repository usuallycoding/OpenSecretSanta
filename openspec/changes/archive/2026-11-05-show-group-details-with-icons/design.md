# Design

## Context

`GroupInfoCard` draws the description, then one text line per fact, each as a semi-bold label followed by the value. The app depends only on `compose.material.icons.core`, which has none of the three icons wanted, and already draws missing ones as `ImageVector`s from Material path data (`DragHandleIcon`, `GiftIcon`, `SwapIcon`). See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- Three icons that look like the rest of the Material set, with no new dependency.
- A screen reader hears the same line as before.

**Non-Goals:**
- No icon on the description, and none on the group list rows.
- No change to what the lines say or to the data behind them.

## Decisions

- **Hand-drawn vectors, not the extended icon set.** The calendar, pin and bank glyphs are added as `ImageVector`s from the Material path data, like the existing ones. The extended set would add a large library to every platform, the web build most of all, for three icons.
- **Each line is an icon and a text in a row, the icon in its own column.** A long location or a long currency then wraps under its own text and not under the icon. The icon is about 20dp, in the secondary text colour, and aligned with the first line of the text.
- **The spoken label is on the line, not on the icon.** The row is read as one item whose description is the same `"When? - 24 Dec 2026, 18:30"` text built by `labelledLine`, and the icon has no description of its own, as `NumberBadge` has none. That keeps what a screen reader says exactly as it is and keeps the existing strings.
- **The description is drawn by the same function, above the card.** `GroupInfoCard` keeps its place in both screens and draws the description first, as plain text, then the card when there is something for it. Splitting it into two calls would mean both screens must keep them in step. Alternative: two composables; rejected for that reason.

## Risks / Trade-offs

- [A bank icon is the least obvious of the three] → the amount and the currency sit right next to it, and the spoken label says "Budget?".
- [A group with only a description now shows no card, so the page looks plainer] → intended; the description is text, not a fact line.
