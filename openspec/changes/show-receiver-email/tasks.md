# Tasks

## 1. Domain, contract and server

- [x] 1.1 Add `email` to `ReceiverDetails` (`core/domain/src/gift/Questions.kt`) and fill it from `account.profile.email` in `GetMyReceiver` (`GiftVisibility.kt`); extend `core/domain/test/GiftVisibilityTest.kt` with a receiver that has an email and one that has none, and verify `./kotlin` domain tests pass
- [x] 1.2 Add `val email: String? = null` to `ReceiverDto` (`core/contract/src/GiftDtos.kt`) and update the key-set assertion in `core/contract/test/GiftDtoSerializationTest.kt`, including that a payload without `email` still decodes; verify the contract tests pass
- [x] 1.3 Map `details.email` in `server/src/presentation/GiftRoutes.kt`; update `server/test/GiftVisibilityRoutesTest.kt` (expected keys now include `email`, a receiver with an email returns it live after a change, an unset email is empty, and the password/admin-flag exclusion check still holds, replacing the old "no email" assertion); verify the server tests pass

## 2. App

- [x] 2.1 Add `email` to `ReceiverInfo`, `ReceiverDto.toInfo()` (cleaned like the other fields) and `hasPostalDetails` in `app/shared/src/ui/gift/GiftModels.kt`; extend `app/shared/test/ui/ReceiverViewModelTest.kt` and `GiftApiTest.kt` to cover an email set, a blank email treated as unset, and an email-only receiver counting as having postal details; verify the app tests pass
- [x] 2.2 Add `RECEIVER_EMAIL = "Email"` and show it with `DetailField` in the receiver sheet (`app/shared/src/ui/groups/PairSheets.kt`, `ReceiverDetails`); verify by opening the sheet for a receiver with only an email (shows the email, no "not added postal details" message) and for one with nothing set (shows the message)
- [x] 2.3 Reword `PROFILE_WHY` in `app/shared/src/ui/Strings.kt` to say a Santa also sees the person's email besides using it to send the draw, without hinting who the Santa is; update any test asserting the old text and verify the Profile tab shows the new note

## 3. Specs and integration

- [x] 3.1 Run `openspec validate show-receiver-email --strict` and verify it passes, then run the full build and test suites for core, server and app and verify they are green
