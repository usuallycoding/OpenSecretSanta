# Proposal

## Why

The way into a person's Gifter or Receiver page, and into an administrator's view of a pair, is a bare arrow between two usernames. It looks like a decoration that says who gives to whom, not like something to press, so people do not find the pages behind it. A group of icons that reads as one button, with the gift in the middle, says what is behind it and invites the tap.

## What Changes

- Everywhere a row shows `giver -> receiver` with an arrow that can be pressed, the arrow is replaced by one **pair link**: a compact tonal pill holding an arrow, a gift icon and an arrow (`->[gift]->`), pressed as a whole.
- This is in three places: the first row of a participant's group page (opens the Gifter page), its second row (opens the Receiver page), and every row of an administrator's pair list (opens the pair's sheet).
- The pair link is one shared piece of the app, so the three look and behave the same. It is a real button: the whole pill is the touch target and shows when it is pressed, and a screen reader announces it once, with what it opens, instead of three separate icons.
- Where the names sit does not change: the first name at the start of the row, the second at the end, the pair link between them.
- The lone gift icon of the Pending row, and the gift icon beside each participant in an administrator's list, stay as they are: they are not arrows and are not a group.

## Capabilities

### New Capabilities
- `client-pair-link`: how the pair link looks and behaves wherever it is used: its three icons, the tonal pill, being one touch target with a pressed state, its size, its spoken label and its direction in right-to-left languages.

### Modified Capabilities
- `client-group-pages`: the pair rows show the pair link instead of arrows, and the Gifter and Receiver pages open from it.
- `client-admin-oversight`: the rows of the pair list show the pair link instead of an arrow, and the pair sheet opens from it.

## Impact

- **Shared client code**: a new shared composable for the pair link; `PairRows` (participant group page) and `PairList` (administrator's pair list) use it in place of their arrow buttons.
- **No server, API or data change**, and no new strings beyond what the existing spoken labels already are.
- **Layout**: the link is wider than the arrow button it replaces (about 90dp instead of 48dp), so the two names have a little less room in a row; long usernames may wrap sooner.
- **Not affected**: what the Gifter page, the Receiver page and the pair sheet show, the lone gift icons, and every other screen.
