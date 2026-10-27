package com.usuallycoding.opensecretsanta.data.session

/**
 * The web keeps the token in memory only: a script on the page could read anything written to
 * `localStorage` or `sessionStorage`, so a refresh asks for the password again. Persisting a web
 * session is a separate, later design.
 */
fun webTokenStore(): TokenStore = MemoryTokenStore()
