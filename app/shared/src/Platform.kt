package com.usuallycoding.opensecretsanta

/** How a person gets a newer version of the app on a platform. */
enum class UpdateWay {
    /** Reload the page: the web app is served by the server, so a reload fetches the new one. */
    Reload,

    /** Download the Android app from the server and install it. */
    Download,

    /** Nothing the app can offer; the person is told to update it. */
    None,
}

interface Platform {
    val name: String

    /** Whether a screen is refreshed by pulling it down: the phones yes, the web no (the browser refreshes the page). */
    val hasPullToRefresh: Boolean

    /** Whether the sign-in screen asks to stay signed in: the web, where the person may be on a shared computer; the phones always keep the session. */
    val canChooseToStaySignedIn: Boolean

    /** Whether the app offers to download the Android app from the server: the web, where the person may be on a phone that has none; the phones are the apps already. */
    val offersAndroidDownload: Boolean

    /** What the update screen can offer when the app is too old for the server. */
    val updateWay: UpdateWay

    /** Reloads the page, for [UpdateWay.Reload]; does nothing where there is no page. */
    fun reload()
}

expect fun getPlatform(): Platform
