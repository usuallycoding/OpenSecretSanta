# Design

## Context

`KeyboardDismissRoot` (`KeyboardDismiss.kt`) wraps the whole app. It closes the keyboard on a drag, a tap nothing handled, and any vertical scroll that reaches it (`scrollHook(countConsumed = true)`). `close()` skips a touch that started in a text field, but only by comparing `guard.fieldDownAt` with `tracker.downAt`, and `GestureTracker` resets `downAt` to -1 on release.

When the keyboard opens, `imePadding()` shrinks the form and Compose scrolls the focused field into view. That scroll arrives at the hook as `NestedScrollSource.UserInput` with no finger on the screen, and after release `downAt` is -1, so the guard does not match and `close()` hides the keyboard. The form's own `dismissKeyboardOnDrag` (`FormComponents.kt`) already avoids this with a finger-down check. On a short screen every field needs the scroll, so every field is hit.

`SheetContent` (`SheetSupport.kt`) uses the same `scrollHook` with `countConsumed = false`.

## Goals / Non-Goals

**Goals:**
- A scroll with no finger on the screen never closes the keyboard through the root hook.
- A scroll with a finger down behaves exactly as before.
- One definition of "finger is down" in the root, shared by the tap/drag watcher and the scroll hook.

**Non-Goals:**
- Changing `dismissKeyboardOnDrag`, the touch guard, or tap and drag handling.
- Detecting the keyboard's opening or its size (the web does not report it).

## Decisions

**Expose the finger state from `GestureTracker`.** Add a read-only `fingerDown` (true from the first pressed event until release; today that is `origin != null`). `watchGestures` already feeds the tracker every event in the Final pass, so no new pointer listener is needed. Alternative: a second `pointerInput` with its own `FingerTracker`, as `dismissKeyboardOnDrag` has. Rejected: it would duplicate state the tracker already holds, and the two could disagree.

**Give `scrollHook` a finger-down condition.** `scrollHook` takes a `fingerDown: () -> Boolean` and only calls `onScroll` when it is true. `KeyboardDismissRoot` passes `tracker::fingerDown`. `SheetContent` passes its own tracker's too: its drags also have a finger down, and an auto-scroll inside a sheet should not hide the keyboard either. Alternative: add the check only inside `KeyboardDismissRoot`. Rejected: `scrollHook` is the shared place and the sheet has the same exposure.

**Ordering.** The tracker sees the pointer event in the Final pass, after children, and the scroll it causes runs from the same event, so `fingerDown` is already true for a drag. The auto-scroll comes later with the finger up, so it reads false. A drag that starts and ends before a scroll is dispatched is not expected, because a scroll needs the finger moving.

## Risks / Trade-offs

- [A real drag could be reported before the tracker marks the finger down] → the tracker marks it on the first pressed event, before any move; a unit test and the manual check below cover this.
- [A fling that continues after the finger lifts no longer closes the keyboard] → the drag itself already closed it when it started; the previous behaviour for a fling was incidental. Verify on a device.
- [Auto-scroll while the finger is still down, for example a long press on a field] → treated as a drag by this hook, as before; the touch guard still covers a touch that started in a field.
- The fix is verified on a real Android device (portrait and landscape), since the unit tests cover the decision logic and not the framework's scroll.
