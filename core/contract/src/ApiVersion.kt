package com.usuallycoding.opensecretsanta.contract

/**
 * The version of the server's API, which the server reports as `apiVersion` in `GET /health` and every app is built with. An app
 * that finds the server's number higher than its own stops and asks to be updated.
 *
 * Raise it by hand, by one, only for a change that an app built with the earlier number would misbehave against (a field it
 * relied on is gone or means something else, a route it calls is gone, a rule it does not know is now enforced). Do not raise it
 * for a change such an app still copes with: a new optional field, a new route, a field kept as an alias for an older reading.
 * Raising it is a release step: have the new app published (an APK in the server's downloads folder, the web app deployed with
 * the server) before the server that reports the new number goes live.
 */
object ApiVersion {
    const val CURRENT = 4
}
