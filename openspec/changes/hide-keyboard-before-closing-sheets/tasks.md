## 1. Spike

- [x] 1.1 Find how to receive taps and drags on the dimmed area of a `ModalBottomSheet`, and how to stop the sheet's own drag while the keyboard is open, on Android and iOS; record the choice in `design.md`

## 2. The shared sheet

- [x] 2.1 Add `AppBottomSheet` to `app/shared/src/ui/form/SheetSupport.kt`: while the keyboard is open, a tap on the dimmed area hides the keyboard and does not dismiss, and any drag on the sheet that nothing used hides it and does not move or close the sheet
- [x] 2.2 With the keyboard closed it dismisses, drags and scrolls exactly as `ModalBottomSheet` does

## 3. Use it

- [x] 3.1 Replace the `ModalBottomSheet` calls in `SheetFrame.kt`, `TextSheet.kt`, `ItemSheetHost.kt` and the participants sheet in `AdminGroupScreen.kt`

## 4. Check

- [x] 4.1 Run `./kotlin test -m shared` and `./kotlin build`
- [x] 4.2 Check on Android and iOS with the keyboard open in a short text sheet, a scrolling one and a read-only one: swipes in every direction on the sheet, a tap on the dimmed area, then again with the keyboard closed, and Cancel and Save still work
