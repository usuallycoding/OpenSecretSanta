package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DtoSerializationTest {
    private val json = Json

    private inline fun <reified T> roundTrip(value: T): String {
        val text = json.encodeToString(value)
        assertEquals(value, json.decodeFromString<T>(text))
        return text
    }

    private val account = AccountDto(
        username = "alice",
        isAdmin = false,
        mustChangePassword = true,
    )

    @Test
    fun loginDtosRoundTrip() {
        roundTrip(LoginRequest("alice", "secret"))
        roundTrip(LoginResponse("jwt", mustChangePassword = true))
    }

    @Test
    fun passwordDtosRoundTrip() {
        roundTrip(ChangePasswordRequest("old", "new-password"))
        roundTrip(ChangePasswordResponse("jwt"))
    }

    @Test
    fun accountRoundTripsWithAndWithoutOptionalFields() {
        roundTrip(account)
        roundTrip(AccountDto(username = "bob", isAdmin = true, mustChangePassword = false))
    }

    @Test
    fun deliveryInfoRoundTripsAndToleratesMissingFields() {
        roundTrip(DeliveryInfoDto(fullName = "Alice", email = "a@b.co", phone = null, address = "1 Main St", deliveryInfo = "Ring twice"))
        assertEquals(DeliveryInfoDto(), json.decodeFromString<DeliveryInfoDto>("{}"))
    }

    @Test
    fun managedUserCarriesTemporaryPasswordOnlyWhenPresent() {
        val withTemp = roundTrip(ManagedUserDto(account, temporaryPassword = "TempPass0001"))
        val without = roundTrip(ManagedUserDto(account))
        assertEquals(true, withTemp.contains("TempPass0001"))
        assertFalse(without.contains("temporaryPassword"))
    }

    @Test
    fun resetAndErrorDtosRoundTrip() {
        roundTrip(ResetPasswordResponse("alice", "TempPass0001"))
        roundTrip(ErrorResponse("validation_failed", "Bad email", field = "email"))
        assertEquals(ErrorResponse("not_found", "x"), json.decodeFromString<ErrorResponse>("""{"code":"not_found","message":"x"}"""))
    }

    @Test
    fun noResponseDtoCanLeakPasswordHashes() {
        val texts = listOf(
            roundTrip(account),
            roundTrip(ManagedUserDto(account, "TempPass0001")),
            roundTrip(LoginResponse("jwt", false)),
        )
        for (text in texts) {
            assertFalse(text.contains("hash", ignoreCase = true), text)
            assertFalse(text.contains("\"password\""), text)
        }
    }
}
