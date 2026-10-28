package com.usuallycoding.opensecretsanta

interface Platform {
    val name: String

    /** Whether a screen is refreshed by pulling it down: the phones yes, the web no (the browser refreshes the page). */
    val hasPullToRefresh: Boolean
}

expect fun getPlatform(): Platform
