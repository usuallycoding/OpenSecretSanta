package com.usuallycoding.opensecretsanta.data.push

import com.usuallycoding.opensecretsanta.data.web.FirebaseWebConfig
import kotlinx.coroutines.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.Promise

// What push.js offers (window.ossPush); the browser's own work is done there, in plain JavaScript.
@OptIn(ExperimentalWasmJsInterop::class)
private fun jsSupported(): Boolean = js("!!(window.ossPush && window.ossPush.supported())")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsPermission(): String = js("(window.ossPush ? window.ossPush.permission() : 'denied')")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsRequestPermission(): Promise<JsAny?> = js("window.ossPush.requestPermission()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsToken(config: String): Promise<JsAny?> = js("window.ossPush.token(config)")

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsRemove(config: String): Promise<JsAny?> = js("window.ossPush.remove(config)")

/**
 * Push in the browser: the Notification permission, a service worker and Firebase's web messaging, through push.js. It is
 * only offered where the browser can do it and the page was given the [config] of the Firebase web app.
 */
@OptIn(ExperimentalWasmJsInterop::class)
class WebPushDevice(config: FirebaseWebConfig?) : PushDevice {
    private val configJson: String? = config?.let { Json.encodeToString(it) }

    override val platform: String = "web"
    override val isSupported: Boolean = configJson != null && jsSupported()

    override suspend fun isPermitted(): Boolean = isSupported && jsPermission() == GRANTED

    override suspend fun enable(): PushEnable {
        val config = configJson ?: return PushEnable.Failed
        if (!isSupported) return PushEnable.Failed
        var state = jsPermission()
        // asked right away, while the click that turned the switch on still counts for the browser
        if (state == DEFAULT) state = jsRequestPermission().await<JsAny?>().toString()
        if (state != GRANTED) return PushEnable.Denied
        return jsToken(config).await<JsAny?>()?.toString()?.let { PushEnable.Ready(it) } ?: PushEnable.Failed
    }

    override suspend fun currentToken(): String? {
        val config = configJson ?: return null
        if (!isPermitted()) return null
        return jsToken(config).await<JsAny?>()?.toString()
    }

    override suspend fun disable() {
        val config = configJson ?: return
        jsRemove(config).await<JsAny?>()
    }

    private companion object {
        const val GRANTED = "granted"
        const val DEFAULT = "default"
    }
}
