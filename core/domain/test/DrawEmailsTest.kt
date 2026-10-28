package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.email.EmailMessage
import com.usuallycoding.opensecretsanta.domain.email.EmailSummary
import com.usuallycoding.opensecretsanta.domain.email.Mailer
import com.usuallycoding.opensecretsanta.domain.email.SendDrawEmails
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.Username
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeMailer(override val enabled: Boolean = true, private val results: (EmailMessage) -> Boolean = { true }) : Mailer {
    val sent = mutableListOf<EmailMessage>()
    var calls = 0
    var throwing = false

    override fun send(messages: List<EmailMessage>): List<Boolean> {
        calls++
        if (throwing) throw IllegalStateException("service exploded")
        sent += messages
        return messages.map(results)
    }
}

class DrawEmailsTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val owner = users.add("boss", admin = true)

    private fun u(name: String) = Username.parse(name)

    private fun person(name: String, email: String? = "$name@example.org", fullName: String? = null, address: String? = null) {
        users.add(name)
        users.updateProfile(u(name), Profile(fullName = fullName, email = email, phone = "+40 123", address = address, deliveryInfo = "Ring twice"))
    }

    /** alice -> bob -> carol -> dave -> alice. */
    private fun drawn(details: GroupDetails = GroupDetails("Office party")): com.usuallycoding.opensecretsanta.domain.group.Group {
        val names = listOf("alice", "bob", "carol", "dave").map(::u)
        val group = groups.seed(owner, GroupState.ACTIVE, *names.toTypedArray()).let { seeded ->
            groups.setDetails(seeded.id, details)
            groups.find(seeded.id)!!
        }
        groups.assign(group.id, names.indices.map { Assignment(names[it], names[(it + 1) % names.size]) })
        return group
    }

    private fun send(mailer: Mailer, appUrl: String? = null, group: com.usuallycoding.opensecretsanta.domain.group.Group = drawn()) =
        SendDrawEmails(users, groups, mailer, appUrl)(group)

    @Test
    fun eachPersonGetsTheirOwnReceiverAndNobodyElses() {
        listOf("alice", "bob", "carol", "dave").forEach { person(it) }
        val mailer = FakeMailer()

        val summary = send(mailer)

        assertEquals(EmailSummary(sent = 4, failed = 0, noAddress = 0), summary)
        val byRecipient = mailer.sent.associateBy { it.to }
        assertTrue("You are Secret Santa for: bob" in byRecipient.getValue("alice@example.org").text)
        assertTrue("You are Secret Santa for: carol" in byRecipient.getValue("bob@example.org").text)
        assertTrue("You are Secret Santa for: dave" in byRecipient.getValue("carol@example.org").text)
        assertTrue("You are Secret Santa for: alice" in byRecipient.getValue("dave@example.org").text)
        // nobody's email mentions another pair
        assertFalse("carol" in byRecipient.getValue("alice@example.org").text)
        assertFalse("alice" in byRecipient.getValue("bob@example.org").text.replace("Hi bob", ""))
    }

    @Test
    fun theEmailCarriesTheGroupDetailsAndOnlyThoseThatAreSet() {
        listOf("alice", "bob", "carol", "dave").forEach { person(it) }
        val full = FakeMailer()
        send(full, group = drawn(GroupDetails("Party", "Bring snacks", "HQ", LocalDate(2026, 12, 24), LocalTime(18, 30))))
        val text = full.sent.first().text
        assertTrue("The names for \"Party\" have been drawn." in text)
        assertTrue("Bring snacks" in text)
        assertTrue("When: 24 Dec 2026, 18:30" in text)
        assertTrue("Where: HQ" in text)

        val bare = FakeMailer()
        send(bare, group = drawn(GroupDetails("Party")))
        val bareText = bare.sent.first().text
        assertFalse("When:" in bareText)
        assertFalse("Where:" in bareText)
    }

    @Test
    fun theReceiversFullNameIsAddedWhenSetButNoPostalDetailsAreEverIncluded() {
        person("alice")
        person("bob", fullName = "Bob Builder", address = "1 Secret Street, Hidden City")
        person("carol")
        person("dave")
        val mailer = FakeMailer()

        send(mailer)

        val alice = mailer.sent.first { it.to == "alice@example.org" }
        assertTrue("You are Secret Santa for: bob (Bob Builder)" in alice.text)
        for (message in mailer.sent) {
            assertFalse("Secret Street" in message.text, message.to)
            assertFalse("+40 123" in message.text, message.to)
            assertFalse("Ring twice" in message.text, message.to)
        }
    }

    @Test
    fun theSubjectNamesTheGroupButNotTheReceiver() {
        listOf("alice", "bob", "carol", "dave").forEach { person(it) }
        val mailer = FakeMailer()

        send(mailer, group = drawn(GroupDetails("Office\nparty")))

        for (message in mailer.sent) {
            assertEquals("Your Secret Santa draw: Office party", message.subject)
        }
    }

    @Test
    fun theAppAddressIsLinkedOnlyWhenConfigured() {
        listOf("alice", "bob", "carol", "dave").forEach { person(it) }
        val with = FakeMailer()
        send(with, appUrl = "https://santa.example.org")
        assertTrue("Open the app: https://santa.example.org" in with.sent.first().text)

        val without = FakeMailer()
        send(without)
        assertFalse("Open the app" in without.sent.first().text)
    }

    @Test
    fun peopleWithoutAnAccountOrAnAddressAreCountedAndNotEmailed() {
        person("alice")
        person("bob", email = null)
        person("carol")
        // dave has no account at all
        val mailer = FakeMailer()

        val summary = send(mailer)

        assertEquals(EmailSummary(sent = 2, failed = 0, noAddress = 2), summary)
        assertEquals(listOf("alice@example.org", "carol@example.org"), mailer.sent.map { it.to })
    }

    @Test
    fun failuresAreCountedAndTheOthersStillCount() {
        listOf("alice", "bob", "carol", "dave").forEach { person(it) }
        val mailer = FakeMailer(results = { it.to != "bob@example.org" })

        assertEquals(EmailSummary(sent = 3, failed = 1, noAddress = 0), send(mailer))
    }

    @Test
    fun aMailerThatThrowsFailsEveryEmailButNotTheCaller() {
        listOf("alice", "bob", "carol", "dave").forEach { person(it) }
        val mailer = FakeMailer().apply { throwing = true }

        assertEquals(EmailSummary(sent = 0, failed = 4, noAddress = 0), send(mailer))
    }

    @Test
    fun aDisabledMailerIsNeverAskedAndGivesNoSummary() {
        listOf("alice", "bob", "carol", "dave").forEach { person(it) }
        val mailer = FakeMailer(enabled = false)

        assertNull(send(mailer))
        assertEquals(0, mailer.calls)
    }
}
