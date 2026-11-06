# Proposal

## Why

The owner of a group cannot tell, from its lists, who still has to change a temporary password or fill in their delivery info, and what counts as complete is only a name and an address, which is not enough for a gift to reach someone (no email for the draw, no phone for the courier). The owner needs two clear signs per person, and the person needs to be asked for all they must give.

## What Changes

- The owner's group view carries `mustChangePassword` for every participant: true while their account is on a temporary password. No password is revealed.
- "Delivery info complete" now means full name, email, phone and address, all four. Delivery information stays optional. `profileComplete` keeps its name and gets this meaning.
- In the owner's lists a username is followed by small icons instead of `*`: a key while the account is on a temporary password, a pin while the delivery info is incomplete. `*` stays only for "no account". Each icon is named for screen readers, and a legend under the list gives each mark shown and what it means.
- A plain count line above the list ("2 on a temporary password, 3 with delivery info incomplete"), with no filter and no tap.
- The owner's user sheet lists what is missing ("Missing: email, phone") beside what is set.
- Saving delivery info now requires the full name, email, phone and address; only the delivery information may stay empty. The server refuses a save with a blank required field, naming it, and the page marks the four fields as required, checks them before sending and updates its explanation to the four details. Existing incomplete info stays readable; its owner must fill all four before a save succeeds.
- Not changed: who can read what, any filter, notifications, and the Users tab.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `delivery-info`: "Delivery info belongs to a group" and "Edit own delivery info" are modified: four fields required on save.
- `groups`: "List and view owned groups" is modified (the completeness rule), and "The owner sees who still has a temporary password" is added.
- `client-delivery-info`: "The Delivery Info page" and "Delivery info is checked before it is sent" are modified, "Why the name and the address matter" is replaced by "Why the four details matter".
- `client-admin-groups`: "Marks on participants" and "Marks in the pair list" are modified.
- `client-admin-oversight`: "The draw" and "Marks on usernames in the oversight lists" are modified, "The marks are counted" and "The user sheet names the missing delivery info" are added.

## Impact

- Server and client. `core/domain` (`DeliveryInfo`, the update use case, `ParticipantStatus`/`GroupViewer`), `core/contract` (`ParticipantDto`), `server` (the group routes' mapping), the delivery info form and page, the owner's lists, count, legend and user sheet, two icons in `ui/form`, strings, README and tests, including fixtures that saved partial delivery info. No database change; an old client ignores the new field and an old record is simply incomplete.
