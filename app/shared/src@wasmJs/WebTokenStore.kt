package com.usuallycoding.opensecretsanta.data.session

/**
 * The web keeps the token in the tab's `sessionStorage`, so a refresh keeps the person signed in and closing
 * the tab ends the session. A script on the page can read `sessionStorage`; the app draws to a canvas and builds
 * no HTML from what people type, which keeps that exposure small. An httpOnly cookie would take it away entirely
 * and is the stronger, later design (it needs the server to issue and read it).
 */
fun webTokenStore(): TokenStore = SessionStorageTokenStore()
