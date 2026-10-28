# Proposal

## Why

An administrator who has made a group Pending now has accounts to look after: people lose or never receive their temporary password, and the administrator needs to see who still has to change theirs. The server already lets an administrator see the users they manage (people in their non-Draft groups, and themselves) and reset a password, but the app has no screen for it. At the same time, the product decision is that nobody can be made an administrator through the API any more: the first administrator comes from the server's configuration and that is the only way. The promote route is a way to create administrators that is no longer wanted, so it goes, on the server and in the specs, rather than being left unused.

## What Changes

- **Users tab.** The admin view gains a bottom bar with Groups and Users, like the participant view's tabs. Users lists the users the administrator manages (everyone in their Pending, Active or Archived groups, and themselves): username, full name when set, and a note for anyone who still has to change a temporary password.
- **User detail.** Username, full name, email, phone, address and delivery information as far as they are set, and, for someone who has not changed their temporary password yet, that temporary password, selectable so it can be copied. A person who has set their own password shows no password.
- **Reset password.** On a managed user other than the administrator themselves, after a confirmation that says the old password and sessions stop working: the server makes a new temporary password, which the app shows once in a dialog (selectable), with the sentence that the person must change it when they next sign in. Resetting one's own password is not offered.
- **No promotion.** The server's `POST /admin/users/{username}/promote` route, its use case, its contract constant and its tests are removed, and so are the requirement in `user-administration` and the scenarios that exist only because of it. Nothing in the app offers it.
- **Group passwords dialog wording.** The dialog that shows temporary passwords after making a group Pending no longer says they are never shown again: an administrator can look them up under Users until the person changes them. The app still keeps none of them after the dialog closes.
- **Concurrent changes.** A user who has dropped out of the administrator's scope since the list loaded (their last group was archived away or they were removed from it) is shown as no longer available, with a way back.

Out of scope:
- Creating, deleting or editing users, changing someone's profile, demoting, and anything that makes an administrator. Owner oversight (assignments, wishlists, question threads) is the next admin change.

## Capabilities

### New Capabilities
- `client-admin-users`: how an administrator lists and views the users they manage in the app, sees a user's temporary password, and resets a password.

### Modified Capabilities
- `client-app`: the admin view has a Groups tab and a Users tab, and the scenario about being promoted while signed in goes.
- `client-admin-groups`: the temporary passwords dialog's wording.
- `user-administration`: the promote requirement is removed, and its purpose line stops mentioning promotion.
- `authentication`: the requirement that the stored user decides each request loses its promotion example.

## Impact

- Code: three admin user calls in `ApiClient`, two view models and two screens and an admin tab bar in `app/shared`, two typed routes, strings and the README. On the server and in `core`: the promote route, its use case, DI binding, contract constant and tests are deleted, and the authentication test that used promotion is rewritten.
- APIs: **BREAKING** for anything calling `POST /admin/users/{username}/promote`: it answers 404 afterwards. The app never called it.
- Dependencies: none.
- Systems: none. The first administrator is still created from `ADMIN_USERNAME` and `ADMIN_PASSWORD` on an empty database.
