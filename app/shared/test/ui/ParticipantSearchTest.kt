package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.admin.AdminPair
import com.usuallycoding.opensecretsanta.ui.admin.AdminParticipant
import com.usuallycoding.opensecretsanta.ui.admin.filterPairs
import com.usuallycoding.opensecretsanta.ui.admin.filterParticipants
import com.usuallycoding.opensecretsanta.ui.admin.matchesSearch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticipantSearchTest {
    private val people = listOf(
        AdminParticipant("alice", hasAccount = true, profileComplete = true, fullName = "Alice Smith"),
        AdminParticipant("bob", hasAccount = true, profileComplete = false, fullName = "Bob Brown"),
        AdminParticipant("carol-new", hasAccount = false, profileComplete = false),
    )

    private fun names(query: String) = filterParticipants(people, query).map { it.username }

    @Test
    fun aUsernameMatchesAsAPartCaseIgnored() {
        assertEquals(listOf("alice"), names("ALI"))
        assertEquals(listOf("carol-new"), names("carol"))
    }

    @Test
    fun aFullNameMatches() {
        assertEquals(listOf("bob"), names("brown"))
        assertEquals(listOf("alice"), names("Smith"))
    }

    @Test
    fun wordsMayComeInAnyOrderAndMayBeSplitBetweenUsernameAndName() {
        assertEquals(listOf("bob"), names("brown bob"))
        assertEquals(listOf("alice"), names("smith ali"))
        assertEquals(emptyList(), names("alice brown"))
    }

    @Test
    fun aBlankSearchShowsEveryoneInTheirOrder() {
        assertEquals(listOf("alice", "bob", "carol-new"), names(""))
        assertEquals(listOf("alice", "bob", "carol-new"), names("   "))
    }

    @Test
    fun aWordDoesNotMatchAcrossTheUsernameAndTheName() {
        assertFalse(matchesSearch("bobbob", "bob", "Bob Brown"))
        assertTrue(matchesSearch("bob", "bob", null))
    }

    @Test
    fun nobodyMatchingGivesAnEmptyList() {
        assertEquals(emptyList(), names("zzz"))
    }

    private fun pair(santa: String, receiver: String, santaName: String? = null, receiverName: String? = null) =
        AdminPair(santa, receiver, santaIsMe = false, receiverIsMe = false, santaFullName = santaName, receiverFullName = receiverName)

    private val pairs = listOf(
        pair("alice", "bob", "Alice Smith", "Bob Brown"),
        pair("carol", "alice", null, "Alice Smith"),
        pair("dave", "carol"),
    )

    private fun shown(query: String) = filterPairs(pairs, query).map { it.santa to it.receiver }

    @Test
    fun aPairIsShownWhenItsSantaOrItsReceiverMatches() {
        assertEquals(listOf("alice" to "bob", "carol" to "alice"), shown("alice"))
        assertEquals(listOf("alice" to "bob"), shown("bob"))
    }

    @Test
    fun aPairMatchesByEitherSidesFullName() {
        assertEquals(listOf("alice" to "bob"), shown("brown"))
    }

    @Test
    fun wordsOfOneSearchMustAllMatchOnTheSameSide() {
        assertEquals(emptyList(), shown("bob carol"))
    }

    @Test
    fun aBlankSearchShowsEveryPair() {
        assertEquals(pairs.size, shown(" ").size)
    }
}
