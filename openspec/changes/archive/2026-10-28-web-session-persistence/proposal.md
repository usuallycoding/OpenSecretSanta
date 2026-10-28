# Proposal

## Why

On the web the sign-in lives in memory only, so a browser refresh signs the person out and asks for the password again. That was a deliberate first step (the code says persisting a web session is "a separate, later design"), and it has been tolerable while the app has its own Refresh buttons. The next changes remove those buttons on the web and rely on the browser's refresh instead, which only makes sense if a refresh keeps the person signed in on the page they are on. This change is that missing piece.

## What Changes

- **The web keeps the token in the tab's `sessionStorage`.** A refresh of the page keeps the person signed in: the app checks the stored token with the server as it does on Android and iOS, and shows the screen named in the address.
- **Closing the tab ends the session.** `sessionStorage` belongs to one tab, so a closed tab, a new tab or a new browser window starts signed out. This is the web's equivalent of "close the app", chosen over `localStorage`, which would keep the sign-in for the full week the token lives.
- **Nothing else changes about the token.** Signing out, a rejected token, a password change and an unreadable store behave as they do now; only where the token is kept on the web differs. The token is still never written to `localStorage`, to cookies, to logs or to plain preferences.
- **The trade-off is written down.** Any script running on the page can read `sessionStorage`. The app draws to a canvas and builds no HTML from what people type, which keeps that exposure small, but it is the reason the earlier design kept the token out of browser storage, so the README says so and names an httpOnly cookie as the stronger later option.

Out of scope:
- Cookies, a server change, refresh tokens, a shorter token lifetime, and a Content-Security-Policy for the host (a deployment concern).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `client-app`: "Session storage per platform" now has the web keep the token in the tab's session storage, a refresh keeps the person signed in, and a closed tab does not.

## Impact

- Code: a `sessionStorage`-backed token store in `app/shared/src@wasmJs`, used by `webTokenStore()`; a browser test of it; doc comments on `TokenStore`, `MemoryTokenStore` and `webTokenStore`; README (the platform table and the web sentences).
- APIs: none. Server: none.
- Dependencies: none (`kotlinx.browser` is already used by the web entry point).
