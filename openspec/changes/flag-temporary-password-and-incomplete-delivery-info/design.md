# Design

## Context

Completeness is one rule, `DeliveryInfo.isComplete` (name and address), used by `GroupViewer` to build `ParticipantStatus.profileComplete`, which `GroupRoutes` maps to `ParticipantDto`; the app turns that into `AdminParticipant.profileComplete`, the `*` mark (`marked(draft)` in `AdminGroupScreen.kt`, `usernameLabel` in `OversightSheets.kt`) and its one legend. A person's delivery info is saved by `UpdateMyDeliveryInfo`, whose route builds the value with `DeliveryInfo.of` (trim, blanks to null, email and length checks); the page's form (`DeliveryInfoViewModel`) checks the same and nothing is required. `GroupViewer` already reads each participant's account (`users.findAll`), whose `User` has `mustChangePassword`. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- One definition of "complete" shared by the flag, the save rule and the form, and one source for the temporary-password sign.
- The marks, the count and the missing-fields line come from pure functions that are unit tested apart from the screens.

**Non-Goals:**
- No filter, no change to who can read what, no schema change, no automatic reminder to people.

## Decisions

- **`isComplete` means the four required fields; the save rule is the same list.** `DeliveryInfo` gets the list of required fields once, used by `isComplete`, by `requireComplete()` (which throws `ValidationFailed` naming the first missing field, as `of` does for the others) and by the client's own copy of it. `UpdateMyDeliveryInfo` calls `requireComplete()` before saving, so the rule lives in the domain and the route only maps. Reading, the draw, the receiver and the Santa views are unchanged: they show whatever is saved. Alternative: validate only in the route; rejected because other callers of the use case would bypass the rule.
- **Old data is not migrated.** Profiles copied from accounts that lack an email or a phone simply read as incomplete, which is the point of the marks. Such a person can read their info and must fill the four before any save succeeds. Alternative: a migration marking people; rejected, the flag is computed.
- **`mustChangePassword` on `ParticipantStatus` and `ParticipantDto`**, taken from the account that `GroupViewer` already loads: true only when an account exists and has a temporary password. The DTO field defaults to false, so an older client ignores it and an older payload still parses. It is a flag only; the owner can already read the temporary password itself through `GET /admin/users/{username}` and "Get passwords", so nothing new is exposed. Alternative: have the client call the user route per participant; rejected as one request per person on every list load.
- **Marks are a pure value, `ParticipantMarks(noAccount, temporaryPassword, incompleteInfo)`**, computed from `AdminParticipant` and the group's phase: no account is `*` only (no key, no pin), the key needs an account and a non-Draft group, the pin needs an account and a non-Draft group (in a Draft nobody can fill info in and accounts do not exist yet). One function feeds the rows, the pair list, the pair sheet titles, the legend and the count, so they cannot disagree. Alternative: keep the string label with glyphs; rejected, an icon needs a spoken name and the legend must know which marks are present.
- **Icons: a new `KeyIcon` in `ui/form`, and the existing `PlaceIcon` (a map pin) as the pin,** drawn like the other icons, placed after the username as inline content of the text, so they flow and wrap with it in rows, in the pair list and in the pair sheet title, each with a content description ("Temporary password", "Delivery info incomplete"). The legend is one line under the list with each present mark and its text, replacing `ADMIN_MARK_LEGEND` and `ADMIN_INCOMPLETE_LEGEND`.
- **The count is `markCounts(participants, draft)`**, rendered as one text above the list, parts with 0 left out, nothing when both are 0, no click handlers.
- **Missing fields are a pure function over the loaded delivery info** (`missingFields`: the labels, in the order full name, email, phone, address, of those not set), shown as "Missing: …" in the user sheet's delivery part only when some but not all four are set; with nothing set the sheet keeps its single "has not added delivery info yet" sentence, which already says everything.
- **The form shows every problem at once.** `DeliveryInfoViewModel` checks the four required fields as well as the email and the lengths before sending, reports each next to its field, and the four labels carry a required mark (a trailing `*`, which on this page means only "required"). The explanatory text and the "more directly" line follow the new rule.

## Risks / Trade-offs

- [Many people become "incomplete" at once, since old profiles lack an email or a phone] → intended: the key and pin marks and the count then show the owner whom to ask; activation is still never blocked by them.
- [A person with incomplete info cannot make a one-field edit and save] → they are asked for all four; the form says which are missing.
- [Two marks on one name make long rows on a phone] → the icons are inline and wrap with the text, and the legend carries the words.
- [`profileComplete` now means more than its first spec said] → every spec and the README that stated name and address are modified in this change.
