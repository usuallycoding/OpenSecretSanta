# Tasks

## 1. The group page's actions

- [x] 1.1 In the header row of `AdminGroupScreen.kt`, replace "Edit details", "Send announcement", "Get passwords" and "Delete" with outlined icon buttons (the core pencil, the core bell, the app's `KeyIcon`, the core trash can with outline and icon in the error colour) in that order, each spoken by its old label and keeping its conditions and its disabled rule, and put the step button last in the same wrapping row; verify on a device, in light and dark, on Android, iOS and the web: the icons and their order in a Draft, a Pending, an Active and an Archived group, the step last and wrapping on a narrow screen, the key and the trash can disabled while busy, and each still doing what it did (open the edit, the announcement sheet, the passwords dialog, the delete confirmation)
- [x] 1.2 Run `./kotlin test` and `openspec validate use-icon-buttons-on-admin-group-page --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`
