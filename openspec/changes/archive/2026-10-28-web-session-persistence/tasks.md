# Tasks

## 1. The store

- [x] 1.1 Add `SessionStorageTokenStore` (one fixed key, every access guarded so a blocked storage reads as nothing stored) in `app/shared/src@wasmJs`, return it from `webTokenStore()`, and add a browser test of write, read, clear, a second instance reading what the first wrote, and an empty store; verify `./kotlin test -p wasmJs` for the shared module and `./kotlin build`

## 2. Words around it

- [x] 2.1 Update the doc comments of `TokenStore`, `MemoryTokenStore` and `webTokenStore()` and the README (the platform table row for the web, the sentence about a refresh asking for the password again, and the trade-off with the httpOnly cookie named as the stronger later option); verify by reading them against the spec delta

## 3. Behavior in a browser

- [x] 3.1 Run the web app against a real server in headless Chrome and check each scenario of the spec delta: sign in, refresh, still signed in on the same screen named in the address; a new tab starts at sign-in; sign out then refresh shows sign-in; the token is in `sessionStorage` and `localStorage` and the cookies hold nothing; a refresh after the server stops accepting the token (restart it with a different signing secret) shows sign-in and leaves no token; note what could not be tried

## 4. Integration

- [x] 4.1 Run `./kotlin build` and `./kotlin test` for the whole repo

## Workflow follow-up

- Archive the change after review and sync the delta spec into `openspec/specs`.
