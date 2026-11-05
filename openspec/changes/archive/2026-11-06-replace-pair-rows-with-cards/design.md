# Design

## Context

`PairRows` draws the rows from the group's phase and the receiver loaded by `PairViewModel`, and `GroupPage` keeps which details sheet is open (`PairSheet.Me`, `Receiver`, `Santa`) and passes `onOpenGifter` and `onOpenReceiver`, with no tab. `GifterRoute` and `ReceiverRoute` already carry a `questions` flag, which notifications and web addresses use. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- Every way in the rows gave (two sheets, two pages, the Santa reveal) is one button of a card, and nothing identifies the Santa before the group is Archived.
- Opening a page on a tab uses what already exists.

**Non-Goals:**
- No change to the sheets, the pages or the server.
- No new tab or deep link; the pair link and the gift icon stay for the administrator.

## Decisions

- **Two composables replace `PairRows`.** One draws the Secret Santa card, the other the Your stuff card; both take the phase and callbacks, and `GroupPage` places them after the information card. The card heading and button labels are strings, as the rest of the page's text is.
- **Cards show by phase, not by what loaded.** Pending shows "TBD" and no buttons in the first card; Active and Archived show the receiver. Draft is not shown to participants, and an unknown phase shows no cards, as the rows did.
- **`PairSheet` stays as it is.** The three sheets (own details, receiver, Santa) and `PairSheets` are reused; only what opens them changes. "Reveal Santa" sets `PairSheet.Santa` exactly as the "Secret Santa" row did, so the server's refusal before archiving and its "not yet" message keep working.
- **The tab is passed through the callbacks.** `onOpenGifter` and `onOpenReceiver` take whether to open on the questions tab, and `ShellScreen` builds `GifterRoute(groupId, questions)` and `ReceiverRoute(groupId, questions)`. The pages and the web address already follow the flag.
- **Capitals are drawn, not stored.** The username is upper-cased for display only (the data and the sheets keep the real casing) and the card's spoken description is the name as written, so a screen reader does not spell it out. Alternative: upper-case in the model; rejected because the same value feeds the sheet and the pages.
- **A receiver that has not loaded disables the first card's buttons** and shows "..." with the existing failure message and "Try again" under it, as the row did.

## Risks / Trade-offs

- [Removing the rows removes the pair link from the participant's page] → the cards carry every action; the pair link stays in the administrator's list, so its requirements are narrowed, not dropped.
- [The "Reveal Santa" button exists only when Archived] → it is not drawn at all earlier, so there is nothing to tap and nothing that hints at the Santa.
- [The page is taller, with two more cards] → it scrolls as one with the chips, as before.
