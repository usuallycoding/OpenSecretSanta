package com.usuallycoding.opensecretsanta

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val hasPullToRefresh: Boolean = true
}

actual fun getPlatform(): Platform = IOSPlatform()
