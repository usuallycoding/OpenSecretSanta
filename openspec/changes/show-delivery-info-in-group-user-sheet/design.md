# Design

## Context

The owner's oversight reads are small owner-only routes under `/admin/groups/{id}`, each a use case over `OwnedGroupLoader` (`GetParticipantWishlist`: Draft is 409 `invalid_state`, a non-participant is 404, a non-owner sees a missing group) wired in `ServerModules.kt` and `Application.kt` and served by `AdminOversightRoutes.kt`. Delivery info is stored per group and person (`DeliveryInfoRepository.find`), with an existing `DeliveryInfoDto`. The group's user sheet (`UserSheet` in `OversightSheets.kt`) shows only `UserDetail` from `GET /admin/users/{username}`; the pair sheet already loads two parts separately, each with its own view model. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- The owner reads one participant's whole delivery info on request, read-only, leaving no trace.
- The group's user sheet shows it under the account part, each part loading and failing on its own.

**Non-Goals:**
- No change to `ParticipantDto` or the group view, no write route, no delivery info on the Users tab, and no new way for anyone but the owner to read it.

## Decisions

- **A new route beside the wishlist one, not new fields on the group view.** `GET /admin/groups/{id}/participants/{username}/delivery-info` reads one person on tap, so a list load or a search never carries anyone's address. Alternative: add the fields to `ParticipantDto`; rejected as it sends everyone's details to the client on every load and widens the exposure far beyond what a sheet needs.
- **Same rules as the wishlist route.** Draft answers 409 `invalid_state`, a non-participant 404, and a non-owner is told the group does not exist (404), a non-admin 403. This keeps the sheet's error handling (`Oversight errors and refreshing`) unchanged: 404 means "no longer available" and 409 "not in a state where this can be read". The proposal's wording of "404 for a Draft group" is read as the pattern's 409, which is already how the other oversight routes answer a Draft group.
- **A use case `GetParticipantDeliveryInfo` in `core/domain/delivery`.** It loads the owned group, refuses Draft, checks the username is a participant and returns `DeliveryInfoRepository.find(group.id, participant)`, which is empty details for someone who saved nothing. It writes and records nothing, as `GetParticipantWishlist` does. The route returns the existing `DeliveryInfoDto`, so no new DTO.
- **Two view models in the sheet, one per part.** `UserSheet` keeps `AdminUserViewModel` for the account part and adds `AdminParticipantDeliveryInfoViewModel(groupId, username)` for the delivery info, both reloaded when the sheet opens, mirroring `AdminParticipantWishlistViewModel`. A failure of one leaves the other shown, each with its own loading, message and "Try again". The sheet needs the group id, which `GroupSheetHost` already has, so `GroupSheet.User` is unchanged.
- **Shown as the receiver's sheet shows it.** Name, email, phone, address and delivery information as labelled fields in a card, leaving out what is not set; when nothing is set a line says the person has not added delivery info yet, in new wording for the owner (the receiver's "Your receiver has not added postal details yet." speaks to a Santa). Nothing is editable.
- **The Users tab is untouched.** It has no group to read from; `UserDetail` stays the account part, and the sheet composes it with the new part.

## Risks / Trade-offs

- [This reverses the earlier rule that the owner never sees a participant's email, phone and address] → chosen explicitly; it is limited to the group's owner, to one person per request, never writes, and the group view keeps its narrow fields. The delivery-info and admin-oversight specs say so.
- [An admin who is not the owner could try the route] → the same owned-group check as the other oversight routes: 404 for another admin, 403 for a non-admin, with tests.
- [The read must leave no trace] → no write path in the use case; a privacy test compares the participant's own responses before and after repeated owner reads, as for the wishlist.
- [Two parts in a sheet mean two loading states] → each shows its own progress, so the account part, which people use most, never waits on the other.
