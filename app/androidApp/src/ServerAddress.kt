package com.usuallycoding.opensecretsanta

/** The server a release build talks to, whatever the working copy it was built from. */
internal const val PRODUCTION_SERVER = "https://santa-api.plm360.org"

/**
 * The server a debuggable build talks to while developing. 10.0.2.2 is the emulator's alias for the development machine's
 * localhost; for a phone, change it to the machine's address and allow that address in `network_security_config.xml`.
 */
internal const val DEVELOPMENT_SERVER = "http://10.0.2.2:8080"

/** A debuggable build keeps to the development server; every other build is a release and uses the real one. */
internal fun serverAddress(debuggable: Boolean): String = if (debuggable) DEVELOPMENT_SERVER else PRODUCTION_SERVER
