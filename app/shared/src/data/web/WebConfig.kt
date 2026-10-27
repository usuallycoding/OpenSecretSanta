package com.usuallycoding.opensecretsanta.data.web

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
