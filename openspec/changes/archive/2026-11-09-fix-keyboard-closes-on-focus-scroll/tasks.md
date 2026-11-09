# Tasks

## 1. Finger state in the tracker

- [x] 1.1 Add a read-only `fingerDown` to `GestureTracker` (true between the first pressed event and release), and extend `app/shared/test/ui/GestureTrackerTest.kt` with cases for before a touch, during a touch and after release; verify the test passes.

## 2. Scroll hook

- [x] 2.1 Give `scrollHook` a `fingerDown: () -> Boolean` and call `onScroll` only while it is true; pass the tracker's `fingerDown` from `KeyboardDismissRoot` and from `SheetContent`; verify the app still compiles for Android, iOS and web targets.
- [x] 2.2 Add `app/shared/test/ui/ScrollHookTest.kt` driving the hook's `onPostScroll` directly: a `UserInput` scroll with no finger down does not call `onScroll` (with and without `countConsumed`); the same scroll with a finger down does; a non-`UserInput` scroll does not; verify the tests pass and fail without the check.

## 3. Verification

- [x] 3.1 Run the shared module tests (`./gradlew :app:shared:allTests` or the project's equivalent) and verify they pass.
- [ ] 3.2 On an Android phone, verify: tapping Delivery information in portrait keeps the keyboard open; in landscape, tapping each of the five fields keeps it open; dragging the form with the keyboard open still closes it; a drag inside a multi-line field still keeps it open; a swipe on the Groups list still closes it.
