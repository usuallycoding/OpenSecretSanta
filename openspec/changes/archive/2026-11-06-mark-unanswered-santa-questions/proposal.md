# Proposal

## Why

A question from the Santa that the person has not answered looks almost like an answered one: grey "Not answered yet" text and a plain "Answer" link. Nothing on the group page says there is anything waiting, so questions sit unanswered. Marking them, and counting them where the person looks first, tells them what needs a reply.

## What Changes

- On the Receiver page's "Respond to Secret Santa" tab, each question that has no answer, while the group is Active, gets a pill reading "Unanswered" beside the time it was asked and an accent bar on the card's start edge. The existing "Not answered yet" line stays, so colour is never the only sign.
- The "Respond to Santa" button of the "Your stuff:" card gets a badge on its corner with the number of unanswered questions, shown only while the group is Active and the number is above 0, as "9+" above 9.
- The pill, the bar and the badge use the theme's tertiary colour, in the light and the dark theme. A screen reader hears the count with the button ("Respond to Santa, 2 unanswered") and not the badge's own text.
- The group page loads the Santa's questions while the group is Active to count them: client only, no server or contract change. While they load, or if loading fails, no badge is shown.
- Not changed: the "Respond to Secret Santa" tab (it carries no badge: the button is enough), answered questions, Pending and Archived groups (nothing can be answered then), the Gifter page (the person's own questions to the receiver), the group list, and push notifications.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-receiver-and-questions`: one added requirement, the marks on an unanswered question.
- `client-group-pages`: one added requirement, the count on "Respond to Santa".

## Impact

- Client only: `QuestionThread`, `QuestionCard` and `SantaQuestionsSection` in `ui/gift`, `GroupPage` and `YourStuffCard` in `ui/groups`, a small shared badge and pill in `ui/form`, and strings. No server, contract or database change. The group page makes one more request, `GET /groups/{id}/santa/questions`, while the group is Active.
