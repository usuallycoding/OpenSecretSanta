# Tasks

## 1. Flake shapes and placement

- [x] 1.1 Replace the single `SnowflakeIcon` in `ui/form/SnowflakeIcon.kt` with a list of five 6-fold shapes (dendrite, star with one long V, plate, fern, dotted), dropping the commented-out AC-unit path; verify with a `@Preview` or a throwaway screen that each shape draws as a snowflake at 14dp and at 64dp
- [x] 1.2 Add a pure `SnowflakePattern` beside `Snowfall` in `ui/snow` that, for a seed and a card size in dp, returns the flakes (shape, centre, size 14-64dp, angle, alpha) from a jittered grid with centres allowed on or past the edge and a density per area, with a test in `app/shared/test/ui` (next to `SnowfallTest`) that verifies the same input gives the same flakes, different seeds differ, a larger card has more flakes at about the same density, all shapes are used over a large card, some flakes cross the edge and none has its centre far outside

## 2. The card

- [x] 2.1 Add `SnowCard` in `ui/form`, with and without `onClick`, taking `Card`'s arguments plus a `seed`, drawing the pattern in one `drawBehind` on an inner `Column` with `rememberVectorPainter` shapes shared by all cards and the theme's secondary colour at a faint alpha, with no semantics or pointer input; verify a card with it is the same size and lays out its content the same as the plain `Card`, and that a screen reader does not read a flake
- [x] 2.2 Remove the corner flake from `SecretSantaCard` in `PairCards.kt` (the `Box`, the `Icon`, `SnowflakeSize`, `SnowflakeInset`, `SnowflakeClearance` and the end padding) and use `SnowCard` there and for the "Your stuff:" card; verify the card in a Pending, an Active and an Archived group shows the pattern behind its text and buttons, and that its text starts where the plain card's did

## 3. Every other card

- [x] 3.1 Use `SnowCard` for the group and user rows (`GroupsScreen.kt`, `AdminGroupsScreen.kt`, `AdminUsersScreen.kt`) with the group's or user's id as the seed; verify the rows stay clickable, scrolling a long list out and back keeps each row's pattern, and two rows differ
- [x] 3.2 Use `SnowCard` for `GroupInfoCard`, the delivery sheets (`PairSheets.kt`), the oversight sheets (`OversightSheets.kt`), `QuestionCard` (`ReceiverScreen.kt`), `ReceiverWishlistScreen.kt` and the wishlist item card (`WishlistScreen.kt`, keeping its elevation and semantics) with an id or item position as the seed, or a constant for the screen where there is none; verify no `Card(` call is left in `ui/` and that dragging a wishlist item still works
- [x] 3.3 Run `./kotlin test` and `openspec validate add-snowflake-pattern-to-cards --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`, and look at the app on Android, iOS and the web in light and dark, with "Make it snow" on and off, for text that is hard to read over a flake

## Workflow follow-up

- Archive the change after looking at the cards in both themes.
