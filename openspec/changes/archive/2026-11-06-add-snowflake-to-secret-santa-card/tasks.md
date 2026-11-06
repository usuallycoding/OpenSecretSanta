# Tasks

## 1. The snowflake

- [x] 1.1 Add `SnowflakeIcon` in `ui/form` from the Material "AC unit" path and draw it in the top right corner of `SecretSantaCard` in a soft theme colour, with end padding on the heading and the name so no text runs under it, no content description, cleared semantics and no pointer input; verify it shows in a Pending, an Active and an Archived group, in light and dark, with a long username, and not on the "Your stuff:" card
- [x] 1.2 Run `./kotlin test` and `openspec validate add-snowflake-to-secret-santa-card --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`

## Workflow follow-up

- Archive the change after looking at the card in both themes.
