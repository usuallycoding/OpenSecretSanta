# Tasks

## 1. The button and its dialog

- [x] 1.1 Add `getPasswords()` to `AdminGroupViewModel`: load `api.adminUsers()`, keep the group's participants who have a temporary password in participant order, open the passwords dialog (also when none), disable the button while loading, and show the unreachable notice on failure without a dialog; verify view-model tests cover several people, one who changed their password, one in another group only, nobody, an unreachable server and the busy state
- [x] 1.2 Add the "Get passwords" button after "Send announcement" for a Pending or Active group, and give the dialog its on-demand and "nobody" texts; verify a Draft and an Archived group have no button, and look at the dialog on a Pending group with new accounts
- [x] 1.3 Run `./kotlin test` and `openspec validate add-get-passwords-button --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`

## Workflow follow-up

- Archive the change after trying the button on a Pending group.
