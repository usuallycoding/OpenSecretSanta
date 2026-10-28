package com.usuallycoding.opensecretsanta.data.session

/**
 * Where a sign-in survives a restart. Each platform supplies one: the Android Keystore, the iOS
 * Keychain, or the tab's session storage on the web. Implementations must not throw for ordinary storage
 * problems; they report "nothing stored" from [read] and ignore a failed [write] or [clear].
 */
interface TokenStore {
    suspend fun read(): String?

    suspend fun write(token: String)

    suspend fun clear()
}

/** Keeps the token only for the life of the process, which is what tests and a store that cannot be reached need. */
class MemoryTokenStore : TokenStore {
    private var token: String? = null

    override suspend fun read(): String? = token

    override suspend fun write(token: String) {
        this.token = token
    }

    override suspend fun clear() {
        token = null
    }
}
