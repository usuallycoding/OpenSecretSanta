# Design

## Context

The Receiver page's second tab (`SantaQuestionsSection`) lists the Santa's questions as `QuestionCard`s built on `QuestionThread`, whose unanswered case is muted "Not answered yet" text and a plain "Answer" button. `SantaQuestionsViewModel` loads `GET /groups/{id}/santa/questions` with the group's phase and says whether answering is open (`canAnswer`, Active only). `GroupPage` knows only the group and, once drawn, the receiver (`PairViewModel`); `YourStuffCard` takes just the phase. The theme has a tertiary colour (a gold, with a container and on-colours) in both themes. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- One definition of "unanswered", used by the card and the button, so the two always agree.
- No server or contract change.

**Non-Goals:**
- No badge on group list rows (it would need a count from the server), none on the Receiver page's tabs (the button is enough), no change to notifications, no mark on the Gifter page.

## Decisions

- **One pure rule: unanswered = no answer while answering is open.** A function over the questions and `canAnswer` gives the count, and another gives the badge's text (the number, "9+" above 9, nothing for 0). Both are plain functions, so they are unit tested apart from the screens. Archived and Pending give 0 because answering is not open, which keeps the pill, bar and badges off when nothing can be done about them.
- **The group page loads the questions with the existing `SantaQuestionsViewModel`**, keyed by the group as the Receiver page does, only while the group is Active. It reuses the request and the model instead of adding an endpoint or a count field. It reloads on resume, with the page's pull to refresh and when the page's own refresh runs. Alternative: a count on `MemberGroupDto`; rejected for now as a server change that only pays off with a list badge, which is out of scope. The extra request is small and only while Active.
- **Quiet on failure.** While loading, or after a failure, the count is unknown and no badge is shown; the group page does not add a message, since the Receiver page's tab already reports errors for the same data.
- **`CountBadged` in `ui/form`**, a small tertiary circle (pill for two digits) with the on-tertiary text, drawn on the corner of what it badges with `BadgedBox`. It clears its own semantics; the button instead carries the spoken count ("Respond to Santa, 2 unanswered") in its own description. Used on the "Respond to Santa" button, whose row keeps its two equal buttons. An earlier plan also badged the second tab; its corner covered the label, and the button alone is enough, so the tab has none.
- **`UnansweredPill` and the accent bar for the question card.** The pill is a small tertiary-container label with its text beside the asked time in `QuestionThread` (a `needsAnswer` flag, off by default, so the Gifter page and answered questions are unchanged). The bar is a few dp wide on the start edge of the `QuestionCard`'s content, drawn behind it, so the card's shape rounds it and it takes no layout; it follows the layout direction. The bar has no semantics; the pill's text is what a screen reader reads.
- **Tertiary, not error red.** It suits the festive palette and reads as "for you to do" rather than as an error; text on the pill and badge uses the matching on-colours, so contrast holds in both themes.
- **The snowflake pattern is untouched.** The pill and badge draw over the card's content as before; the bar is drawn after the pattern so the pattern does not cover it.

## Risks / Trade-offs

- [The group page now makes one more request while Active] → one small read, shared model, reloaded only on the events above.
- [The count can be stale if the Santa asks while the page is open] → it refreshes on resume, pull and return to the foreground, as the rest of the page does; push notifications for new questions already exist.
- [Two digits make a wider badge that could be cut by the card's edge] → "9+" is the widest; keep the badge inside the card's padding.
