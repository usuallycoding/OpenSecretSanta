package com.usuallycoding.opensecretsanta

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val hasPullToRefresh: Boolean = true
    override val canChooseToStaySignedIn: Boolean = false
    override val offersAndroidDownload: Boolean = false
    override val updateWay: UpdateWay = UpdateWay.Download
    override fun reload() = Unit
}

actual fun getPlatform(): Platform = AndroidPlatform()
