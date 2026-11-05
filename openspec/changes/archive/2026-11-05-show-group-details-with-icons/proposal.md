# Proposal

## Why

The group card reads as a list of questions: "When? - ", "Where? - ", "Budget? - ". An icon says the same thing faster and takes less room, and the description is prose, which does not belong in a card of short facts.

## What Changes

- In the group information card the words "When? - ", "Where? - " and "Budget? - " are replaced by a calendar, a map pin and a bank icon at the start of each line. The words are not shown, but a screen reader still reads them.
- The description moves out of the card. It is shown above the card, under the name and the state with its note, as plain text.
- The card holds only the date and time, the location and the budget. A group with none of them shows no card, even when it has a description.
- Both places that show the card change together: the group page and the admin group screen.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-groups`: the card's lines start with icons instead of words, the description is outside the card, and the requirements that named the labels are renamed to say icons.
- `client-admin-groups`: the admin group screen shows the description above the card and the icons in it, as the group page does.

## Impact

- Client only: `GroupInfoCard.kt`, three new icon files in `ui/form`, and the places that call the card (`GroupPage.kt`, `AdminGroupScreen.kt`) if its shape changes. No server, contract or database change.
- The strings `WHEN_LABEL`, `WHERE_LABEL` and `BUDGET_LABEL` stay, as what a screen reader says.
