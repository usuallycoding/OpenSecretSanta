package com.usuallycoding.opensecretsanta.data.session

/**
 * The web keeps the token in `localStorage` when the person stays signed in and in the tab's `sessionStorage` when not.
 * A script on the page can read either; the app draws to a canvas and builds no HTML from what people type, which keeps
 * that exposure small. An httpOnly cookie would take it away entirely and is the stronger, later design (it needs the
 * server to issue and read it).
 */
fun webTokenStore(): TokenStore = BrowserTokenStore()
