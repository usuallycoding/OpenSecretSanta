# Proposal

## Why

Eleven of the project's capability specs still carry the placeholder purpose "TBD - created by archiving change …", so the spec index does not say what each capability is for. The build also prints a standing pile of deprecation and test warnings (the modal bottom sheet state, `BackHandler`, two unchecked casts and an experimental coroutines opt-in), which hides any new warning that matters. Both are cheap to clear now and only get more expensive to leave.

## What Changes

- Write a real one- or two-sentence `## Purpose` for each of the eleven specs that still has the placeholder: `admin-oversight`, `client-admin-groups`, `client-admin-oversight`, `client-admin-users`, `client-app`, `client-group-pages`, `client-groups`, `client-profile`, `client-receiver-and-questions`, `client-wishlist` and `docker-deployment`. Nothing else in those files changes, and no requirement or scenario is touched.
- Replace the deprecated `rememberModalBottomSheetState` (used by the four sheets: `SheetFrame`, `TextSheet`, `ItemSheetHost` and the participants sheet of the owner's page) with its replacement through one shared helper, so the sheets open and behave exactly as before.
- Replace the deprecated `BackHandler` (the owner's page and the two shell sheets) with `NavigationEventHandler`, keeping Android's back button closing an open sheet before leaving the screen.
- Clear the test warnings: the two unchecked casts in the wishlist API tests and the experimental coroutines API opt-in.
- No behaviour, spec requirement, server or API change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This change has no spec deltas (`skip_specs`): the purposes are documentation in the main specs, edited directly, and the rest is code.

## Impact

- `openspec/specs/<eleven specs>/spec.md` (the `## Purpose` paragraph only).
- `app/shared/src/ui/form/SheetFrame.kt` (a shared sheet-state helper), `gift/TextSheet.kt`, `wishlist/ItemSheetHost.kt`, `admin/AdminGroupScreen.kt`, `admin/OversightSheets.kt`, `shell/ShellScreen.kt`.
- `app/shared/test/ui/WishlistApiTest.kt` and the tests that use `UnconfinedTestDispatcher`/`setMain`.
