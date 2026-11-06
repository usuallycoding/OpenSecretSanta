# Tasks

## 1. The rule

- [x] 1.1 Add `MyDetails.isComplete` (built on the domain's `DeliveryInfo.isComplete`) and a pure `showsDeliveryNudge(phase, state)` in `ui/groups` that is true only for a Pending or Active group whose delivery info has loaded without error and lacks a required field, with tests that verify each missing required field, all four set, only the delivery information empty, Draft and Archived never, loading, failed and a failed reload after a load never

## 2. The message

- [x] 2.1 In `GroupPage` obtain the shared `MyDetailsViewModel` (same key as the sheet) while the group is Pending or Active, reload it on resume and with pull to refresh, and give `YourStuffCard` the result so it draws "Please fill out your delivery info in order to be able to receive your secret gift!" in the error colour directly above its "Delivery Info" button (below "Reveal Santa" when shown), with the text in a `Strings` constant; add `MyDetailsViewModel` tests in `PairViewModelTest` style that verify an incomplete load, a complete load, a failure, and a reload after the server's info became complete; verify in a Pending, an Active and an Archived group that the message shows only in the first two, in the error colour, above the button, that it disappears after saving on the Delivery Info page and going back, and that no action is blocked
- [x] 2.2 Mention the message in the README's description of the "Your stuff:" card, and update any existing test or fixture that assumed the group page makes no request to `/groups/{id}/delivery-info`; verify `README.md` says when it shows

## 3. Check

- [x] 3.1 Run `./kotlin test` and `openspec validate ask-for-missing-delivery-info-on-group-page --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`, and look at the card in light and dark on Android, iOS and the web

## Workflow follow-up

- Archive the change after looking at the card in a Pending, an Active and an Archived group.
