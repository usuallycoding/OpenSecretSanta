# Design

## Context

`openspec list --specs` shows 26 capabilities; eleven were created by archiving a change and kept the generated purpose "TBD - created by archiving change …". The build (`./kotlin build`) warns about `rememberModalBottomSheetState` (deprecated: "Use rememberBottomSheetState with Hidden initial value"), at the four sheet call sites, about `BackHandler` ("Use NavigationEventHandler instead") at the owner's group page and twice in `ShellScreen` (the password and appearance sheets), and, when compiling tests, about two unchecked casts in `WishlistApiTest` and five uses of an experimental coroutines API without an opt-in. The sheets are opened full height (`skipPartiallyExpanded = true`) and the spec "Back closes an open sheet" requires Android's back to close a sheet first, with Escape closing it on the web.

## Goals / Non-Goals

**Goals:**
- Every capability spec says in a sentence or two what it is for.
- No warning from the project's own code in the build or the test build.
- Identical behaviour of every sheet and of back and Escape.

**Non-Goals:**
- Any change to a requirement, scenario, route or screen; warnings that come from a library or the toolchain itself rather than from this code (they are listed if any remain).

## Decisions

1. **Purposes.** For each of the eleven specs, read its requirements and replace the single placeholder line under `## Purpose` with one or two plain sentences (at least a full sentence, as the archive validation expects), in the style of the already-written ones (`client-layout`, `client-times`, `draw-emails`): what the capability lets people do, not how. The heading, the requirements and the scenarios are not touched. `skip_specs` is set in `.openspec.yaml` because no requirement changes; the edit is to the main specs directly.
2. **Bottom sheet state.** One internal helper in `ui/form` (`rememberAppSheetState()`) returns the sheet state for a sheet that opens full height, using the non-deprecated function; the four call sites use it. The exact replacement signature is confirmed first with a short compile spike in `SheetFrame.kt`; the visible behaviour (opening at full height, no half-open stop, dragging down or scrim tap dismisses) is checked in the browser.
3. **Back handling.** `BackHandler(enabled) { … }` becomes the navigation-event equivalent (`NavigationEventHandler` with its state) at the three sites, kept together in one small helper `SheetBackHandler(enabled, onBack)` in `ui/form` so the next API change touches one place. If the replacement turns out not to exist for every target (Android, iOS, web) in the Compose version the project uses, the fallback is to keep `BackHandler` in that helper with a narrow `@Suppress("DEPRECATION")` and a comment saying why, so the warning is cleared without changing behaviour.
4. **Test warnings.** The unchecked casts become `assertIs<List<*>>` plus a `filterIsInstance`, and the experimental opt-in is added with `@OptIn(ExperimentalCoroutinesApi::class)` on the tests (or their file) that need it.

## Risks / Trade-offs

- A replacement API for the sheets or back handling may behave slightly differently on one platform; the browser check covers the web, the builds cover every target, and Android and iOS behaviour is for a device check.
- Purposes are written from the specs' contents, so a purpose can be slightly narrower than the capability's future; it can be edited like any text.
