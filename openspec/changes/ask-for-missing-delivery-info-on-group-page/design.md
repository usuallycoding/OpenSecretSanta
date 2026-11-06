# Design

## Context

`GroupPage` knows the group and, once drawn, the receiver (`PairViewModel`) and the Santa's questions (for the unanswered count); `YourStuffCard` takes the phase and that count. The person's own delivery info is loaded only when their sheet opens, by `MyDetailsViewModel` (keyed `my-details-<group>`, one instance per group within the page's view model store), which turns `GET /groups/{id}/delivery-info` into `MyDetails` with blank values left out. `DeliveryInfo.isComplete` in `core/domain` already defines complete as the four required fields. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- One definition of "complete" on the client, the domain's, and one place that decides to show the message.
- No new request path and no server change.

**Non-Goals:**
- No change to the Delivery Info page, no blocking of any action, no new view model.

## Decisions

- **Share `MyDetailsViewModel` instead of adding one.** `GroupPage` obtains it with the same key the sheet uses, only while the group is Pending or Active, so the sheet opens on data that is already there, and the page loads the person's info once. It reloads on resume and with the page's pull to refresh, as the Santa's questions do. Alternative: a new small view model; rejected, it would load the same data twice.
- **A pure rule decides the message: `MyDetails.isComplete` and `showsDeliveryNudge(phase, state)`.** `isComplete` builds a domain `DeliveryInfo` from the cleaned fields and asks it, so the four-field rule exists once; `showsDeliveryNudge` is true only for Pending or Active, with a loaded value (`value != null`), no error (`error == null`, so a failed reload is unknown, not "incomplete") and an incomplete result. It is a plain function over the load state, unit tested apart from the screen.
- **`YourStuffCard` gets a boolean and draws the text.** The text sits directly above the "Delivery Info" button, below "Reveal Santa", in `MaterialTheme.colorScheme.error` with the body style, as plain `Text`: not a button and not inside one, so a screen reader reads it as it is. Alternative: a banner at the top of the card; rejected, the person asked for it above the button.
- **Quiet when unknown.** Loading, failure and Draft show nothing and add no message of their own, matching the unanswered badge, so a flaky connection never accuses anyone.
- **The page's own hint is untouched.** The Delivery Info page keeps its existing explanation; this message only brings the person to it.

## Risks / Trade-offs

- [One more request per visit while Pending or Active] → one small read, shared with the sheet, reloaded only on the events above.
- [A stale message right after saving] → the page reloads on resume, which is when the person comes back from the Delivery Info page; a failed reload counts as unknown and hides the message rather than keeping a wrong one.
- [Many people see the message at once, because earlier saves lacked an email or phone] → intended; it is the prompt to complete them, and nothing is blocked.
