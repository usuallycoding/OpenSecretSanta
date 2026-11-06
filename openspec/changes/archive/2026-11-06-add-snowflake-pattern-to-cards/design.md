# Design

## Context

Cards are drawn with Material `Card` in 13 places across `ui/admin`, `ui/groups`, `ui/gift` and `ui/wishlist`; some take `onClick`, one has custom elevation and semantics, and their content is a `Column` or a `Row`. Icons the core set lacks are drawn as `ImageVector`s; `SnowflakeIcon` is one of them and `SecretSantaCard` draws it in a `Box` with end padding that keeps text clear. The falling snow has a pure, tested generator (`Snowfall`) apart from its drawing. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- One place that gives every card the pattern, so a new card gets it by using the wrapper.
- Placement that is pure and testable, and stable per card.

**Non-Goals:**
- No animation, no link to the snow setting, no setting to turn the pattern off.
- No change to the card's colours, shape, padding or the spacing of its content.

## Decisions

- **`SnowCard`, a wrapper over `Card`, with and without `onClick`.** It takes the same arguments as `Card` plus a `seed`, so each call site changes only its name. Its content stays a `ColumnScope`, exactly as `Card`'s own, so the layout of content already written does not change. Alternative: a modifier on each `Card`; rejected because a modifier on the `Card` draws under its surface fill.
- **Drawn with `drawBehind` on a `Column` inside the card.** The pattern is behind the content and over the surface, adds no layout, semantics or pointer input, and the card's own shape clips flakes that cross the edge, so clipping needs no code. Alternative: an `Icon` per flake in a `Box`; rejected for the extra layout nodes, semantics to clear and cost per card.
- **Flake shapes as `ImageVector`s in `SnowflakeIcon.kt`, drawn through `rememberVectorPainter`.** Five shapes, all 6-fold: the current dendrite, a star with one long V, a plate (hexagon with spokes), a fern (arms with crossbars) and a dotted one (round tips, a zero-length round-cap stroke being a dot). The solid Material "AC unit" shape is left out: it is much heavier than the others. The painters are made once and shared by every card. Stroke scales with the flake, so a large flake has a thicker line.
- **Placement is a pure function, `SnowflakePattern.flakes(seed, width, height)`.** Like `Snowfall`: no drawing, no clock, the same list for the same inputs, so it is unit tested. It lays a jittered grid over the card, one flake per cell at a random point of the cell, so they are spread rather than bunched, and lets a centre fall on or just past the card's edge so some are cut off. Each flake gets a shape, a size from about 14dp to 64dp, an angle and an alpha. Cell size sets the density per area, as `Snowfall.AREA_PER_FLAKE` does. Positions are fractions of the card, so the list is computed once per size and not on every draw. Alternative: independent random points; rejected because they clump and leave holes.
- **A stable seed given by the caller.** Rows pass their id (the group's or user's), items pass their id or position, and a card with nothing to tell it apart passes a constant for its screen. The seed is a value, not a `remember`ed random, so scrolling out and back, refreshing and recomposition give the same pattern, and two rows differ. Alternative: `rememberSaveable` random seeds; rejected as it is lost when a card leaves a list and is no help in a sheet that is recreated.
- **A soft theme colour.** The theme's secondary colour at an alpha of about 0.08 to 0.14, a little fainter for the larger flakes, so the pattern reads in both themes yet text over it keeps its contrast. Many flakes are fainter than the one before, which was 0.3.
- **The corner clearance goes.** `SnowflakeClearance`, `SnowflakeSize`, `SnowflakeInset`, the corner `Icon`, the `Box` and the end padding on the heading and name in `SecretSantaCard` are removed, with their comment.

## Risks / Trade-offs

- [Text over a flake is a little harder to read] → low alpha, the flakes are thin lines, and buttons are opaque; check both themes with a name and a long text over a large flake.
- [A list of many cards costs more to draw] → one draw pass per card, a few painter draws, flake lists cached per card size and the painters shared; only visible cards are drawn.
- [A card with no id shows the same pattern as another such card] → they are different screens' constants, and cards in one list use their ids.
- [Cards whose size changes, as when text wraps, re-place their flakes] → positions are fractions of the card, so they move with it; acceptable, as that is a layout change anyway.
