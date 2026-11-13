# Design

## Context

Every time line goes through `TimesLine` (`app/shared/src/ui/form/TimesLine.kt`), drawn by three shared composables: `ItemText` (`WishlistScreen.kt`), `QuestionThread` (`gift/QuestionThread.kt`) and `LetterCard` (`letters/LetterScreens.kt`). The same composables serve both the participant screens and the owner's oversight sheets (`admin/OversightSheets.kt`: `ItemText` at the participant wishlist, `QuestionThread` in `ThreadQuestions`, `LetterCard` in `PairLetters`). `AnnouncementCard` also uses `TimesLine` but is out of scope.

The Admin and Participant views are separate screen sets (`AppView`, `isAdminScreen()`); an administrator can switch between them. The oversight sheets only exist in the Admin view, so "Admin view" and "oversight sheets" are the same set of call sites.

## Goals / Non-Goals

**Goals:**
- Participant screens draw no time line, whoever is signed in.
- The oversight sheets draw exactly what they draw today.
- Pills (`New`, `Edited`, `Unanswered`) stay where they are and still depend on DTO timestamps.

**Non-Goals:**
- No change to the server, DTOs, `ReadMarks.kt` or the view models; the mapped `added`/`changed`/`asked`/`answered` strings keep being produced.
- No change to announcements.

## Decisions

**An explicit `showTimes: Boolean = false` parameter on `ItemText`, `QuestionThread` and `LetterCard`; the three oversight call sites pass `true`.**
The rule is "Admin view shows, everything else hides", and the call sites of the Admin view are exactly the three in `OversightSheets.kt`. Defaulting to `false` means a participant screen cannot show times by forgetting a flag.
- *Alternative: a `CompositionLocal` provided by `ShellScreen` from `ShellViewModel.view`.* Rejected: the view is derived from the destination in some places, the Admin view embeds participant composables elsewhere (the announcements pane), and a hidden ambient value is harder to test than a parameter.
- *Alternative: decide by `account.isAdmin`.* Rejected by the user: an administrator in the Participant view must see what a participant sees.

**Do not compose the time `Row` when there is nothing in it.** With `showTimes = false` and no pill the `Row` is skipped, so the column's `spacedBy(2.dp)` does not leave a gap; with a pill (`New`, `Edited`, `Unanswered`) only the pill is drawn, at the start of the row.

**Keep the mapping untouched.** Models such as `WishItem.added` and `LetterItem.sent` still carry the formatted times (the oversight sheets need them and `TimesMappingTest` covers them). Hiding is purely in composition.

**Screen readers.** The time labels are hidden along with the line; the oversight sheets keep the labels they have.

## Risks / Trade-offs

- [A participant composable is later reused in the Admin view and shows no time] → Admin call sites pass `true` explicitly; the oversight sheet tests assert the lines are present.
- [Tests reading the time lines on participant screens fail] → Update them to assert absence; keep the admin ones asserting presence.
- [A card with only a pill looks different] → The pill is drawn alone at the start of the row; check the Receiver, Gifter and Inbox cards visually in light and dark.
