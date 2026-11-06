# Design

## Context

`SecretSantaCard` is a `Card` around a padded `Column`: heading, the name or "TBD", an italic line and buttons. The project draws icons it lacks as `ImageVector`s from Material path data (`CalendarIcon`, `PlaceIcon`, `BankIcon`). The falling snow of `client-appearance` is a separate overlay. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- A snowflake in the corner that never changes the card's layout or hides text.

**Non-Goals:**
- No link to the snow setting, no animation, no snowflake on the other card.

## Decisions

- **A vector from the Material "AC unit" path, as `SnowflakeIcon`.** Same approach as the other icons: no new dependency and the same look as the rest of the set.
- **Drawn in a `Box` around the column, aligned top end.** The column keeps its layout, so the card's height does not change, and the icon sits behind the content in the `Box`'s order so it can never cover text. About 40dp, with a small inset from the corner.
- **The text keeps clear of it.** The heading and the name get end padding of the icon's width plus its inset, so a long username wraps or is cut before the flake; the buttons below use the full width, as they are lower than the flake.
- **A soft colour from the theme.** The theme's secondary colour at low opacity, so it reads in both light and dark and stays quieter than the text.
- **Decorative means silent.** The icon has no content description and is cleared from the semantics tree, and it takes no pointer input, so it can be neither read nor pressed.

## Risks / Trade-offs

- [The end padding on the heading and name narrows them] → only by the icon's width; a long username wraps within the space left, as it already wraps at the card's edge.
- [Two kinds of snow, the setting and the flake] → they are independent by design; the spec says so.
