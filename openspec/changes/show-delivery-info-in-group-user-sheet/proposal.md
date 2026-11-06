# Proposal

## Why

When delivery info moved from the account to each group, the administrator's user sheet lost the person's name, email, phone, address and delivery information, and the owner of a group now has no way to read them: the group view carries only the full name and a completeness flag. The owner needs to see what a participant told their group, next to the account details they already manage, so they can help people who got it wrong or left it out.

## What Changes

- New owner-only, read-only route `GET /admin/groups/{id}/participants/{username}/delivery-info`, in the pattern of the participant's wishlist route: it returns that person's delivery info for that group, with unset fields empty, while the group is Pending, Active or Archived.
- The sheet opened by choosing a username in a group's pair list, or the username of a participant with an account in a Pending group, shows, under the account part (username, temporary password while temporary, "Reset password"), that person's delivery info for that group: name, email, phone, address and delivery information, leaving out what is not set, and saying so when none is.
- The sheet loads the two parts separately, so a failure of one does not hide the other, each with its own loading, message and "Try again".
- The Users tab detail stays account-only: username, temporary password and "Reset password", with no personal fields.
- **Reverses** the rule that the owner sees only the full name and a completeness flag of a participant's delivery info. The group view (`GET /admin/groups/{id}`) is unchanged and still carries only those two things; the full details are read one person at a time, on request.
- The read leaves no trace: nothing a participant sees changes, and nothing records that or when the owner read.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `admin-oversight`: one added requirement, the owner reads a participant's delivery info.
- `delivery-info`: "Others read delivery info only as the draw allows" is modified, so that the owner may also read a participant's delivery info through the new route.
- `client-admin-users`: "User detail shows the account" is modified: the Users tab stays account-only, and the sheet opened from a group adds the group's delivery info.
- `client-admin-oversight`: one added requirement, the delivery info part of the user sheet with its empty and failed states.

## Impact

- Server and client. `core/contract` (route and path helper; the existing `DeliveryInfoDto` is reused), a use case in `core/domain/delivery`, the route in `server/src/presentation/AdminOversightRoutes.kt` with its wiring in the server modules, `ApiClient`, a view model and the user sheet in `ui/admin`, strings, the README's route table and tests on both sides. No database change.
