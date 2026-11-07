# Tasks

## 1. The button

- [x] 1.1 Add `MailIcon` (an `ImageVector` of the Material "mail" icon) to `ui/form`.
- [x] 1.2 Add `CardIconButton` to `ui/groups`: an outlined icon button with a tooltip (`TooltipBox` with a plain tooltip), a count badge through `CountBadged`, a spoken name that carries the count, a disabled state and an error (red) state, with no text.

## 2. The cards

- [x] 2.1 In `SecretSantaCard` replace the text buttons with one row of `CardIconButton`s in the order gift (See Wishlist, with its count), send (Ask Questions, with its count), mail (Inbox, with its count) and pin (Delivery Info), disabled while the receiver is unknown; keep "Try again" as it is.
- [x] 2.2 In `YourStuffCard` do the same: gift (Edit Wishlist), then from Active on send (Respond to Santa, with the unanswered count), mail (Send letter) and, last, the pin (Delivery Info), red while the missing-delivery-info message is shown; keep "Reveal Santa" a text button above the row and the message directly above the row, below it.

## 3. Verification

- [x] 3.1 Build for Android, iOS and the web and run the shared tests and `openspec validate --specs`.
- [x] 3.2 Look at both cards in a Pending, an Active and an Archived group in both themes: the order and icons, the badges on the corners, the tooltips on hover (web) and on a long press (touch), a screen reader reading "Inbox, 3 new" and "Respond to Santa, 2 unanswered", the buttons disabled while the receiver loads, and the Delivery Info button red with the message and normal without it.
