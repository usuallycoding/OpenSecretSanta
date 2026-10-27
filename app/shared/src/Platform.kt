package com.usuallycoding.opensecretsanta

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
