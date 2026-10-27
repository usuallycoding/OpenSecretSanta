package com.usuallycoding.opensecretsanta.data.session

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Keeps the access token as a generic-password item in the iOS Keychain, readable after the first
 * unlock and never copied to another device or backup. Keychain errors are treated as "nothing
 * stored" so a broken Keychain can only cost the person a sign-in.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainTokenStore : TokenStore {
    override suspend fun read(): String? = memScoped {
        val query = query(
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFBridgingRelease(query)
        if (status != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
    }

    override suspend fun write(token: String) {
        clear()
        val data = NSString.create(string = token).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val item = query(
            kSecValueData to data,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        )
        SecItemAdd(item, null)
        CFBridgingRelease(item)
    }

    override suspend fun clear() {
        val query = query()
        SecItemDelete(query)
        CFBridgingRelease(query)
    }

    /** A retained dictionary the caller must release with `CFBridgingRelease`. */
    private fun query(vararg extra: Pair<CFStringRef?, Any?>): CFDictionaryRef? {
        val dictionary = NSMutableDictionary()
        dictionary.put(kSecClass, kSecClassGenericPassword)
        dictionary.put(kSecAttrService, SERVICE)
        dictionary.put(kSecAttrAccount, ACCOUNT)
        for ((key, value) in extra) dictionary.put(key, value)
        return CFBridgingRetain(dictionary)?.reinterpret()
    }

    private fun NSMutableDictionary.put(key: CFStringRef?, value: Any?) {
        setObject(value.bridged(), forKey = key.bridged() as NSCopyingProtocol)
    }

    /**
     * Core Foundation values (the Security constants) become Objective-C objects for the dictionary.
     * They are shared globals, so they are retained first: the bridge then hands back ownership
     * of the extra reference instead of releasing the constant itself.
     */
    private fun Any?.bridged(): Any? = if (this is CPointer<*>) CFBridgingRelease(CFRetain(this)) else this

    private companion object {
        const val SERVICE = "com.usuallycoding.opensecretsanta"
        const val ACCOUNT = "session-token"
    }
}
