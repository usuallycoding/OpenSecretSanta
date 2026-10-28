package com.usuallycoding.opensecretsanta

interface Platform {
    val name: String

    /** Whether a screen is refreshed by pulling it down: the phones yes, the web no (the browser refreshes the page). */
    val hasPullToRefresh: Boolean

    /** Whether the sign-in screen asks to stay signed in: the web, where the person may be on a shared computer; the phones always keep the session. */
    val canChooseToStaySignedIn: Boolean
}

expect fun getPlatform(): Platform
