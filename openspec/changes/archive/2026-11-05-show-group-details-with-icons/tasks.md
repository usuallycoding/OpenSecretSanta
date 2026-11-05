# Tasks

## 1. Icons

- [x] 1.1 Add the calendar, map pin and bank icons as `ImageVector`s in `ui/form`, from the Material path data, like `DragHandleIcon`; verify they compile on every platform and read as the right shapes when drawn in the app

## 2. Card

- [x] 2.1 Draw the description as plain text above the card, in `GroupInfoCard`, with its "Description" label for screen readers, and no card when the group has none of the date and time, location or budget; verify a group with only a description shows the text and no card
- [x] 2.2 Replace the words with the icons in each line, in a row with the icon in its own column, the line read as `When? - ...`, `Where? - ...`, `Budget? - ...` by a screen reader and the icon silent; verify `GroupFormatTest` still passes for the spoken text and look at the card on the group page and the admin screen, including a long location
- [x] 2.3 Run `./kotlin test` and `openspec validate show-group-details-with-icons --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`

## Workflow follow-up

- Archive the change after looking at both screens.
