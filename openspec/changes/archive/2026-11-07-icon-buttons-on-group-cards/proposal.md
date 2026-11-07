# Proposal

## Why

The two cards on the group page, "You're Secret Santa for:" and "Your stuff:", are a stack of wide text buttons. Most of what they open has a well-known symbol, and icons say it in less room and leave the cards lighter, as the owner's group page already does for its actions.

## What Changes

- The actions of the two cards become **outlined icon buttons** with no text, in one row at the start of each card, in this order:
  - the **gift** for "See Wishlist" and "Edit Wishlist",
  - the Material **send** paper plane (the icon the wide-mode "Send letter" button already uses) for "Ask Questions" and "Respond to Santa",
  - a **mail** icon for "Inbox" and "Send letter",
  - the outlined **map pin** for both "Delivery Info" buttons.
- Each button's former label is its **spoken name** and its **tooltip**, shown on hover and on a long press on a touch screen.
- The **count badges** stay on the corner of their buttons (See Wishlist, Ask Questions and Inbox on the Santa card, Respond to Santa on the other), with the count still spoken with the name, and a button is disabled where it was (the Santa card's buttons while the receiver loads).
- "Reveal Santa" stays a **text button**, above the row.
- The message that asks for missing delivery info stays directly above the row, and while it is shown the "Delivery Info" icon button is drawn **red** too (its outline and its icon), so the nudge reaches the button.
- Before the draw a Pending group's "Your stuff:" card has only the gift and the pin, as it had two buttons then.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-group-pages`: new requirements for the icon buttons and for their names, tooltips, badges and states; "The Secret Santa card", "The Your stuff card" and "The Your stuff card asks for missing delivery info" say where the buttons sit and that the Delivery Info button is red with the message. The requirements that name the buttons (their scenarios, the badges of `client-read-marks`) stay true, because the names are kept.

## Impact

- `app/shared/src/ui/groups/PairCards.kt` (the two cards), a new `MailIcon` in `ui/form`, and a small shared button for a card action (outline icon button, tooltip, badge, spoken name, disabled, red).
- Strings: the labels stay as they are (they become the names and tooltips); no new text.
- No server, contract or data change. The wide-mode page buttons and floating buttons are not touched.
