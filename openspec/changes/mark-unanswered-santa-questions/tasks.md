# Tasks

## 1. The rule and the pieces

- [x] 1.1 Add a pure rule for the count (unanswered questions while answering is open, 0 otherwise) and for the badge text (the number, "9+" above 9, none for 0) in `ui/gift`, with a test in `app/shared/test/ui` that verifies Active with some, all or no answers, Pending and Archived giving 0, no questions, and the text for 0, 1, 9, 10 and 30
- [x] 1.2 Add `CountBadged` (a `BadgedBox` with a tertiary badge, on-tertiary text, cleared semantics) and `UnansweredPill` in `ui/form`, and the strings "Unanswered" and the spoken form "Respond to Santa, N unanswered" in `Strings.kt`; verify with a preview or a throwaway screen that they read in light and dark and that "9+" fits

## 2. The question card

- [x] 2.1 Add a `needsAnswer` flag to `QuestionThread` that shows the pill beside the asked time, and to `QuestionCard` that draws the accent bar on the start edge behind the content (following the layout direction, after the snowflake pattern, no semantics), and pass it from `SantaQuestionsSection` as "no answer and `canAnswer`"; verify an unanswered question in an Active group has the pill and the bar, an answered one and any question in an Archived group has neither, the Gifter page is unchanged, and a screen reader reads the question as unanswered

## 3. The count

- [x] 3.1 Load `SantaQuestionsViewModel` in `GroupPage` while the group is Active (keyed by the group as the Receiver page does), reload it on resume, with pull to refresh and with the page's refresh, and give `YourStuffCard` the count so its "Respond to Santa" button carries `CountBadged` on its corner with the spoken count, none while loading, on failure, with none unanswered, Pending or Archived and with no new message; verify the badge shows 2 with three questions of which one is answered, drops after answering on the Receiver page and going back, shows "9+" with twelve, and that the two buttons keep their size

## 4. Check

- [x] 4.1 Run `./kotlin test` and `openspec validate mark-unanswered-santa-questions --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`, and look at the card and the button in light and dark on Android, iOS and the web

## Workflow follow-up

- Archive the change after looking at the marks and the count in both themes.
