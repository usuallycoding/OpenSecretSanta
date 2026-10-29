# Tasks

## 1. Spec purposes

- [x] 1.1 Write the `## Purpose` of the eleven specs that still say "TBD" (`admin-oversight`, `client-admin-groups`, `client-admin-oversight`, `client-admin-users`, `client-app`, `client-group-pages`, `client-groups`, `client-profile`, `client-receiver-and-questions`, `client-wishlist`, `docker-deployment`) from what each one's requirements say, changing nothing else in them; check with `grep` that none says "TBD" and with `openspec validate --specs` that all still validate

## 2. Deprecations

- [x] 2.1 Settle the replacements with a short spike (the non-deprecated sheet state function and the navigation-event handler, on every target), then add `rememberAppSheetState()` and `SheetBackHandler(enabled, onBack)` in `ui/form`, use them at the four sheet call sites and the three back-handler sites, and confirm the build no longer warns about either; if the back handler has no replacement on some target, keep it inside the helper with a narrow `@Suppress("DEPRECATION")` and say so
- [x] 2.2 Verify in headless Chrome that every kind of sheet (the wishlist item sheet, the question sheet, the participants sheet, a details sheet, the password and appearance sheets) still opens at full height, closes with Escape, with a tap outside and by dragging down, and that the owner's page still closes its sheet before leaving

## 3. Test warnings

- [x] 3.1 Remove the unchecked casts in the wishlist API tests and add the opt-in for the experimental coroutines API, so that `./kotlin build` and `./kotlin test` print no warning from this project's own code (list any that come from a library)

## Workflow follow-up

- Archive the change after review; there are no delta specs to sync.
