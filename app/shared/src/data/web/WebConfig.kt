package com.usuallycoding.opensecretsanta.data.web

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * The server address the web app talks to: the `baseUrl` from `config.json` when the file is there
 * and usable, the page's own [origin] otherwise. A missing, broken or empty file must never stop
 * the app from starting, so every problem falls back to the origin.
 */
fun resolveBaseUrl(configText: String?, origin: String): String {
    val configured = try {
        (Json.parseToJsonElement(configText ?: return origin).jsonObject["baseUrl"] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.contentOrNull
            ?.trim()
    } catch (_: Exception) {
        null
    }
    return configured?.takeIf { it.isNotEmpty() }?.trimEnd('/') ?: origin
}

/**
 * The public settings of the Firebase web app, which the browser needs to receive push messages. None of them is secret.
 * [vapidKey] is the public half of the Web Push key pair.
 */
@Serializable
data class FirebaseWebConfig(
    val apiKey: String,
    val projectId: String,
    val messagingSenderId: String,
    val appId: String,
    val vapidKey: String,
)

/**
 * The `firebase` object of `config.json`, or null when the file, the object or any of its five settings is missing or
 * empty: push is then simply not offered in this browser, and nothing else is affected.
 */
fun resolveFirebaseConfig(configText: String?): FirebaseWebConfig? = try {
    val firebase = Json.parseToJsonElement(configText ?: return null).jsonObject["firebase"]?.jsonObject
    fun setting(name: String) = (firebase?.get(name) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
    val apiKey = setting("apiKey")
    val projectId = setting("projectId")
    val messagingSenderId = setting("messagingSenderId")
    val appId = setting("appId")
    val vapidKey = setting("vapidKey")
    if (apiKey == null || projectId == null || messagingSenderId == null || appId == null || vapidKey == null) {
        null
    } else {
        FirebaseWebConfig(apiKey, projectId, messagingSenderId, appId, vapidKey)
    }
} catch (_: Exception) {
    null
}
