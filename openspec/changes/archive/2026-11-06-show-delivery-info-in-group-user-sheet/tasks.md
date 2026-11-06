# Tasks

## 1. Server

- [x] 1.1 Add `ADMIN_GROUP_PARTICIPANT_DELIVERY_INFO` and its path helper to `ApiRoutes`, and `GetParticipantDeliveryInfo` in `core/domain/delivery` (owned group, Draft refused with `invalid_state`, a non-participant is not found, returns the participant's saved details or empty ones, writes nothing), with a test in `core/domain/test` next to `GetParticipantWishlistTest` that verifies all five fields read, nothing set reading empty, Pending/Active/Archived readable, Draft refused, a non-participant and another owner's group not found, and a person's other groups not leaking in
- [x] 1.2 Serve it from `AdminOversightRoutes.kt` as `DeliveryInfoDto` and wire the use case in `ServerModules.kt` and `Application.kt`, with route tests in `AdminOversightRoutesTest` that verify 200 with the fields, 200 with all empty, 409 for a Draft group, 404 for a non-participant and for another admin, 403 for a non-admin, 401 without a token, and `AdminOversightPrivacyTest` that verifies the participant's own delivery info, group view and receiver responses are unchanged after repeated owner reads and that the group view still has only the full name and the completeness flag
- [x] 1.3 Add the route to the README's route table beside the wishlist one, and mention in the delivery info text that the owner can read one participant's details; verify `README.md` lists it with "owner" and the states it works in

## 2. Client

- [x] 2.1 Add `ApiClient.adminParticipantDeliveryInfo(groupId, username)` and `AdminParticipantDeliveryInfoViewModel` in `ui/admin` (a `Loader` over it, as `AdminParticipantWishlistViewModel`), register it in `AppModules.kt`, with tests in `AdminOversightApiTest` and `AdminOversightViewModelTest` (and `AppModulesTest` if it lists view models) that verify the request is a GET of the right path, the fields map, and that a failure of this part leaves the account part's state untouched and the reverse
- [x] 2.2 Show the delivery info in `UserSheet` under `UserDetail`: pass the group id from `GroupSheetHost`, load both parts apart on open, show the labelled fields leaving out what is unset, the "has not added delivery info yet" line when none is set, and per part loading, the 404 and 409 messages and "Try again"; add the strings; verify in an Active, a Pending and an Archived group that the sheet shows username, temporary password and Reset as before with the delivery info below, that the Users tab detail is unchanged and has none, and that a failing part does not hide the other

## 3. Check

- [x] 3.1 Run `./kotlin test` and `openspec validate show-delivery-info-in-group-user-sheet --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`, and look at the sheet in light and dark on Android, iOS and the web

## Workflow follow-up

- Archive the change after looking at the sheet in a Pending, an Active and an Archived group.
