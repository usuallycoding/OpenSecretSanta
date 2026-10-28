# Design

## Context

`TokenStore` (`read`, `write`, `clear`) is the seam: Android supplies an encrypted preferences store, iOS the Keychain and the web `webTokenStore()`, which today returns `MemoryTokenStore`. `SessionManager.restore()` already reads the store at start, asks the server about the token (`GET /me`), shows the account or signs out, and clears a rejected token; `signIn`, `changePassword`, `signOut` and a rejected session already write or clear the store. So the whole change is a new store on the web and the words around it. The web entry point binds the address bar to the navigation controller, and the existing requirement is that after signing in the app opens the screen named in the address. See proposal.md and specs/.

## Goals / Non-Goals

**Goals:**
- A page refresh keeps the person signed in on the screen they were on; closing the tab does not.

**Non-Goals:**
- Cookies, server changes, refresh tokens, a shorter lifetime, a Content-Security-Policy for the host.

## Decisions

### D1: A `sessionStorage` token store
A class `SessionStorageTokenStore` in `src@wasmJs` implements `TokenStore` over `kotlinx.browser.sessionStorage` with one fixed key. Every access is wrapped: a read that throws (storage blocked, a sandboxed frame, private mode in an older browser) answers "nothing stored", and a failed write or clear is ignored, exactly as the interface already requires. `webTokenStore()` returns it. `MemoryTokenStore` stays, for tests and as the fallback an implementation can use.

### D2: Why `sessionStorage` and not `localStorage` or a cookie
```
 storage            survives F5   survives tab close   readable by page scripts   needs server work
 memory (today)     no            no                   no                         no
 sessionStorage     yes           no                   yes                        no
 localStorage       yes           yes (up to 7 days)   yes                        no
 httpOnly cookie    yes           yes                  no                         yes (cookies, CSRF)
```
`sessionStorage` is the smallest step that makes a refresh keep the session without keeping it for the week the token lives. The exposure it adds is a script on the page reading the token. The app is a Compose canvas: it builds no HTML from what people type (notes, questions, links are drawn as text and opened with the browser's own link handling), so there is no injection point today. The README states the trade-off and names the httpOnly cookie as the stronger later design, together with a Content-Security-Policy for whoever hosts the app.

### D3: Restoring on a refresh
Nothing new in the flow: the web start calls `restore()`, which finds the token, asks the server, and either shows the account or, for a rejected token, signs out and clears the store (a failure to reach the server leaves the token for the next try but shows sign-in). The address bar is bound after the shell exists, so the screen in the address is opened as it is after a sign-in. If the address names a screen of the view the person does not start in, the behavior is the same as after signing in; this change does not alter it.

### D4: Tests
A browser test of `SessionStorageTokenStore` (the project already runs wasm tests in a headless browser): write then read gives the token, clear gives nothing, a value written by one instance is read by another (the refresh case), and an empty store reads as nothing. `SessionManagerTest` already covers restore, rejection and sign-out against a fake store. The behavior itself is verified by running the app in headless Chrome against a real server: sign in, refresh, same screen and still signed in; sign out, refresh, sign-in; a new tab is signed out; the token is in `sessionStorage` and not in `localStorage` or cookies; a refresh after the server stopped accepting the token shows sign-in.

## Risks / Trade-offs

- [A script on the page can read the token] → See D2; no injection point today, and the README names the stronger later option.
- [Duplicating a tab copies its `sessionStorage`, so the copy is signed in as well] → That is browser behavior for one person's own tabs and does not widen who can read the token.
- [A blocked storage makes the web app forget the sign-in on every refresh] → The same as today's behavior, and covered by the "storage unavailable" scenario.

## Migration Plan

Nothing to migrate: the web never stored a token before. Rollback is the previous build; a token left in `sessionStorage` is ignored by it and dies with the tab.
