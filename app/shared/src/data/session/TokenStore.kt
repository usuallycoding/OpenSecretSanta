package com.usuallycoding.opensecretsanta.data.session

/**
 * Where a sign-in survives a restart. Each platform supplies one: the Android Keystore, the iOS
 * Keychain, or the browser's storage on the web. Implementations must not throw for ordinary storage
 * problems; they report "nothing stored" from [read] and ignore a failed [write] or [clear].
 */
interface TokenStore {
    suspend fun read(): String?

    /**
     * Stores [token]. [remember] says how long it should last where the platform has a choice (the web): true for as long
     * as the token is accepted, false for the life of the tab, null (a refreshed token) for wherever it is now. Platforms
     * with one place ignore it.
     */
    suspend fun write(token: String, remember: Boolean? = null)

    suspend fun clear()
}

/** Keeps the token only for the life of the process, which is what tests and a store that cannot be reached need. */
class MemoryTokenStore : TokenStore {
    private var token: String? = null

    override suspend fun read(): String? = token

    override suspend fun write(token: String, remember: Boolean?) {
        this.token = token
    }

    override suspend fun clear() {
        token = null
    }
}
