package com.usuallycoding.opensecretsanta.data.session

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
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
 * unlock and never copied to another device or backup. Queries are built with the Core Foundation
 * dictionary API directly: going through Objective-C collections made the read query invalid
 * (`errSecParam`), so a saved token was never found again.
 *
 * Keychain errors are treated as "nothing stored", so a broken Keychain can only cost the person a
 * sign-in, but the status code is logged (never the token) because that is otherwise invisible.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainTokenStore : TokenStore {
    override suspend fun read(): String? = memScoped {
        val query = baseQuery()
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        if (status != errSecSuccess) {
            if (status != errSecItemNotFound) log("read", status)
            return@memScoped null
        }
        val data: CFDataRef = result.value?.reinterpret() ?: return@memScoped null
        val bytes = CFDataGetBytePtr(data)?.reinterpret<ByteVar>()?.readBytes(CFDataGetLength(data).toInt())
        CFRelease(data)
        bytes?.decodeToString()
    }

    override suspend fun write(token: String, remember: Boolean?) {
        clear()
        val bytes = token.encodeToByteArray()
        val data: CFDataRef? = bytes.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), bytes.size.convert()) }
        val item = baseQuery()
        CFDictionaryAddValue(item, kSecValueData, data)
        CFDictionaryAddValue(item, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        val status = SecItemAdd(item, null)
        CFRelease(item)
        CFRelease(data)
        if (status != errSecSuccess) log("save", status)
    }

    override suspend fun clear() {
        val query = baseQuery()
        SecItemDelete(query)
        CFRelease(query)
    }

    private fun log(action: String, status: Int) = println("Keychain $action failed with status $status")

    /** The dictionary that names the one item this store owns; the caller releases it with `CFRelease`. */
    private fun baseQuery(): CFMutableDictionaryRef? {
        val query = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        for ((key, value) in listOf(kSecAttrService to SERVICE, kSecAttrAccount to ACCOUNT)) {
            val string = CFStringCreateWithCString(null, value, kCFStringEncodingUTF8)
            CFDictionaryAddValue(query, key, string)
            CFRelease(string)
        }
        return query
    }

    private companion object {
        const val SERVICE = "com.usuallycoding.opensecretsanta"
        const val ACCOUNT = "session-token"
    }
}
