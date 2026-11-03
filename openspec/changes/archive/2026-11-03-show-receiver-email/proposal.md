# Proposal

## Why

A Secret Santa posts a gift to their receiver, and delivery often needs a way to reach the receiver (a courier asks for an email, a parcel needs a notification address). The receiver sheet already gives the Santa the receiver's name, address, phone and delivery information, but deliberately leaves the email out, so the Santa has nothing to use when a phone number is missing.

## What Changes

- `GET /groups/{id}/receiver` additionally returns the receiver's email address when they have set one, as currently stored; it stays empty when unset.
- The receiver sheet shows the email, labelled, among the receiver's details when it is set. An email alone counts as a postal detail, so the "has not added postal details yet" message no longer shows when only an email is set.
- The Profile tab's "why" note says that a Santa also sees the person's email, in addition to using it to send the draw.
- Password data and the admin flag remain excluded from the receiver response; the Santa's reveal stays username and full name only; the draw email is unchanged.
- No opt-in toggle: the email is shared whenever it is set, like the phone number.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `gift-visibility`: the receiver details now include the email address; the "No email or credentials" scenario becomes "No credentials".
- `client-group-pages`: the receiver details sheet shows the email when set and counts it as a postal detail.
- `client-profile`: the Profile tab's note says a Santa also sees the person's email.

## Impact

- `core/domain`: `ReceiverDetails` (`Questions.kt`) and `GetMyReceiver` (`GiftVisibility.kt`) carry `email`.
- `core/contract`: `ReceiverDto` gains an optional `email` (backward compatible, defaults to null).
- `server`: `GiftRoutes.kt` maps the new field.
- `app/shared`: `ReceiverInfo` and `hasPostalDetails` (`GiftModels.kt`), the receiver sheet (`PairSheets.kt`) and the strings (`Strings.kt`: new label, updated `PROFILE_WHY`).
- Tests for the above (domain, DTO serialization, routes, view model, API client) and the specs.
- Privacy: existing users' emails become visible to their Santa without a new consent step; the updated Profile note is the disclosure.
