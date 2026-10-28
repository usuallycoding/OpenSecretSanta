package com.usuallycoding.opensecretsanta

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val hasPullToRefresh: Boolean = false
}

actual fun getPlatform(): Platform = WasmPlatform()
