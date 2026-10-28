# Proposal

## Why

Four things get in the way when someone types in the app. On the Profile page the keyboard covers the Save button and nothing lets the person scroll it away, so the form cannot be saved without closing the keyboard some other way. The address is a multi-line field whose Return key jumps to the next field instead of starting a new line, so an address cannot be written on several lines. The address and delivery fields grow without limit while every other multi-line field stops at six rows. And on iOS, in every field inside a bottom sheet, the cursor jumps back before the first character as soon as it is typed, so "abcd" becomes "bcda".

## What Changes

- **Save stays reachable.** On the Profile page, and on the other plain forms (Change password, Sign in, the group form), the content lifts above the keyboard and scrolls, so the primary button can always be reached.
- **Dragging dismisses the keyboard.** A drag that scrolls a form or a sheet closes the keyboard. Scrolling that the app does itself to bring a field into view does not, and a drag inside a field that only scrolls the field's own text does not either.
- **Line breaks in the address.** Return in the address field starts a new line instead of moving to the next field. The server already keeps line breaks, and the Santa sees the address as written.
- **Six rows, then scroll.** Every multi-line field is at least as tall as its minimum, grows with its text up to six rows and then scrolls inside itself. The Profile address and delivery information fields get the cap the other multi-line fields already have, from one shared field used everywhere.
- **Typing in a sheet keeps the cursor.** On iOS, typing "abcd" into any field of a bottom sheet gives "abcd". The cause is not known yet, so this is investigated on the iOS simulator, with a short list of candidate fixes tried in order until typing is right in every sheet field; the person who reported it checks each attempt on iOS.

Out of scope:
- Server changes, new limits on text length, tap-outside-to-dismiss, and any change to how fields are validated.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `client-profile`: the address and delivery information fields take several lines.
- `client-app`: forms stay usable with the keyboard open, dragging dismisses the keyboard, multi-line fields stop growing at six rows, and typing in a sheet keeps the cursor.

## Impact

- Code: a shared form container and a shared multi-line field in `app/shared`, used by the Profile, Change password, Sign in and group form screens and by the wishlist, question, answer and participants sheets; the iOS entry point or the sheet field code for item 4; a domain test that a line break inside an address is kept; README.
- APIs: none.
- Dependencies: none expected.
- Platforms: Android and web are checked by running them; iOS is checked by the person who reported the cursor bug, because typing cannot be driven there from this environment.
