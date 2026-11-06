# Tasks

## 1. Server: completeness, the flag and mandatory saves

- [x] 1.1 In `core/domain` make `DeliveryInfo.isComplete` mean full name, email, phone and address, add `requireComplete()` (first missing field as `ValidationFailed`, delivery information optional) and call it from `UpdateMyDeliveryInfo`, updating `DeliveryInfoUseCasesTest` and any test or fixture that saved partial delivery info; verify new cases for each blank required field, blank delivery information accepted, invalid email and length still refused, and that reading incomplete saved info still works
- [x] 1.2 Add `mustChangePassword` to `ParticipantStatus` (from the account `GroupViewer` already loads: true only with an account on a temporary password), to `ParticipantDto` (default false) and to its mapping in `GroupRoutes`, with tests in `GroupManagementTest`/`LifecycleTest`, `GroupDtoSerializationTest` and the group route tests that verify true for a new account, false after the person changes the password, false with no account and in a Draft, the owner-only 404, and that no password text appears; update fixtures that assumed name-and-address completeness
- [x] 1.3 Update the README (the `profileComplete` explanation, the new `mustChangePassword` flag, delivery info now requiring four fields) and run `./kotlin test`; verify the README states each and the server tests pass

## 2. The delivery info page

- [x] 2.1 In `DeliveryInfoViewModel` check the full name, email, phone and address before sending and report every missing field next to it (the email and length checks as now), allow delivery information to stay empty, and mark the four labels as required in `DeliveryInfoScreen`; update `DELIVERY_WHY`, `DELIVERY_INCOMPLETE` and the page's "more directly" condition to the four fields, with tests in `DeliveryInfoViewModelTest` that verify each blank required field blocks the send with its own error, all errors appear at once, a form with blank delivery information is sent, and the explanation changes with completeness

## 3. The owner's marks and count

- [x] 3.1 Add a `KeyIcon` `ImageVector` in `ui/form` (the pin is the existing `PlaceIcon`); add `mustChangePassword` to `AdminParticipant`; add pure `ParticipantMarks` and `markCounts` in `ui/admin` (no account is `*` only, key and pin need an account and a non-Draft group) and the legend and count strings, with tests that verify each mark, both icons together, no-account only `*`, Draft showing none, the counts with a 0 left out and nothing when both are 0, and the legend listing only the marks present
- [x] 3.2 Use the marks in the Draft chips, the Pending rows, the pair list and the pair sheet titles in place of `*`/`usernameLabel`: inline icons with spoken descriptions, the single legend under each list and the count line above it, with no tap or filter; verify in a Draft, a Pending, an Active and an Archived group that the icons, legend and count match the rules, that a screen reader reads the icons, that rows stay readable on a narrow screen, and that no action is blocked by a mark

## 4. The user sheet

- [x] 4.1 Add a pure `missingFields` over `OversightDeliveryInfo` and show "Missing: …" in the sheet's delivery info part only when some but not all four required fields are set, with tests that verify each combination, none shown when all four are set or when only delivery information is empty, and the existing "has not added delivery info yet" sentence when nothing is set; verify in the sheet

## 5. Check

- [x] 5.1 Run `./kotlin test` and `openspec validate flag-temporary-password-and-incomplete-delivery-info --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`, and look at the lists, the count, the sheet and the delivery info page in light and dark on Android, iOS and the web

## Workflow follow-up

- Archive the change after looking at the marks, the count and the page.
