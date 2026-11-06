# Proposal

## Why

Delivery info now needs a full name, email, phone and address, and many people saved less before. The group page offers a plain "Delivery Info" button, so someone with details missing has no sign that anything is wanted of them, and a gift cannot reach them. A clear message right where they would act, shown only to those who need it, closes that gap.

## What Changes

- On the group page, directly above the "Delivery Info" button of the "Your stuff:" card, an error message reads "Please fill out your delivery info in order to be able to receive your secret gift!", in the theme's error colour, for a person whose delivery info for the group lacks the full name, email, phone or address. The delivery information stays optional and never triggers it.
- It shows only in a Pending or an Active group, where the person can still fill the details in, and only once their delivery info has loaded. It does not show while loading, when loading fails, in a Draft, or in an Archived group.
- It goes away once the details are complete: the group page reloads the person's delivery info when it is shown again, on pull to refresh and when the app returns to the foreground, so it clears after saving on the Delivery Info page.
- It is plain text above the button, not a control, and blocks nothing.
- Client only: the group page reads the person's own delivery info (`GET /groups/{id}/delivery-info`, which exists). No server or contract change.
- Not changed: the Delivery Info page and its own hint, the Secret Santa card, the receiver sheet, the owner's views and push notifications.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-group-pages`: two added requirements, the message and how the group page decides to show it. "The Your stuff card" is unchanged.

## Impact

- Client only: `GroupPage` and `YourStuffCard` in `ui/groups`, a pure check next to `MyDetails`, one string, the README and tests. The group page makes one more request while the group is Pending or Active.
