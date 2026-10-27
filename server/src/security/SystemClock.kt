package com.usuallycoding.opensecretsanta.server.security

import com.usuallycoding.opensecretsanta.domain.auth.Clock

object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
