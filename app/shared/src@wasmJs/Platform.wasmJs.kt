package com.usuallycoding.opensecretsanta

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val hasPullToRefresh: Boolean = false
    override val canChooseToStaySignedIn: Boolean = true
    override val offersAndroidDownload: Boolean = true
    override val updateWay: UpdateWay = UpdateWay.Reload
    override fun reload() = jsReload()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsReload(): Unit = js("window.location.reload()")

actual fun getPlatform(): Platform = WasmPlatform()
