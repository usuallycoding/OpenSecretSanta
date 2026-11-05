package com.usuallycoding.opensecretsanta

/**
 * The host of the web app the emails link to. It is also the host of the link filter in `AndroidManifest.xml`, which Android
 * verifies against `/.well-known/assetlinks.json` on that host; keep the two the same.
 */
internal const val WEB_APP_HOST = "santa.example.com"

/** The part after the `#` of a link that is an `https` address of the web app, or null for any other address. */
internal fun linkFragment(scheme: String?, host: String?, fragment: String?): String? =
    fragment?.takeIf { scheme == "https" && host == WEB_APP_HOST }
