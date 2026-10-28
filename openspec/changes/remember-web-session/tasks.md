# Tasks

## 1. The store

- [x] 1.1 Add the `remember` argument to `TokenStore.write` (ignored by Android, iOS and the memory store), pass it from `SessionManager.signIn` (default `true`) and keep the token where it is on `changePassword`; cover it in the session manager tests
- [x] 1.2 Turn the web store into one with both `localStorage` and `sessionStorage` (read from either, write to one and remove from the other, `null` keeps the place, clear removes both, storage errors still read as nothing); extend the browser test for each case, including a stale copy in the other storage

## 2. The checkbox

- [x] 2.1 Add `staySignedIn` (ticked by default) to the login view model and state, `Platform.canChooseToStaySignedIn` (web only), the checkbox and its string on the sign-in screen, and cover the default and the value passed to `signIn` in the login view model tests

## 3. Check and document

- [x] 3.1 Verify in headless Chrome: ticked, close and reopen (a new page in the same profile) stays signed in; unticked, a refresh stays and a new tab asks; sign out leaves nothing in either storage; a changed password keeps the choice; no checkbox on the native builds (by the platform flag); then update the README, run `./kotlin build` and `./kotlin test` for the whole repo and note what could not be tried (phones)

## Workflow follow-up

- Archive the change after review and sync the delta spec into `openspec/specs`.
